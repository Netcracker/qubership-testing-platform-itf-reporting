/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.qubership.automation.itf.integration.config.jms.connection;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;

@Configuration
@EnableJms
public class ActiveMqConnectionFactoryConfiguration {

    @Value("${message-broker.url}")
    private String brokerUrl;
    @Value("${receiver.prefetchPolicy.queue}")
    private Integer queuePrefetch;
    @Value("${receiver.prefetchPolicy.topic}")
    private Integer topicPrefetch;
    @Value("${receiver.redeliveryPolicy.maximumRedeliveries}")
    private Integer maximumRedeliveries;

    /**
     * Constructor.
     *
     * @return ActiveMq Connection Factory.
     */
    @Bean
    public ActiveMQConnectionFactory activeMqConnectionFactory() {
        ActiveMQConnectionFactory activeMqConnectionFactory = new ActiveMQConnectionFactory();
        activeMqConnectionFactory.getPrefetchPolicy().setQueuePrefetch(queuePrefetch);
        activeMqConnectionFactory.getPrefetchPolicy().setTopicPrefetch(topicPrefetch);
        activeMqConnectionFactory.getRedeliveryPolicy().setMaximumRedeliveries(maximumRedeliveries);
        activeMqConnectionFactory.setBrokerURL(brokerUrl);
        activeMqConnectionFactory.setMaxThreadPoolSize(100);
        return activeMqConnectionFactory;
    }
}
