
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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.source.AbstractSource;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


public class RabbitMQSource extends AbstractSource implements Configurable, PollableSource {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQSource.class);
    private CounterGroup _CounterGroup;
    private ConnectionFactory _ConnectionFactory;
    private Connection _Connection;
    private Channel _Channel;
    private String _QueueName;
    private String _ExchangeName;
    private String _SelectorHeader;
    private String[] _Topics;
    private String[] _ChannelsNames;
    private String[] _DefaultChannelsNames;
    private Map<String,String> _SelectorsMapping;
      
    public RabbitMQSource(){
        _CounterGroup = new CounterGroup();
    }
    
    
    @Override
    public void configure(Context context) {
        _ConnectionFactory = RabbitMQUtil.getFactory(context);        
        _QueueName = RabbitMQUtil.getQueueName(context);  
        _ExchangeName = RabbitMQUtil.getExchangeName(context);
        _Topics = RabbitMQUtil.getTopics(context);
        _SelectorHeader = RabbitMQUtil.getSelectorHeader(context);
        _SelectorsMapping = RabbitMQUtil.getSelectorsMappings(context);
        _ChannelsNames = RabbitMQUtil.getChannelNames(context);
        _DefaultChannelsNames = RabbitMQUtil.getDefaultChannels(context);

        ensureConfigCompleteness(context);
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
                        if(log.isInfoEnabled())log.info(this.getName() + " - creating exchange...");
        	        	//declare an exchange
                        _Channel.exchangeDeclare(_ExchangeName,"direct",true,true,null);
        	        	//_Channel.exchangeDeclarePassive(_ExchangeName);

        	        	//only grab a default queuename if one is not specified in config
                        if(log.isInfoEnabled())log.info(this.getName() + " - creating queue...");
        	        	if( StringUtils.isEmpty( _QueueName ) ) {
        	        		_QueueName = _Channel.queueDeclare().getQueue();
        	        	} else {
                            // declare the queue anyway
                            _Channel.queueDeclare(_QueueName,true,true,true,null);
                        }

        	        	//for each topic, bind to the key
        	        	if( null != _Topics ) {
	        	        	for ( String topic : _Topics ) {
	        	        		_Channel.queueBind(_QueueName, _ExchangeName, topic);
	        	        	}
        	        	} else {
                            _Channel.queueBind(_QueueName, _ExchangeName, _QueueName); // binds the exchange to queue by default
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

		GetResponse response;

		try {
            response = _Channel.basicGet(_QueueName, false);
            _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_GET);
		} 
		catch (Exception ex) {
			_CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_EXCEPTION);
			if (log.isErrorEnabled())
				log.error(this.getName() + " - Exception thrown while pulling from queue.", ex);
			resetConnection();
			return Status.BACKOFF;
		}

		if (null == response) {
			_CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_GET_MISS);
			return Status.BACKOFF;
		}

		try {
			Map<String, String> properties = RabbitMQUtil.getHeaders(response
					.getProps());

			Event event = new SimpleEvent();
			event.setBody(response.getBody());
			event.setHeaders(properties);

            if (_SelectorsMapping != null) {   // process the event to couple of channels in case there are selectors mapping
                String responseBody = new String(response.getBody());
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    String header = jsonObject.getString(_SelectorHeader);
                    String channelName = _SelectorsMapping.get(header);
                    processSpecificChannels(event, channelName);           // sending to a specific channels
                } catch (org.codehaus.jettison.json.JSONException jsonEx) {
                    if (log.isDebugEnabled()) {
                        log.debug(this.getName() + " - the response body isn't JSON valid", jsonEx);
                    }
                    processSpecificChannels(event, "");           // sending to default channels

                    return Status.BACKOFF;
                }
            } else {
                getChannelProcessor().processEvent(event);  // process the event by default
            }
        } catch (Exception ex) {
			if (log.isErrorEnabled())
				log.error(this.getName() + " - Exception thrown while processing event", ex);

			return Status.BACKOFF;
		}

        try {
            _Channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
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
     * Process the event to specific channels by name. In any case, the event will be processed to default channels.
      * @param event
     * @param channelName
     * @return Status after processing the event to channels
     * @throws EventDeliveryException
     */
    private Status processSpecificChannels(Event event, String channelName) throws EventDeliveryException {
        for (String name : _DefaultChannelsNames) {
            processEventToSpecificChannelByName(event, name);
        }

        processEventToSpecificChannelByName(event, channelName);
        return Status.READY;
    }

    /**
     * Process the event to specific channel by its name
     * @param event
     * @param name
     */
    private void processEventToSpecificChannelByName(Event event, String name) {
        org.apache.flume.Channel channel = getChannelByName(name);
        if (channel != null) {
           Status status = processSpecificChannel(event,channel);
           writeStatusToLogAfterProcess(status, channel.getName());
        }
    }

    /**
     * Write the status of the process to log
     * @param status
     * @param channelName
     */
    private void writeStatusToLogAfterProcess(Status status, String channelName) {
        if(log.isDebugEnabled())  {
           log.debug(this.getName() + " - Status of process event to channel + ", channelName + " is" + status);
        }
    }

    /**
     * process the event to the channel
     * @param event
     * @param ch
     * @return Status of the process
     */
    private Status processSpecificChannel(Event event, org.apache.flume.Channel ch) {
        Status status;Transaction txn = ch.getTransaction();
        txn.begin();
        // This try clause includes whatever Channel operations you want to do
        try {
            // Store the Event into this Source's associated Channel(s)
            ch.put(event);

            txn.commit();
            status = Status.READY;
        } catch (Throwable t) {
            txn.rollback();
            if (t instanceof Error) {
                if(log.isErrorEnabled()) {
                    log.error("Error while writing to channel: " +  ch, t);
                }
                throw (Error) t;
            } else {
                throw new ChannelException("Unable to put event on " + "channel: " + ch, t);
            }
        } finally {
            if (txn != null) {
                txn.close();
            }
        }
        return status;
    }

    private org.apache.flume.Channel getChannelByName(String channelName)  {
        if (StringUtils.isNotEmpty(channelName)) {
            List<org.apache.flume.Channel> allChannels = getChannelProcessor().getSelector().getAllChannels();
            if (!allChannels.isEmpty()) {
                for (org.apache.flume.Channel channel : allChannels) {
                    if (channel != null) {
                        if (channel.getName().compareTo(channelName) == 0) {
                            return channel;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Verify that the required configuration is set
     * @param context
     */
    private void ensureConfigCompleteness( Context context ) {
    	if( StringUtils.isEmpty(context.getString( RabbitMQConstants.CONFIG_EXCHANGENAME ) ) &&
    			StringUtils.isEmpty( context.getString( RabbitMQConstants.CONFIG_QUEUENAME ) ) ) {
    		throw new IllegalArgumentException( "You must configure at least one of queue name or exchange name parameters" );
    	}
    }
}
