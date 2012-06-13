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

package org.apache.flume;

import com.google.common.base.Preconditions;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Date;
import java.util.Map;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RabbitMQUtil {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQUtil.class);
    static final String PREFIX="RabbitMQ";
    
    private static void setTimestamp(Map<String,String> headers, BasicProperties properties){
        Date date = properties.getTimestamp()==null?new Date():properties.getTimestamp();        
        Long value=date.getTime();
        headers.put("timestamp", value.toString());
    }
    
    public static Map<String,String> getHeaders(BasicProperties properties){
        Preconditions.checkArgument(properties!=null, "properties cannot be null.");
        Map<String,String> headers = new CaseInsensitiveMap();
        setTimestamp(headers, properties);
        
        Map<String, Object> rabbitmqHeaders = properties.getHeaders();
        
        if(null!=rabbitmqHeaders){
            for(Map.Entry<String, Object> kvp:rabbitmqHeaders.entrySet()){
                if(!headers.containsKey(kvp.getKey())&&null!=kvp.getValue()){
                    if(log.isInfoEnabled())log.info("header=" + kvp.getKey() + " value=" + kvp.getValue());
                    headers.put(kvp.getKey(), kvp.getValue().toString());
                }
            }
        }
        
        return headers;
    }
    
    public static String getQueueName(Context context) {
        String queueName = context.getString(RabbitMQConstants.CONFIG_QUEUENAME);
        Preconditions.checkState(queueName!=null, "No queueName specified.");
        return queueName;
    }
    
    public static String getExchangeName(Context context){
        return context.getString(RabbitMQConstants.CONFIG_QUEUENAME, "");
    }
    
    public static ConnectionFactory getFactory(Context context){
        Preconditions.checkArgument(context!=null, "context cannot be null.");
        ConnectionFactory factory = new ConnectionFactory();
        
        String hostname = context.getString("hostname");
        Preconditions.checkArgument(hostname!=null, "No hostname specified.");
        factory.setHost(hostname);
        
        int port = context.getInteger(RabbitMQConstants.CONFIG_PORT, -1);
        
        if(-1!=port){
            factory.setPort(port);
        }
        
        String username = context.getString(RabbitMQConstants.CONFIG_USERNAME);
        
        if(null==username){
            factory.setUsername(ConnectionFactory.DEFAULT_USER);
        } else {
            factory.setUsername(username);
        }
        
        String password = context.getString(RabbitMQConstants.CONFIG_PASSWORD);
        
        if(null==password){
            factory.setPassword(ConnectionFactory.DEFAULT_PASS);
        } else {
            factory.setPassword(password);
        }
        
        String virtualHost = context.getString(RabbitMQConstants.CONFIG_VIRTUALHOST);
        
        if(null!=virtualHost){
            factory.setVirtualHost(virtualHost);
        }
        
        int connectionTimeout = context.getInteger(RabbitMQConstants.CONFIG_CONNECTIONTIMEOUT, -1);
        
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
