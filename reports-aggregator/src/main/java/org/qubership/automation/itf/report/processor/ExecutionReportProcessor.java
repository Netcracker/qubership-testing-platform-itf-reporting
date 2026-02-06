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

package org.qubership.automation.itf.report.processor;

import java.sql.SQLException;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionReportProcessor extends AbstractProcessor {

    private final ExecutionReportQueryExecutor queryExecutor;

    protected void process(JsonObject jsonObject) throws SQLException {
        String type = jsonObject.get("objectType").getAsString();
        queryExecutor.getStorageByType(type).store(jsonObject, queryExecutor);
        logIfDebug(jsonObject);
    }

    private void logIfDebug(JsonObject jsonObject) {
        if (!log.isDebugEnabled()) {
            return;
        }
        JsonElement jsonId = jsonObject.get("id");
        JsonElement jsonName = jsonObject.get("name");
        if (Objects.nonNull(jsonId) && Objects.nonNull(jsonName)) {
            log.debug("Processed event, id {}, name {}",
                    jsonId.isJsonNull() ? "null" : jsonId.getAsString(),
                    jsonName.isJsonNull() ? "null" : jsonName.getAsString());
        }
    }
}
