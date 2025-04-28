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

package org.qubership.automation.itf.integration.config.jms.factory;

import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Configuration
@EnableJms
public class JmsListenerContainerFactoryConfiguration {

    @Value("${receiver.listenerContainerFactory.concurrency}")
    private String concurrency;
    @Value("${receiver.listenerContainerFactory.maxMessagesPerTask}")
    private Integer maxMessagesPerTask;

    /**
     * Constructor.
     *
     * @param activeMqConnectionFactory - ActiveMq connection factory,
     * @param jmsListenerContainerFactoryInstance - Jms Listener Container Factory,
     * @return Reporting Queue Jms Listener Container Factory.
     */
    @Bean
    public DefaultJmsListenerContainerFactory reportingDefaultJmsListenerContainerFactory(
            ActiveMQConnectionFactory activeMqConnectionFactory,
            DefaultJmsListenerContainerFactory jmsListenerContainerFactoryInstance) {
        jmsListenerContainerFactoryInstance.setConnectionFactory(activeMqConnectionFactory);
        jmsListenerContainerFactoryInstance.setConcurrency(concurrency);
        jmsListenerContainerFactoryInstance.setMaxMessagesPerTask(maxMessagesPerTask);
        jmsListenerContainerFactoryInstance.setPubSubDomain(false);
        jmsListenerContainerFactoryInstance.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
        jmsListenerContainerFactoryInstance.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        jmsListenerContainerFactoryInstance.setAutoStartup(false);
        return jmsListenerContainerFactoryInstance;
    }
}
