Flume-ng RabbitMQ
========

This project provides both a RabbitMQ source and sink for Flume-NG.  To use this plugin with your Flume installation, build from source using

<pre>mvn package</pre>

and put the resulting jar file in the lib directory in your flume installation.

This project is available under the Apache License.  

Configuration of RabbitMQ Source
------
The configuration of RabbitMQ sources requires that you either declare an exchange name or a queue name.

The exchange name option is helpful if you have declared an exchange in RabbitMQ, but want to use a
default named queue.  If you have a predeclared queue that you want to receive events from, then you can simply declare
the queue name and leave the exchange name out.  Another optional configuration option is the declaration of
topic routing keys that you want to listen to.  This is a comma-delimited list.

**Minimal Config Example**

	agent1.sources.rabbitmq-source1.channels = ch1  
	agent1.sources.rabbitmq-source1.type = org.apache.flume.source.rabbitmq.RabbitMQSource  
	agent1.sources.rabbitmq-source1.hostname = 10.10.10.173  
	
	agent1.sources.rabbitmq-source1.queuename = log_jammin 
	OR
	agent1.sources.rabbitmq-source1.exchangename = log_jammin_exchange

**Full Config Example**

	agent1.sources.rabbitmq-source1.channels = ch1  
	agent1.sources.rabbitmq-source1.type = org.apache.flume.source.rabbitmq.RabbitMQSource  
	agent1.sources.rabbitmq-source1.hostname = 10.10.10.173  
	
	agent1.sources.rabbitmq-source1.queuename = log_jammin
	OR
	agent1.sources.rabbitmq-source1.exchangename = log_jammin_exchange
	
	agent1.sources.rabbitmq-source1.topics = topic1,topic2
	agent1.sources.rabbitmq-source1.username = rabbitmquser
	agent1.sources.rabbitmq-source1.password = p@$$w0rd!
	agent1.sources.rabbitmq-source1.port = 12345
	agent1.sources.rabbitmq-source1.virtualhost = virtualhost1

RabbitMQ Sink
------
**Minimal Config Example**

	agent1.sinks.rabbitmq-sink1.channels = ch1  
	agent1.sinks.rabbitmq-sink1.type = org.apache.flume.sink.rabbitmq.RabbitMQSink  
	agent1.sinks.rabbitmq-sink1.hostname = 10.10.10.173  
	agent1.sinks.rabbitmq-sink1.queuename = log_jammin

**Full Config Example**

	agent1.sinks.rabbitmq-sink1.channels = ch1  
	agent1.sinks.rabbitmq-sink1.type = org.apache.flume.sink.rabbitmq.RabbitMQSink  
	agent1.sources.rabbitmq-source1.hostname = 10.10.10.173  
	agent1.sources.rabbitmq-source1.queuename = log_jammin
	agent1.sources.rabbitmq-source1.username = rabbitmquser
	agent1.sources.rabbitmq-source1.password = p@$$w0rd!
	agent1.sources.rabbitmq-source1.port = 12345
	agent1.sources.rabbitmq-source1.virtualhost = virtualhost1
	agent1.sources.rabbitmq-source1.exchange = exchange1