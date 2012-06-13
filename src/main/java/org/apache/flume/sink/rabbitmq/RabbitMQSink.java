/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author jcustenborder
 */
public class RabbitMQSink extends AbstractSink implements Configurable {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQSink.class);
    
    private ConnectionFactory _ConnectionFactory;
    private Connection _Connection;
    private Channel _Channel;
    private String _QueueName;
    private String _ExchangeName;
    
    @Override
    public void configure(Context context) {
        _ConnectionFactory = RabbitMQUtils.getFactory(context);
        _QueueName = RabbitMQUtils.getQueueName(context);
        _ExchangeName= RabbitMQUtils.getExchangeName(context);        
    }

    @Override
    public synchronized void stop() {
        RabbitMQUtils.close(_Connection, _Channel);      
        super.stop();
    }
    
    
    
    @Override
    public Status process() throws EventDeliveryException {
        
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
        
        Transaction tx = getChannel().getTransaction();

        try {
            tx.begin();
            
            Event e = getChannel().take();

            if(e==null){
                tx.rollback();
                return Status.BACKOFF;
            }
            
            _Channel.basicPublish(_ExchangeName, _QueueName, null, e.getBody());

            tx.commit();

            return Status.READY;

        } catch (Exception ex) {
         
          tx.rollback();
          
          if(log.isErrorEnabled())
              log.error("Exception thrown", ex);
          
          return Status.BACKOFF;

        } finally {
            tx.close();
        }   
    }   
}
