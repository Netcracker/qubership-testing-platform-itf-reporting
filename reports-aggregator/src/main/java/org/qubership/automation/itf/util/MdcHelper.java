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

package org.qubership.automation.itf.util;

import java.util.Objects;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.springframework.stereotype.Service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MdcHelper {

    /**
     * Fill MDC fields from json object provided.
     *
     * @param json - json object reported, with extra properties.
     */
    public void fillMdsFields(JsonObject json) {
        JsonElement objectType = json.get("objectType");
        if (Objects.isNull(objectType) || objectType.getAsString().isEmpty()) {
            log.error("Can't get 'objectType' element value from json object to fill MDC fields.");
            return;
        }
        String type = objectType.getAsString();
        try {
            if ("Combined_TcContext_Initiator".equals(type)) {
                JsonElement tcContextObject = json.get("TcContext");
                if (tcContextObject.isJsonObject()) {
                    MdcUtils.put(MdcField.CONTEXT_ID.toString(),
                            tcContextObject.getAsJsonObject().get("id").getAsString());
                }
            } else if ("Combined_StepInstance_SituationInstance".equals(type)) {
                fillFromJson(json.getAsJsonObject("StepInstance"));
            } else if ("Combined_SituationInstance_StepInstances".equals(type)) {
                fillFromJson(json.getAsJsonObject("SituationInstance"));
            } else if ("combinedFastStubMessage".equals(type)) {
                MdcUtils.put(MdcField.PROJECT_ID.toString(), json.get(CustomHeader.X_PROJECT_ID).getAsString());
            } else if ("TcContext".equals(type)) {
                MdcUtils.put(MdcField.CONTEXT_ID.toString(), json.get("id").getAsString());
            } else {
                if ("CallChainInstance".equals(type)) {
                    MdcUtils.put(MdcField.CALL_CHAIN_ID.toString(), json.get("testCaseId").getAsString());
                }
                fillFromJson(json);
            }
        } catch (Exception e) {
            log.error("Error processing message to fill MDC fields.", e);
        }
    }

    private void fillFromJson(JsonObject jsonObject) {
        JsonObject context = jsonObject.getAsJsonObject("context");
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), context.get("tc").getAsString());
        MdcUtils.put(MdcField.PROJECT_ID.toString(), context.get("projectUuid").getAsString());
    }
}
