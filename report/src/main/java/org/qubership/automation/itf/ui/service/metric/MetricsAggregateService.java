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

package org.qubership.automation.itf.ui.service.metric;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class MetricsAggregateService {

    @Value("${message-broker.reports.queue}")
    private String destinationQueueName;
    private final ApplicationContext applicationContext;
    private DefaultMessageListenerContainer defaultMessageListenerContainer;
    private final JmsListenerEndpointRegistry jmsListenerEndpointRegistry;
    private final MeterRegistry meterRegistry;

    /**
     * Constructor.
     *
     * @param applicationContext - Application Context,
     * @param meterRegistry - Meter Registry,
     * @param jmsListenerEndpointRegistry - Jms Listener Endpoint Registry.
     */
    @Autowired
    public MetricsAggregateService(ApplicationContext applicationContext, MeterRegistry meterRegistry,
                                   JmsListenerEndpointRegistry jmsListenerEndpointRegistry) {
        this.applicationContext = applicationContext;
        this.jmsListenerEndpointRegistry = jmsListenerEndpointRegistry;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Context refreshed event listener.
     *
     * @param event - context refreshed event.
     */
    @EventListener
    public void init(ContextRefreshedEvent event) {
        if (event.getSource().equals(applicationContext)) {
            contextInitialized();
            fillJmsListenerStatsMetric();
        }
    }

    private void contextInitialized() {
        for (MessageListenerContainer messageListenerContainer : jmsListenerEndpointRegistry.getListenerContainers()) {
            if (destinationQueueName.equals(
                    ((DefaultMessageListenerContainer) messageListenerContainer).getDestinationName())) {
                defaultMessageListenerContainer = (DefaultMessageListenerContainer) messageListenerContainer;
                break;
            }
        }
    }

    private void fillJmsListenerStatsMetric() {
        initializeGauges(Metric.ATP_ITF_REPORTING_JMS_LISTENER_THREAD_POOL_ACTIVE_SIZE,
                () -> defaultMessageListenerContainer.getActiveConsumerCount());
        initializeGauges(Metric.ATP_ITF_REPORTING_JMS_LISTENER_THREAD_POOL_MAX_SIZE,
                () -> defaultMessageListenerContainer.getMaxConcurrentConsumers());
    }

    private void initializeGauges(Metric metric, Supplier<Number> supplier) {
        Gauge.builder(metric.getValue(), supplier).register(meterRegistry);
    }
}
