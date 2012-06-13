/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.flume;

import com.google.common.base.Preconditions;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jcustenborder
 */
public class RabbitMQUtils {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQUtils.class);
    static final String PREFIX="RabbitMQ";
    
    private static void setTimestamp(Map<String,String> headers, BasicProperties properties){
        Date date = properties.getTimestamp()==null?new Date():properties.getTimestamp();
        
        Long value=date.getTime();
        headers.put("timestamp", value.toString());
    }
    
    public static Map<String,String> setProperties(BasicProperties properties){
        Preconditions.checkArgument(properties!=null, "properties cannot be null.");
        Map<String,String> headers = new HashMap<String, String>();
        setTimestamp(headers, properties);
        return headers;
    }
    
    public static String getQueueName(Context context) {
        String queueName = context.getString("queuename");
        Preconditions.checkState(queueName!=null, "No queueName specified.");
        return queueName;
    }
    
    public static String getExchangeName(Context context){
        return context.getString("exchangename", "");
    }
    
    public static ConnectionFactory getFactory(Context context){
        ConnectionFactory factory = new ConnectionFactory();
        
        String hostname = context.getString("hostname");
        Preconditions.checkState(hostname!=null, "No hostname specified.");
        factory.setHost(hostname);
        
        int port = context.getInteger("port", -1);
        
        if(-1!=port){
            factory.setPort(port);
        }
        
        String username = context.getString("user");
        
        if(null==username){
            factory.setUsername(ConnectionFactory.DEFAULT_USER);
        } else {
            factory.setUsername(username);
        }
        
        String password = context.getString("password");
        
        if(null==password){
            factory.setPassword(ConnectionFactory.DEFAULT_PASS);
        } else {
            factory.setPassword(password);
        }
        
        String virtualHost = context.getString("virtualhost");
        
        if(null!=virtualHost){
            factory.setVirtualHost(virtualHost);
        }
        
        int connectionTimeout = context.getInteger("connectiontimeout", -1);
        
        if(connectionTimeout>-1){
           factory.setConnectionTimeout(connectionTimeout); 
        }
        
        
        
//        boolean useSSL = context.getBoolean("usessl", false);
//        if(useSSL){
//            factory.useSslProtocol();
//        }
        
        
        return factory;
    }
    
    public static void close(Connection connection, com.rabbitmq.client.Channel channel){
        if(null!=channel) {
            try {
                channel.close();
            } catch(Exception ex){
                if(log.isErrorEnabled())log.error("Exception thrown while closing channel", ex);
            }
        }
        
        if(null!=connection) {
            try {
                connection.close();
            } catch(Exception ex){
                if(log.isErrorEnabled())log.error("Exception thrown while closing connection", ex);
            }
        }
    }
}
