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

import static org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants.REPORT_EXECUTION_SCHEMA;

import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.report.statement.StatementContext;

@SuppressWarnings("Duplicates")
public interface SqlQueries {
    String DBSCHEMA = Config.getConfig().getString(REPORT_EXECUTION_SCHEMA);
    String KEY = "key";
    String VALUE = "value";

    @SuppressWarnings("CPD-START")
    StatementContext STORE_TC_CONTEXT = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_context as tc (id, type, name, "
                    + "extensions, json_string, initiator_id, project_id, environment_id, "
                    + "environment_name, status, start_time, end_time, client, last_update_time, "
                    + "time_to_live, pod_name, part_num) VALUES (")
            .asLong("id")
            .appendQueryPart(", ").asString("type")
            .appendQueryPart(", ").asString("name")
            .appendQueryPart(", ").asString("extensionsJson")
            .appendQueryPart(", ").asString("jsonString")
            .appendQueryPart(", ").asLong("initiator")
            .appendQueryPart(", ").asLong("projectId")
            .appendQueryPart(", ").asLong("environmentId")
            .appendQueryPart(", ").asString("environmentName")
            .appendQueryPart(", ").asString("status")
            .appendQueryPart(", ").asDataTime("startTime")
            .appendQueryPart(", ").asDataTime("endTime")
            .appendQueryPart(", ").asString("client")
            .appendQueryPart(", ").asLong("lastUpdateTime")
            .appendQueryPart(", ").asLong("timeToLive")
            .appendQueryPart(", ").asString("podName")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") ON CONFLICT (id, part_num) DO  UPDATE SET ")
            .appendQueryPart(" type=").asString("type")
            .appendQueryPart(", name=").asString("name")
            .appendQueryPart(", extensions=").asString("extensionsJson")
            .appendQueryPart(", json_string=").asString("jsonString")
            .appendQueryPart(", initiator_id=").asLong("initiator")
            .appendQueryPart(", project_id=").asLong("projectId")
            .appendQueryPart(", environment_id=").asLong("environmentId")
            .appendQueryPart(", environment_name=").asString("environmentName")
            .appendQueryPart(", status=").asString("status")
            .appendQueryPart(", start_time=").asDataTime("startTime")
            .appendQueryPart(", end_time=").asDataTime("endTime")
            .appendQueryPart(", client=").asString("client")
            .appendQueryPart(", last_update_time=").asLong("lastUpdateTime")
            .appendQueryPart(", time_to_live=").asLong("timeToLive")
            .appendQueryPart(", pod_name=").asString("podName")
            .appendQueryPart(" where tc.id = EXCLUDED.id and tc.part_num = EXCLUDED.part_num");

    StatementContext INSERT_TC_CONTEXT_GENERATE_ID = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_context as tc (id, type, name, "
                    + "extensions, json_string, initiator_id, project_id, environment_id, "
                    + "environment_name, status, start_time, end_time, client, last_update_time, "
                    + "time_to_live, pod_name, part_num) VALUES (")
            .appendQueryPart("(SELECT getid())")
            .appendQueryPart(", ").asString("type")
            .appendQueryPart(", ").asString("name")
            .appendQueryPart(", ").asString("extensionsJson")
            .appendQueryPart(", ").asString("jsonString")
            .appendQueryPart(", ").asLong("initiator")
            .appendQueryPart(", ").asLong("projectId")
            .appendQueryPart(", ").asLong("environmentId")
            .appendQueryPart(", ").asString("environmentName")
            .appendQueryPart(", ").asString("status")
            .appendQueryPart(", ").asDataTime("startTime")
            .appendQueryPart(", ").asDataTime("endTime")
            .appendQueryPart(", ").asString("client")
            .appendQueryPart(", ").asLong("lastUpdateTime")
            .appendQueryPart(", ").asLong("timeToLive")
            .appendQueryPart(", ").asString("podName")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") RETURNING id");

    StatementContext STORE_CONTEXT_BINDING_KEYS = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_context_binding_keys (id, \"key\", part_num) VALUES (")
            .asLong("id")
            .appendQueryPart(",").asString("key")
            .appendQueryPart(",").asInt("partNum")
            .appendQueryPart(") ON CONFLICT (id,\"key\", part_num) DO NOTHING");

    // Reporting commands for "mb_context_report_links" - START
    StatementContext STORE_CONTEXT_REPORT_LINKS = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA
                    + ".mb_context_report_links as rl (parent_id, part_num, value, \"key\") VALUES ");

    String STORE_CONTEXT_REPORT_LINKS_ON_CONFLICT = " ON CONFLICT (parent_id,\"key\", part_num) DO  "
            + "UPDATE set value=EXCLUDED.value "
            + "where rl.parent_id=EXCLUDED.parent_id "
            + "and rl.\"key\"=EXCLUDED.\"key\""
            + "and rl.part_num=EXCLUDED.part_num";
    // Reporting commands for "mb_context_report_links" - END

    StatementContext STORE_INSTANCE_CONTEXT = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_context as icc (id, type, name, "
                    + "extensions, json_string, session_id, instance, tc_id, part_num) VALUES (")
            .appendQueryPart("(SELECT case when ").asLong("id").appendQueryPart(" is null then(SELECT getid()) else ")
            .asLong("id").appendQueryPart(" end)")
            .appendQueryPart(", ").asString("type")
            .appendQueryPart(", ").asString("name")
            .appendQueryPart(", ").asString("extensionsJson")
            .appendQueryPart(", ").asString("jsonString")
            .appendQueryPart(", ").asLong("session_id")
            .appendQueryPart(", ").asLong("instance")
            .appendQueryPart(", ").asLong("tc_id")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") ON CONFLICT (id, part_num) DO NOTHING RETURNING id");

    StatementContext STORE_SP_CONTEXT = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_context as spc (id, type, name, "
                    + "extensions, json_string, step_id, incoming_message_id, outgoing_message_id, "
                    + "parent_ctx_id, validation_results, part_num) VALUES (")
            .appendQueryPart("(SELECT case when ").asLong("id").appendQueryPart(" is null then(SELECT getid()) else ")
            .asLong("id").appendQueryPart(" end)")
            .appendQueryPart(", ").asString("type")
            .appendQueryPart(", ").asString("name")
            .appendQueryPart(", ").asString("extensionsJson")
            .appendQueryPart(", ").asString("jsonString")
            .appendQueryPart(", ").asLong("stepId")
            .appendQueryPart(", ").asLong("incomingMessage")
            .appendQueryPart(", ").asLong("outgoingMessage")
            .appendQueryPart(", ").asLong("parent")
            .appendQueryPart(", ").asStringFixedZero("validationResults")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") RETURNING id");

    StatementContext STORE_STEP_INSTANCE = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_instance as st (id, type, name, "
                    + "status, start_time, end_time, error_name, "
                    + "error_message, parent_id, step_id, part_num) VALUES (")
            .appendQueryPart("(SELECT case when ").asLong("id").appendQueryPart(" is null then(SELECT getid()) else ")
            .asLong("id").appendQueryPart(" end)")
            .appendQueryPart(", ").asString("type")
            .appendQueryPart(", ").asString("name")
            .appendQueryPart(", ").asString("status")
            .appendQueryPart(", ").asDataTime("startTime")
            .appendQueryPart(", ").asDataTime("endTime")
            .appendQueryPart(", ").asString("errorName")
            .appendQueryPart(", ").asString("errorMessage")
            .appendQueryPart(", ").asLong("parent")
            .appendQueryPart(", ").asLong("stepId")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") RETURNING id");

    StatementContext INSERT_SITUATION_INSTANCE = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_instance as sit (id, type, name, "
                    + "status, start_time, end_time, error_name, error_message, parent_id, context_id, "
                    + "situation_id, operation_name, system_name, system_id, part_num) VALUES (")
            .asLong("id")
            .appendQueryPart(", ").asString("type")
            .appendQueryPart(", ").asString("name")
            .appendQueryPart(", ").asString("status")
            .appendQueryPart(", ").asDataTime("startTime")
            .appendQueryPart(", ").asDataTime("endTime")
            .appendQueryPart(", ").asString("errorName")
            .appendQueryPart(", ").asString("errorMessage")
            .appendQueryPart(", ").asLong("parent")
            .appendQueryPart(", ").asLong("parentContext")
            .appendQueryPart(", ").asLong("situationId")
            .appendQueryPart(", ").asString("operationName")
            .appendQueryPart(", ").asString("systemName")
            .appendQueryPart(", ").asLong("systemId")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") ON CONFLICT (id, part_num) DO NOTHING RETURNING id");

    StatementContext INSERT_SITUATION_INSTANCE_GENERATE_ID = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_instance as sit (id, type, name, "
                    + "status, start_time, end_time, error_name, error_message, parent_id, context_id, "
                    + "situation_id, operation_name, system_name, system_id, part_num) VALUES (")
            .appendQueryPart("(SELECT getid())")
            .appendQueryPart(", ").asString("type")
            .appendQueryPart(", ").asString("name")
            .appendQueryPart(", ").asString("status")
            .appendQueryPart(", ").asDataTime("startTime")
            .appendQueryPart(", ").asDataTime("endTime")
            .appendQueryPart(", ").asString("errorName")
            .appendQueryPart(", ").asString("errorMessage")
            .appendQueryPart(", ").asLong("parent")
            .appendQueryPart(", ").asLong("parentContext")
            .appendQueryPart(", ").asLong("situationId")
            .appendQueryPart(", ").asString("operationName")
            .appendQueryPart(", ").asString("systemName")
            .appendQueryPart(", ").asLong("systemId")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") RETURNING id");

    StatementContext UPSERT_SITUATION_INSTANCE = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_instance as sit (id, type, name, "
                    + "status, start_time, end_time, error_name, error_message, parent_id, context_id, "
                    + "situation_id, operation_name, system_name, system_id, part_num) VALUES (")
            .asLong("id")
            .appendQueryPart(", ").asString("type")
            .appendQueryPart(", ").asString("name")
            .appendQueryPart(", ").asString("status")
            .appendQueryPart(", ").asDataTime("startTime")
            .appendQueryPart(", ").asDataTime("endTime")
            .appendQueryPart(", ").asString("errorName")
            .appendQueryPart(", ").asString("errorMessage")
            .appendQueryPart(", ").asLong("parent")
            .appendQueryPart(", ").asLong("parentContext")
            .appendQueryPart(", ").asLong("situationId")
            .appendQueryPart(", ").asString("operationName")
            .appendQueryPart(", ").asString("systemName")
            .appendQueryPart(", ").asLong("systemId")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") ON CONFLICT (id, part_num) DO UPDATE SET ")
            .appendQueryPart(" type=").asString("type")
            .appendQueryPart(", name=").asString("name")
            .appendQueryPart(", status=").asString("status")
            .appendQueryPart(", start_time=").asDataTime("startTime")
            .appendQueryPart(", end_time=").asDataTime("endTime")
            .appendQueryPart(", error_name=").asString("errorName")
            .appendQueryPart(", error_message=").asString("errorMessage")
            .appendQueryPart(", parent_id=").asLong("parent")
            .appendQueryPart(", context_id=").asLong("parentContext")
            .appendQueryPart(", situation_id=").asLong("situationId")
            .appendQueryPart(", operation_name=").asString("operationName")
            .appendQueryPart(", system_name=").asString("systemName")
            .appendQueryPart(", system_id=").asLong("systemId")
            .appendQueryPart(" where sit.id = EXCLUDED.id and sit.part_num = EXCLUDED.part_num")
            .appendQueryPart(" RETURNING id");

    StatementContext UPDATE_SITUATION_INSTANCE = new StatementContext()
            .appendQueryPart("UPDATE " + DBSCHEMA + ".mb_instance set context_id=").asLong("parentContext")
            .appendQueryPart(" where id = ").asLong("id")
            .appendQueryPart(" and part_num = ").asInt("partNum")
            .appendQueryPart(" RETURNING id");

    StatementContext STORE_CALL_CHAIN_INSTANCE = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_instance as cci (id, type, name, "
                    + "status, start_time, end_time, error_name, error_message, parent_id, context_id, "
                    + "chain_id, dataset_name, callchain_execution_data, part_num) VALUES (")
            .asLong("id")
            .appendQueryPart(", ").asString("type")
            .appendQueryPart(", ").asString("name")
            .appendQueryPart(", ").asString("status")
            .appendQueryPart(", ").asDataTime("startTime")
            .appendQueryPart(", ").asDataTime("endTime")
            .appendQueryPart(", ").asString("errorName")
            .appendQueryPart(", ").asString("errorMessage")
            .appendQueryPart(", ").asLong("parent")
            .appendQueryPart(", ").asLong("parentContext")
            .appendQueryPart(", ").asLong("testCaseId")
            .appendQueryPart(", ").asLong("dataSetName")
            .appendQueryPart(", ").asString("callchainExecutionData")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") ON CONFLICT (id, part_num) DO UPDATE SET ")
            .appendQueryPart(" type=").asString("type")
            .appendQueryPart(", name=").asString("name")
            .appendQueryPart(", status=").asString("status")
            .appendQueryPart(", start_time=").asDataTime("startTime")
            .appendQueryPart(", end_time=").asDataTime("endTime")
            .appendQueryPart(", error_name=").asString("errorName")
            .appendQueryPart(", error_message=").asString("errorMessage")
            .appendQueryPart(", parent_id=").asLong("parent")
            .appendQueryPart(", context_id=").asLong("parentContext")
            .appendQueryPart(", chain_id=").asLong("testCaseId")
            .appendQueryPart(", dataset_name=").asLong("dataSetName")
            .appendQueryPart(", callchain_execution_data=").asString("callchainExecutionData")
            .appendQueryPart(" where cci.id = EXCLUDED.id and cci.part_num = EXCLUDED.part_num")
            .appendQueryPart(" RETURNING id");
    @SuppressWarnings("CPD-END")

    StatementContext STORE_MESSAGE = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA + ".mb_message as msg (id, part_num, text) VALUES (")
            .appendQueryPart("(SELECT case when ").asLong("id").appendQueryPart(" is null then(SELECT getid()) else ")
            .asLong("id").appendQueryPart(" end)")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(", ").asStringFixedZero("text")
            .appendQueryPart(") ON CONFLICT (id, part_num) DO NOTHING RETURNING id");

    StatementContext STORE_MESSAGE_HEADERS = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA
                    + ".mb_message_headers as hdr (parent_id, part_num, value, \"key\") VALUES ");

    StatementContext STORE_CONNECTION_PROPS = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA
                    + ".mb_message_connection_properties as prp (parent_id, part_num, value, \"key\") VALUES ");

    /* Composite index (context_id,param_name) is required to perform this checking efficiently.
        In ideal situation it must be an unique constraint (context_id,param_name),
        if it is created we will replace this checking with a new 'insert...on conflict do update' statement.
        but current data disallow creation of it.
    */
    StatementContext STORE_MESSAGE_PARAMETER = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA
                    + ".mb_message_param as msp (id, param_name, multiple, context_id, part_num) VALUES (")
            .asLong("id")
            .appendQueryPart(", ").asString("paramName")
            .appendQueryPart(", ").asBoolean("multiple")
            .appendQueryPart(", ").asLong("parent")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(") ON CONFLICT (id, part_num) DO NOTHING RETURNING id");

    StatementContext STORE_MESSAGE_PARAMETER_VALUE = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA
                    + ".mb_message_param_multiple_value (message_param_id, part_num, value) VALUES ")
            .appendQueryPart("(").asLong("message_param_id")
            .appendQueryPart(", ").asInt("partNum")
            .appendQueryPart(", ").asString("value")
            .appendQueryPart(") RETURNING message_param_id");
    StatementContext STORE_MESSAGE_PARAMETER_VALUE_FE_V = new StatementContext()
            .appendQueryPart("INSERT INTO " + DBSCHEMA
                    + ".mb_message_param_multiple_value (message_param_id, part_num, value) VALUES ");
}
