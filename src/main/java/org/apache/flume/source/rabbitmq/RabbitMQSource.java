
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.flume.source.rabbitmq;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.CounterGroup;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.PollableSource;
import org.apache.flume.RabbitMQConstants;
import org.apache.flume.RabbitMQUtil;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;


public class RabbitMQSource extends AbstractSource implements Configurable, PollableSource {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQSource.class);
    private CounterGroup _CounterGroup;
    private ConnectionFactory _ConnectionFactory;
    private Connection _Connection;
    private QueueingConsumer _Consumer;
    private Channel _Channel;
    private String _QueueName;
    private String _ExchangeName;
    private String[] _Topics;
      
    public RabbitMQSource(){
        _CounterGroup = new CounterGroup();
    }
    
    
    @Override
    public void configure(Context context) {
        _ConnectionFactory = RabbitMQUtil.getFactory(context);        
        _QueueName = RabbitMQUtil.getQueueName(context);  
        _ExchangeName = RabbitMQUtil.getExchangeName(context);
        _Topics = RabbitMQUtil.getTopics(context);
        
        ensureConfigCompleteness( context );
    }
    
    @Override
    public synchronized void stop() {
        RabbitMQUtil.close(_Connection, _Channel);      
        super.stop();
    }
    
    private void resetConnection(){
        _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_EXCEPTION);
        if(log.isWarnEnabled())log.warn(this.getName() + " - Closing RabbitMQ connection and channel due to exception.");
        RabbitMQUtil.close(_Connection, _Channel);
        _Connection=null;
        _Channel=null;
    }
    
    @Override
    public PollableSource.Status process() throws EventDeliveryException {
        if(null==_Connection){
            try {
                if(log.isInfoEnabled())log.info(this.getName() + " - Opening connection to " + _ConnectionFactory.getHost() + ":" + _ConnectionFactory.getPort());
                _Connection = _ConnectionFactory.newConnection();
                _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_NEW_CONNECTION);               
                _Channel = null;
            } catch(Exception ex) {
                if(log.isErrorEnabled()) log.error(this.getName() + " - Exception while establishing connection.", ex);
                resetConnection();
                return Status.BACKOFF;
            }            
        }
        
        if(null==_Channel){
            try {
                if(log.isInfoEnabled())log.info(this.getName() + " - creating channel...");
                _Channel = _Connection.createChannel();
                _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_NEW_CHANNEL);
                if(log.isInfoEnabled())log.info(this.getName() + " - Connected to " + _ConnectionFactory.getHost() + ":" + _ConnectionFactory.getPort());
                
                if( StringUtils.isNotEmpty(_ExchangeName) ) {
                	try {
        	        	//declare an exchange
        	        	_Channel.exchangeDeclarePassive(_ExchangeName);  
        	        	
        	        	//only grab a default queuename if one is not specified in config
        	        	if( StringUtils.isEmpty( _QueueName ) ) {
        	        		_QueueName = _Channel.queueDeclare().getQueue();
        	        	}
        	        	
        	        	//for each topic, bind to the key
        	        	if( null != _Topics ) {
	        	        	for ( String topic : _Topics ) {
	        	        		_Channel.queueBind(_QueueName, _ExchangeName, topic);
	        	        	}
        	        	}
                	}
                	catch( Exception ex ) {              
                        if(log.isErrorEnabled()) log.error(this.getName() + " - Exception while declaring exchange.", ex);
                        resetConnection();
                        return Status.BACKOFF;
                    }      
                }
            } catch(Exception ex) {              
                if(log.isErrorEnabled()) log.error(this.getName() + " - Exception while creating channel.", ex);
                resetConnection();
                return Status.BACKOFF;
            }             
        }
        if(null == _Consumer){
        	try{
        		_Consumer = new QueueingConsumer(_Channel);
    			_Channel.basicConsume(_QueueName, false, _Consumer);
        	}catch( Exception ex ) {              
                if(log.isErrorEnabled()) log.error(this.getName() + " - Exception while registering consumer", ex);
                resetConnection();
                return Status.BACKOFF;
            }      
        }

		QueueingConsumer.Delivery delivery;

		try {
			 delivery = _Consumer.nextDelivery();
			_CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_GET);
		} 
		catch (Exception ex) {
			_CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_EXCEPTION);
			if (log.isErrorEnabled())
				log.error(this.getName() + " - Exception thrown while pulling from queue.", ex);
			resetConnection();
			return Status.BACKOFF;
		}

		if (null == delivery) {
			_CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_GET_MISS);
			return Status.BACKOFF;
		}

		try {
			Map<String, String> properties = RabbitMQUtil.getHeaders(delivery.getProperties());
			Event event = new SimpleEvent();
			event.setBody(delivery.getBody());
			event.setHeaders(properties);

			getChannelProcessor().processEvent(event);
		} catch (Exception ex) {
			if (log.isErrorEnabled())
				log.error(this.getName() + " - Exception thrown while processing event", ex);

			return Status.BACKOFF;
		}

        try {
        	_Channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_ACK);
        } catch(Exception ex){
            _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_EXCEPTION);
            if(log.isErrorEnabled())log.error(this.getName() + " - Exception thrown while sending ack to queue", ex);
            resetConnection();
            return Status.BACKOFF;            
        }
        
        return Status.READY;       
    }
    

    /**
     * Verify that the required configuration is set
     * 
     * @param context
     */
    private void ensureConfigCompleteness( Context context ) {
    	
    	if( StringUtils.isEmpty(context.getString( RabbitMQConstants.CONFIG_EXCHANGENAME ) ) &&
    			StringUtils.isEmpty( context.getString( RabbitMQConstants.CONFIG_QUEUENAME ) ) ) {

    		throw new IllegalArgumentException( "You must configure at least one of queue name or exchange name parameters" );
    	}
    }
}
