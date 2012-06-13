/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.flume.source.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.sun.tools.corba.se.idl.Factories;
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
    
    private ConnectionFactory _ConnectionFactory;
    private Connection _Connection;
    private Channel _Channel;
    private String _QueueName;
    
    
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
    
    
    
    @Override
    public PollableSource.Status process() throws EventDeliveryException {
        if(null==_Connection){
            try {
                if(log.isInfoEnabled())log.info(this.getName() + " - Opening connection to " + _ConnectionFactory.getHost() + ":" + _ConnectionFactory.getPort());
                _Connection = _ConnectionFactory.newConnection();
                _Channel = null;
            } catch(Exception ex) {
                if(log.isErrorEnabled())
                    log.error(this.getName() + " - Exception while establishing connection.", ex);
                return Status.BACKOFF;
            }            
        }
        
        if(null==_Channel){
            try {
                if(log.isInfoEnabled())log.info(this.getName() + " - creating channel...");
                _Channel = _Connection.createChannel();
            } catch(Exception ex) {
                if(log.isErrorEnabled())
                    log.error(this.getName() + " - Exception while creating channel.", ex);
                return Status.BACKOFF;
            }             
        }
        
        GetResponse response;
        
        try {
            response = _Channel.basicGet(_QueueName, false);
        } catch (Exception ex){
            if(log.isErrorEnabled())
                    log.error(this.getName() + " - Exception thrown while pulling from queue.", ex);
            
            if(log.isWarnEnabled())log.warn(this.getName() + " - Closing RabbitMQ connection and channel due to exception.");
            RabbitMQUtils.close(_Connection, _Channel);
            return Status.BACKOFF;
        }
        
        if(null==response){
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
        } catch(Exception ex){
            if(log.isErrorEnabled())log.error(this.getName() + " - Exception thrown while sending ack to queue", ex);
            
            if(log.isWarnEnabled())log.warn(this.getName() + " - Closing RabbitMQ connection and channel due to exception.");
            RabbitMQUtils.close(_Connection, _Channel);
            return Status.BACKOFF;            
        }
        
        return Status.READY;       
    }
}
