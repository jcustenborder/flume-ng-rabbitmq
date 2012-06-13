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

package org.apache.flume.sink.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQSink extends AbstractSink implements Configurable {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQSink.class);
    private CounterGroup _CounterGroup;
    private ConnectionFactory _ConnectionFactory;
    private Connection _Connection;
    private Channel _Channel;
    private String _QueueName;
    private String _ExchangeName;
    
    public RabbitMQSink(){
        _CounterGroup=new CounterGroup();
    }
    
    @Override
    public void configure(Context context) {
        _ConnectionFactory = RabbitMQUtil.getFactory(context);
        _QueueName = RabbitMQUtil.getQueueName(context);
        _ExchangeName= RabbitMQUtil.getExchangeName(context);        
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
    public Status process() throws EventDeliveryException {
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
            } catch(Exception ex) {              
                if(log.isErrorEnabled()) log.error(this.getName() + " - Exception while creating channel.", ex);
                resetConnection();
                return Status.BACKOFF;
            }             
        }
        
        Transaction tx = getChannel().getTransaction();

        try {
            tx.begin();
            
            Event e = getChannel().take();

            if(e==null){
                tx.rollback();
                return Status.BACKOFF;
            }
            
            try {
                _Channel.basicPublish(_ExchangeName, _QueueName, null, e.getBody());
                tx.commit();
                _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_PUBLISH);
            } catch(Exception ex){
                resetConnection();
                throw ex;
            }
            
            return Status.READY;

        } catch (Exception ex) {
         
          tx.rollback();
          
          if(log.isErrorEnabled())
              log.error(this.getName() + " - Exception while publishing...", ex);
          
          return Status.BACKOFF;

        } finally {
            tx.close();
        }   
    }   
}
