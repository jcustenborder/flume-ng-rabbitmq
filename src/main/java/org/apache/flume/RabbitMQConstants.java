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

public class RabbitMQConstants {
    public final static String COUNTER_NEW_CONNECTION="rabbitmq.newconnection";
    public final static String COUNTER_NEW_CHANNEL="rabbitmq.newchannel";
    public final static String COUNTER_EXCEPTION="rabbitmq.exception";
    public final static String COUNTER_GET="rabbitmq.get";
    public final static String COUNTER_GET_MISS="rabbitmq.getmiss";
    public final static String COUNTER_ACK="rabbitmq.ack";
    public final static String COUNTER_PUBLISH="rabbitmq.publish";
}
