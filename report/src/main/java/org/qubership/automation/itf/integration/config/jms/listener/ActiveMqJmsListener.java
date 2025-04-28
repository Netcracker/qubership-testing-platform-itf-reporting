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

package org.qubership.automation.itf.integration.config.jms.listener;

import java.util.Objects;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.report.Processor;
import org.qubership.automation.itf.ui.services.ContextService;
import org.qubership.automation.itf.util.MdcHelper;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveMqJmsListener implements MessageListener {

    private final Processor processor;
    private final MdcHelper mdcHelper;

    @Override
    @JmsListener(destination = "${message-broker.reports.queue}",
            containerFactory = "reportingDefaultJmsListenerContainerFactory")
    public void onMessage(Message message) {
        long timeStamp = 0;
        String objectId = null;
        String objectType = null;
        JsonObject json = null;
        String jmsMessageId = null;
        try {
            timeStamp = message.getLongProperty("Time");
            objectId = message.getStringProperty("ObjectID");
            objectType = message.getStringProperty("ObjectType");
            jmsMessageId = message.getJMSMessageID();
            String projectId = message.getStringProperty(CustomHeader.X_PROJECT_ID);
            MdcUtils.put(MdcField.PROJECT_ID.toString(), projectId);
            log.debug("Message is received: time {}, id {}, type {}", timeStamp, objectId, objectType);
            json = getJsonObject(message);
            addObjectTypeProperty(json, objectType);
            int partNum = message.propertyExists("partNum")
                    ? message.getIntProperty("partNum")
                    : ContextService.getCurrentPartitionNumberByProject(projectId);
            json.add("partNum", new JsonPrimitive(partNum));
            json.add(CustomHeader.X_PROJECT_ID, new JsonPrimitive(projectId));
            processor.process(json, timeStamp, objectId, objectType);
            log.debug("Message is processed: time {}, id {}, type {}", timeStamp, objectId, objectType);
        } catch (Throwable e) {
            fillMdcFields(json);
            log.error("Error while message processing: time {}, id {}, type {}, jmsMessageId {}",
                    timeStamp, objectId, objectType, jmsMessageId, e);
        } finally {
            MDC.clear();
        }
    }

    private JsonObject getJsonObject(Message textMessage) throws JMSException {
        return JsonParser.parseString(((TextMessage) textMessage).getText()).getAsJsonObject();
    }

    private void addObjectTypeProperty(@NotNull JsonObject json, String objectType) {
        if (Objects.isNull(objectType) || objectType.isEmpty()) {
            throw new IllegalArgumentException("Object type is required but missed; message processing is terminated");
        }
        json.add("objectType", new JsonPrimitive(objectType));
    }

    private void fillMdcFields(JsonObject json) {
        if (Objects.isNull(json)) {
            log.error("Can't fill MDC fields, json object is null.");
            return;
        }
        mdcHelper.fillMdsFields(json);
    }
}
