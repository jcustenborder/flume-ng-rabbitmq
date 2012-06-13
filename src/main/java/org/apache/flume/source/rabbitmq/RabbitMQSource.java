/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.flume.source.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.util.Map;
import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jcustenborder
 */
public class RabbitMQSource extends AbstractSource implements Configurable, PollableSource {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQSource.class);
    private CounterGroup _CounterGroup;
    private ConnectionFactory _ConnectionFactory;
    private Connection _Connection;
    private Channel _Channel;
    private String _QueueName;
      
    public RabbitMQSource(){
        _CounterGroup = new CounterGroup();
    }
    
    
    @Override
    public void configure(Context context) {
        _ConnectionFactory = RabbitMQUtils.getFactory(context);        
        _QueueName = RabbitMQUtils.getQueueName(context);  
    }

    
    @Override
    public synchronized void stop() {
        RabbitMQUtils.close(_Connection, _Channel);      
        super.stop();
    }
    
    private void resetConnection(){
        _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_EXCEPTION);
        if(log.isWarnEnabled())log.warn(this.getName() + " - Closing RabbitMQ connection and channel due to exception.");
        RabbitMQUtils.close(_Connection, _Channel);
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
        } catch (Exception ex){
            _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_EXCEPTION);
            if(log.isErrorEnabled()) log.error(this.getName() + " - Exception thrown while pulling from queue.", ex);          
            resetConnection();
            return Status.BACKOFF;
        }
        
        if(null==response){
            _CounterGroup.incrementAndGet(RabbitMQConstants.COUNTER_GET_MISS);
            return Status.BACKOFF;
        }
        
        try {
            Map<String, String> properties = RabbitMQUtils.setProperties(response.getProps());

            Event event = new SimpleEvent();
            event.setBody(response.getBody());
            event.setHeaders(properties);

            getChannelProcessor().processEvent(event);            
        } catch(Exception ex){
            if(log.isErrorEnabled())
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
}