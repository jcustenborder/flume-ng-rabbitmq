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
the queue name and leave the exchange name out.  
Another optional configuration option is the declaration of topic routing keys that you want to listen to.  This is a comma-delimited list.

Another optional configuration option is the declaration of selector mappings - You can send an event to specific channel, by its name.
To enable is option, you must add - agent.sources.amqp.selector.type = multiplexing to the configuration.
The channels list is space-delimited list.(agent.sources.amqp.channels= A B C). In any case - the event will be sent to Default Channels.



**Minimal Config Example**

	agent1.sources.rabbitmq-source1.channels = ch1  
	agent1.sources.rabbitmq-source1.type = org.apache.flume.source.rabbitmq.RabbitMQSource  
	agent1.sources.rabbitmq-source1.hostname = 10.10.10.173  
	
	agent1.sources.rabbitmq-source1.queuename = log_jammin 
	OR
	agent1.sources.rabbitmq-source1.exchangename = log_jammin_exchange

**Full Topic Config Example**

	agent.sources = amqp
	agent.channels = DefaultChannel1 Channel1 Channel2
	agent.sinks = s3n1 s3n2 s3n3
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

**Selector Mapping Config Example**
	agent.sources.amqp.type = org.apache.flume.source.rabbitmq.RabbitMQSource
	agent.sources.amqp.exchangename=log_jammin_exchange
	agent.sources.amqp.queuename=log_jammin
	agent.sources.amqp.hostname=10.10.10.173
	agent.sources.amqp.username=rabbitmquser
	agent.sources.amqp.password=p@$$w0rd!
	agent.sources.amqp.port=12345
	agent.sources.amqp.channels = Channel1 Channel2
	agent.sources.amqp.channels.default= DefaultChannel1

	#Mapping for multiplexing selector
	agent.sources.amqp.selector.type = multiplexing
	agent.sources.amqp.selector.header = selectorHeader
	agent.sources.amqp.selector.mapping.selectorHeaderValue1 = Channel1
	agent.sources.amqp.selector.mapping.selectorHeaderValue2 = Channel2

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

**Selector Mapping Config Example**

	# Each sink's type must be defined
	agent.sinks.s3n1.type = hdfs
	agent.sinks.s3n1.hdfs.path = s3n://userName:password@backetName/FolderName/DefaultChannel1/%y-%m-%d/
	agent.sinks.s3n1.hdfs.fileType = DataStream
	agent.sinks.s3n1.hdfs.rollInterval = 60
	agent.sinks.s3n1.hdfs.rollSize = 0
	agent.sinks.s3n1.hdfs.rollCount = 0
	agent.sinks.s3n1.hdfs.writeFormat = Text
	agent.sinks.s3n1.serializer = Text
	agent.sinks.s3n1.channel = DefaultChannel1

	# Each sink's type must be defined
	agent.sinks.s3n2.type = hdfs
	agent.sinks.s3n2.hdfs.path = s3n://userName:password@backetName/FolderName/Channel1/%y-%m-%d/
	agent.sinks.s3n2.hdfs.fileType = DataStream
	agent.sinks.s3n2.hdfs.rollInterval = 60
	agent.sinks.s3n2.hdfs.rollSize = 0
	agent.sinks.s3n2.hdfs.rollCount = 0
	agent.sinks.s3n2.hdfs.writeFormat = Text
	agent.sinks.s3n2.serializer = Text
	agent.sinks.s3n2.channel = Channel1

	# Each sink's type must be defined
	agent.sinks.s3n3.type = hdfs
	agent.sinks.s3n3.hdfs.path = s3n://userName:password@backetName/FolderName/Channel2/%y-%m-%d/
	agent.sinks.s3n3.hdfs.fileType = DataStream
	agent.sinks.s3n3.hdfs.rollInterval = 60
	agent.sinks.s3n3.hdfs.rollSize = 0
	agent.sinks.s3n3.hdfs.rollCount = 0
	agent.sinks.s3n3.hdfs.writeFormat = Text
	agent.sinks.s3n3.serializer = Text
	agent.sinks.s3n3.channel = Channel2



