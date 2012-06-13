Flume-ng RabbitMQ
========

Apache License

RabbitMQ Source
------
**Minimal Config Example**

	agent1.sources.rabbitmq-source1.channels = ch1  
	agent1.sources.rabbitmq-source1.type = org.apache.flume.source.rabbitmq.RabbitMQSource  
	agent1.sources.rabbitmq-source1.hostname = 10.10.10.173  
	agent1.sources.rabbitmq-source1.queuename = log_jammin

**Full Config Example**

	agent1.sources.rabbitmq-source1.channels = ch1  
	agent1.sources.rabbitmq-source1.type = org.apache.flume.source.rabbitmq.RabbitMQSource  
	agent1.sources.rabbitmq-source1.hostname = 10.10.10.173  
	agent1.sources.rabbitmq-source1.queuename = log_jammin
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