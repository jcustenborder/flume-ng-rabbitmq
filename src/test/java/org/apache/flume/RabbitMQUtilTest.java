/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.flume;

import com.rabbitmq.client.ConnectionFactory;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.Before;


/**
 *
 * @author jcustenborder
 */
public class RabbitMQUtilTest {
    @Test
    public void close_null() {
        RabbitMQUtil.close(null, null);
    }
    
    @Test(expected=java.lang.IllegalArgumentException.class)
    public void getFactory_null(){
        RabbitMQUtil.getFactory(null);
    }
    
    @Test(expected=java.lang.IllegalArgumentException.class)
    public void getFactory_null_hostname(){
        context.put(RabbitMQConstants.CONFIG_HOSTNAME, null);
        RabbitMQUtil.getFactory(context);  
    }
    
    Context context = null;
    
    @Before
    public void createContext() {
        context = new Context();
        context.put(RabbitMQConstants.CONFIG_HOSTNAME, "server01.example.com");
        context.put(RabbitMQConstants.CONFIG_PORT, "12345");
        context.put(RabbitMQConstants.CONFIG_CONNECTIONTIMEOUT, "30000");
        context.put(RabbitMQConstants.CONFIG_PASSWORD, "daofoasidnfioand");
        context.put(RabbitMQConstants.CONFIG_USER, "asdfasdfasd");
        context.put(RabbitMQConstants.CONFIG_VIRTUALHOST, "virtualhost1");
    }
    
    @Test
    public void getFactory(){        
        ConnectionFactory factory = RabbitMQUtil.getFactory(context);
        Assert.assertNotNull("factory should not be null", context);
        
        Assert.assertEquals("Host does not match", context.getString(RabbitMQConstants.CONFIG_HOSTNAME), factory.getHost());
        Assert.assertEquals("Port does not match", context.getInteger(RabbitMQConstants.CONFIG_PORT), (Integer)factory.getPort());
        Assert.assertEquals("ConnectionTimeout does not match", context.getInteger(RabbitMQConstants.CONFIG_CONNECTIONTIMEOUT), (Integer)factory.getConnectionTimeout());
        Assert.assertEquals("Password does not match", context.getString(RabbitMQConstants.CONFIG_PASSWORD), factory.getPassword());
        Assert.assertEquals("Username does not match", context.getString(RabbitMQConstants.CONFIG_USER), factory.getUsername());
        Assert.assertEquals("VirtualHost does not match", context.getString(RabbitMQConstants.CONFIG_VIRTUALHOST), factory.getVirtualHost());
    }
    
    public void getFactory_minimal(){
        context = new Context();
        context.put(RabbitMQConstants.CONFIG_HOSTNAME, "server01.example.com");
        
        ConnectionFactory factory = RabbitMQUtil.getFactory(context);
        Assert.assertNotNull("factory should not be null", context);
        
        Assert.assertEquals("Host does not match", context.getString(RabbitMQConstants.CONFIG_HOSTNAME), factory.getHost());
    }
    
}
