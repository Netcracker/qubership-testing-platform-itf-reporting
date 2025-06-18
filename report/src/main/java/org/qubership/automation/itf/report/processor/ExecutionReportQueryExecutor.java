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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.report.statement.StatementContext;
import org.qubership.automation.itf.util.MdcHelper;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExecutionReportQueryExecutor extends AbstractQueryExecutor implements SqlQueries {

    private static final int MAX_PG_VARCHAR_SIZE = 255;
    private static final String[] PROP_NAMES = {"name", "description"};
    private static final int[] PROP_SIZES = {MAX_PG_VARCHAR_SIZE, MAX_PG_VARCHAR_SIZE};
    private static final String PART_NUM_PROPERTY_NAME = "partNum";
    private final Map<String, Storage> queryMapping = Maps.newHashMap();

    private final MdcHelper mdcHelper;
    private static String atpCatalogueUrl;

    /**
     * Constructor.
     *
     * @param jdbcTemplate - Jdbc Template
     * @param mdcHelper - Mdc Helper
     */
    @Autowired
    public ExecutionReportQueryExecutor(JdbcTemplate jdbcTemplate, MdcHelper mdcHelper) {
        this.mdcHelper = mdcHelper;
        setJdbcTemplate(jdbcTemplate);
    }

    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification = "Looks no problem with it")
    @Value("${atp.catalogue.url}")
    public void setAtpCatalogueUrl(String atpCatalogueUrl) {
        ExecutionReportQueryExecutor.atpCatalogueUrl = atpCatalogueUrl;
    }

    private static JsonObject trunc_propValues(JsonObject obj) {
        for (int k = 0; k < PROP_NAMES.length; k++) {
            if (obj.has(PROP_NAMES[k])) {
                JsonElement prop = obj.get(PROP_NAMES[k]);
                if (!prop.isJsonNull() && prop.isJsonPrimitive()) {
                    String val = prop.getAsString();
                    if (val.length() > PROP_SIZES[k]) {
                        obj.remove(PROP_NAMES[k]);
                        obj.add(PROP_NAMES[k], new JsonPrimitive(val.substring(0, PROP_SIZES[k])));
                    }
                }
            }
        }
        return obj;
    }

    private static String makeContextLink(Object tcContextId, String projectUuid) {
        return atpCatalogueUrl + "/project/" + projectUuid + "/itf#/context/" + tcContextId;
    }

    @PostConstruct
    private void fillQueryMap() {
        queryMapping.put("Combined_TcContext_Initiator",
                new ExecutionReportQueryExecutor.CombinedTcContextInitiatorStorage());
        queryMapping.put("Combined_StepInstance_SituationInstance",
                new ExecutionReportQueryExecutor.CombinedStepInstanceSituationInstance());
        queryMapping.put("Combined_SituationInstance_StepInstances",
                new ExecutionReportQueryExecutor.CombinedSituationInstanceStepInstances());
        queryMapping.put("combinedFastStubMessage",
                new ExecutionReportQueryExecutor.CombinedFastStubMessage());
        queryMapping.put(TcContext.class.getSimpleName(),
                new ExecutionReportQueryExecutor.TcContextStorage());
        queryMapping.put(InstanceContext.class.getSimpleName(),
                new ExecutionReportQueryExecutor.InstanceContextStorage());
        queryMapping.put(SpContext.class.getSimpleName(),
                new ExecutionReportQueryExecutor.SpContextStorage());
        queryMapping.put(StepInstance.class.getSimpleName(),
                new ExecutionReportQueryExecutor.StepInstanceStorage());
        queryMapping.put(SituationInstance.class.getSimpleName(),
                new ExecutionReportQueryExecutor.SituationInstanceStorage());
        queryMapping.put(CallChainInstance.class.getSimpleName(),
                new ExecutionReportQueryExecutor.CallChainInstanceStorage());
        queryMapping.put(Message.class.getSimpleName(),
                new ExecutionReportQueryExecutor.MessageStorage());
        queryMapping.put(MessageParameter.class.getSimpleName(),
                new ExecutionReportQueryExecutor.MessageParameterStorage());
        queryMapping.put("MessageParameterValue",
                new ExecutionReportQueryExecutor.MessageParameterValueStorage());
        queryMapping.put("no",
                new ExecutionReportQueryExecutor.NoStorage());
    }

    protected Object prepareData(ResultSet resultSet, JsonObject object) {
        Long id = 0L;
        if (resultSet != null) {
            try {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            } catch (SQLException e) {
                mdcHelper.fillMdsFields(object);
                log.error("ERROR in prepare data:", e);
                MDC.clear();
                return id;
            }
        }
        return id;
    }

    /**
     * Get storage class by type of object.
     *
     * @param objectType - type of object,
     * @return storage class corresponding to the type of object.
     */
    public ExecutionReportQueryExecutor.Storage getStorageByType(String objectType) {
        if (Objects.isNull(objectType) || objectType.isEmpty()) {
            throw new IllegalArgumentException("Object type is required to process reported message correctly");
        }
        return queryMapping.get(objectType);
    }

    /**
     * Process list or map of entities.
     */
    private void forEach(JsonObject object, String property, StatementContext sql,
                         boolean truncate, boolean isMap, String parentId, String onConflict) {
        JsonObject jsonObject = new JsonObject();
        JsonElement elem = object.get(property);
        if (elem.isJsonNull()) {
            return;
        }
        if (elem.isJsonObject() && elem.getAsJsonObject().entrySet().isEmpty()) {
            return;
        }
        if (elem.isJsonArray() && elem.getAsJsonArray().isEmpty()) {
            return;
        }
        if (isMap) {
            forEachMapPrepareStatementContext(object, property, sql, truncate, jsonObject, parentId);
        } else {
            forEachPrepareStatementContext(object, property, sql, truncate, jsonObject, parentId);
        }
        if (!StringUtils.isBlank(onConflict)) {
            sql = sql.appendQueryPart(onConflict);
        }
        this.execute(sql, jsonObject, false);
    }

    private void forEachMap(JsonObject object, String property, StatementContext sql,
                            @SuppressWarnings("SameParameterValue") boolean truncate, String onConflict) {
        forEach(object, property, sql, truncate, true, "id", onConflict);
    }

    private void forEachPrepareStatementContext(JsonObject object, String property, StatementContext sql,
                                                Boolean truncate, JsonObject jsonObject, String parentId) {
        int count = 1;
        jsonObject.add(PART_NUM_PROPERTY_NAME, object.get(PART_NUM_PROPERTY_NAME));
        jsonObject.add(parentId, object.get(parentId));
        JsonArray jsonArray = object.get(property).getAsJsonArray();
        Iterator<JsonElement> iterator = jsonArray.iterator();
        while (iterator.hasNext()) {
            JsonElement jsonElement = iterator.next();
            String valueName = SqlQueries.VALUE + (count++);
            cutAndFixValue(truncate, jsonElement, jsonObject, valueName);
            sql = sql.appendQueryPart("(")
                    .asLong(parentId)
                    .appendQueryPart(",").asInt(PART_NUM_PROPERTY_NAME)
                    .appendQueryPart(",").asString(valueName)
                    .appendQueryPart(")");
            if (iterator.hasNext()) {
                sql = sql.appendQueryPart(",");
            }
        }
    }

    private void forEachMapPrepareStatementContext(JsonObject object, String property,
                                                   StatementContext sql, Boolean truncate,
                                                   JsonObject jsonObject, String idElemName) {
        JsonElement propElem = object.get(property);
        if (propElem != null && !propElem.isJsonNull()) {
            jsonObject.add(PART_NUM_PROPERTY_NAME, object.get(PART_NUM_PROPERTY_NAME));
            jsonObject.add(idElemName, object.get(idElemName));
            int count = 1;
            JsonObject jsonMap = propElem.getAsJsonObject();
            Iterator<Map.Entry<String, JsonElement>> entryIterator = jsonMap.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, JsonElement> entry = entryIterator.next();
                String keyName = SqlQueries.KEY + count;
                String valueName = SqlQueries.VALUE + (count++);
                jsonObject.add(keyName, new JsonPrimitive(
                        StringUtils.substring(entry.getKey(), 0, MAX_PG_VARCHAR_SIZE)));
                cutAndFixValue(truncate, entry.getValue(), jsonObject, valueName);
                sql = sql.appendQueryPart("(")
                        .asLong(idElemName)
                        .appendQueryPart(",").asInt(PART_NUM_PROPERTY_NAME)
                        .appendQueryPart(",").asString(valueName)
                        .appendQueryPart(",").asString(keyName)
                        .appendQueryPart(")");
                if (entryIterator.hasNext()) {
                    sql = sql.appendQueryPart(",");
                }
            }
        }
    }

    /**
     * Defense on PostgreSQL error:
     *  org.postgresql.util.PSQLException: ERROR: invalid byte sequence for encoding "UTF8": 0x00.
     *
     * @param source - source string,
     * @return - source string (in case it's null or empty),
     *              otherwise string with "\u0000" replaced to StringUtils.EMPTY.
     */
    public static String fixUnicodeZeroByteSequence(String source) {
        return StringUtils.isEmpty(source) ? source : source.replace("\u0000", StringUtils.EMPTY);
    }

    private void cutAndFixValue(boolean truncate, JsonElement value, JsonObject jsonObject, String valueName) {
        if (value.isJsonNull()) {
            jsonObject.add(valueName, value);
        } else {
            String s = fixUnicodeZeroByteSequence(value.isJsonPrimitive() ? value.getAsString() : value.toString());
            jsonObject.add(valueName, new JsonPrimitive(truncate && s.length() > MAX_PG_VARCHAR_SIZE
                    ? s.substring(0, MAX_PG_VARCHAR_SIZE) : s));
        }
    }

    private boolean isMultiple(JsonObject jsonObject) {
        boolean isMultiple = false;
        if (jsonObject.has("multiple")) {
            try {
                isMultiple = jsonObject.get("multiple").getAsBoolean();
            } catch (Exception e) {
                log.error("Error while getting multiple value from json", e);
            }
        }
        return isMultiple;
    }

    /**
     * Storage Interface.
     */
    public interface Storage {

        Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor)
                throws SQLException;

        Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException;
    }

    private static class CombinedTcContextInitiatorStorage implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor) throws SQLException {
            log.debug("CombinedTcContextInitiatorStorage");
            JsonElement partNum = object.get(PART_NUM_PROPERTY_NAME);
            JsonObject initiatorObject = object.get("Initiator").getAsJsonObject();
            initiatorObject.add(PART_NUM_PROPERTY_NAME, partNum);
            String initiatorType = initiatorObject.get("type").getAsString();
            queryExecutor.execute("SituationInstance".equals(initiatorType)
                    ? UPSERT_SITUATION_INSTANCE : STORE_CALL_CHAIN_INSTANCE, trunc_propValues(initiatorObject), true);

            JsonObject tcContextObject = object.get("TcContext").getAsJsonObject();
            tcContextObject.add(PART_NUM_PROPERTY_NAME, partNum);
            return queryExecutor.getStorageByType(tcContextObject.get("type").getAsString())
                    .store(tcContextObject, queryExecutor);
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor); // parentType is NOT valuable here
        }
    }

    private static class CombinedFastStubMessage implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor) throws SQLException {
            log.debug("CombinedFastStubMessage");
            JsonElement partNum = object.get(PART_NUM_PROPERTY_NAME);
            // Insert initiatorObject
            JsonObject initiatorObject = object.get("SituationInstance").getAsJsonObject();
            initiatorObject.add(PART_NUM_PROPERTY_NAME, partNum);
            Object initiatorId = queryExecutor.execute(INSERT_SITUATION_INSTANCE_GENERATE_ID,
                    trunc_propValues(initiatorObject), true);

            // Insert tcContextObject
            JsonObject tcContextObject = object.get("TcContext").getAsJsonObject();
            tcContextObject.add(PART_NUM_PROPERTY_NAME, partNum);
            tcContextObject.add("initiator", new JsonPrimitive((Long) initiatorId));
            Object tcContextId = queryExecutor.execute(INSERT_TC_CONTEXT_GENERATE_ID,
                    trunc_propValues(tcContextObject), true);

            /* Create "ITF context link" object and store it.
                It's needed because it's impossible to generate on the fast stubs side.
            */
            tcContextObject.add("id", new JsonPrimitive((Long) tcContextId));
            JsonObject reportLinks = tcContextObject.get("reportLinks").getAsJsonObject();
            reportLinks.add("ITF context link",
                    new JsonPrimitive(makeContextLink(tcContextId, tcContextObject.get("projectUuid").getAsString())));
            queryExecutor.forEachMap(tcContextObject, "reportLinks",
                new StatementContext().appendQueryPart(STORE_CONTEXT_REPORT_LINKS.getQuery().toString()),
                false, StringUtils.EMPTY);

            // Update initiatorObject, to store tcContext id.
            initiatorObject.add("id", new JsonPrimitive((Long) initiatorId));
            initiatorObject.add("parentContext", new JsonPrimitive((Long) tcContextId));
            queryExecutor.execute(UPDATE_SITUATION_INSTANCE, initiatorObject, false);

            // Insert stepInstanceObject; all linked contexts & objects are stored under it.
            JsonObject stepInstanceObject = object.get("StepInstance").getAsJsonObject();
            stepInstanceObject.add(PART_NUM_PROPERTY_NAME, partNum);
            stepInstanceObject.add("parent", new JsonPrimitive((Long) initiatorId));
            return queryExecutor.getStorageByType(stepInstanceObject.get("type").getAsString())
                    .store(stepInstanceObject, queryExecutor);
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor); // parentType is NOT valuable here
        }
    }

    private static class TcContextStorage implements Storage {

        /*
         * Prerequisites:
         *  1. Initiator instance is stored earlier, and its initiatorId is present in the TcContext object,
         *  2. partNum property is present in the TcContext object.
         */
        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor) throws SQLException {
            log.debug("TcContextStorage");
            JsonElement partNum = object.get(PART_NUM_PROPERTY_NAME);
            JsonElement idElem = object.get("id");
            queryExecutor.execute(STORE_TC_CONTEXT, trunc_propValues(object), false);
            JsonArray jsonArray = object.get("bindingKeys").getAsJsonArray();
            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.add("id", idElem);
                jsonObject.add("key", element);
                jsonObject.add(PART_NUM_PROPERTY_NAME, partNum);
                queryExecutor.execute(STORE_CONTEXT_BINDING_KEYS, jsonObject, false);
            }
            queryExecutor.forEachMap(object, "reportLinks",
                    new StatementContext().appendQueryPart(STORE_CONTEXT_REPORT_LINKS.getQuery().toString()),
                    false, STORE_CONTEXT_REPORT_LINKS_ON_CONFLICT);
            return idElem.getAsBigInteger();
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor); // parentType is NOT valuable here
        }
    }

    private static class InstanceContextStorage implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor)
                throws SQLException {
            return store(object, queryExecutor, true);
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return "SituationInstance".equals(parentType) ? store(object, queryExecutor, false) : null;
        }

        private Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, boolean storeSp)
                throws SQLException {
            log.debug("InstanceContextStorage");
            // After ATPII-41822, 2023-12-19, it's ensured that id always be generated on executor side.
            // But, for reported from fast-stubs messages, ids are generated here.
            Object id = queryExecutor.execute(STORE_INSTANCE_CONTEXT, trunc_propValues(object), true);
            if (object.get("id").isJsonNull()) {
                object.add("id", new JsonPrimitive((Long) id));
            }
            if (storeSp && !object.get("sp").isJsonNull()) {
                JsonObject spObject = object.getAsJsonObject("sp");
                if (spObject.get("parent").isJsonNull()) {
                    spObject.add("parent", new JsonPrimitive((Long) id));
                }
                spObject.add(PART_NUM_PROPERTY_NAME, object.get(PART_NUM_PROPERTY_NAME));
                queryExecutor.getStorageByType(spObject.get("type")
                        .getAsString()).store(spObject, queryExecutor);
            }
            return id;
        }
    }

    private static class SpContextStorage implements Storage {

        /*
         * After ATPII-41822, 2023-12-19, it's ensured that id always be generated on executor side,
         * for objects: InstanceContext, SpContext, Message and MessageParameter.
         */
        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor)
                throws SQLException {
            log.debug("SpContextStorage");
            Long incomingMessageId = 0L;
            Long outgoingMessageId = 0L;
            processMessage(object, queryExecutor, "incomingMessage", incomingMessageId);
            processMessage(object, queryExecutor, "outgoingMessage", outgoingMessageId);
            Object spId = queryExecutor.execute(STORE_SP_CONTEXT, trunc_propValues(object), true);
            object.remove("id");
            object.add("id", new JsonPrimitive((Long) spId));
            if (!object.get("messageParameters").isJsonNull()) {
                JsonArray array = object.get("messageParameters").getAsJsonArray();
                JsonElement ctxId = object.get("id");
                JsonElement partNum = object.get(PART_NUM_PROPERTY_NAME);
                for (JsonElement jsonMap : array) {
                    if (jsonMap.isJsonObject()) {
                        JsonObject jsonObject = jsonMap.getAsJsonObject();
                        jsonObject.remove("parent");
                        jsonObject.add("parent", ctxId);
                        jsonObject.add(PART_NUM_PROPERTY_NAME, partNum);
                        Object paramId = queryExecutor.getStorageByType("MessageParameter")
                                    .store(jsonObject, queryExecutor);
                        if ((Long) paramId != 0L) {
                            if (queryExecutor.isMultiple(jsonObject)) {
                                jsonObject.addProperty("message_param_id", (Long) paramId);
                                queryExecutor.forEach(jsonObject, "multipleValue",
                                        new StatementContext().appendQueryPart(STORE_MESSAGE_PARAMETER_VALUE_FE_V
                                                .getQuery().toString()),
                                        false, false, "message_param_id", StringUtils.EMPTY);
                                jsonObject.remove("message_param_id");
                            } else {
                                // Like 'multiple values' case, we should check the value against null.
                                // Otherwise, we face an exception at jsonObject.get("singleValue").getAsString()
                                if (!jsonObject.get("singleValue").isJsonNull()) {
                                    JsonObject jsonValue = new JsonObject();
                                    jsonValue.addProperty("message_param_id", (Long) paramId);
                                    jsonValue.add(PART_NUM_PROPERTY_NAME, partNum);
                                    jsonValue.addProperty(SqlQueries.VALUE, jsonObject.get("singleValue")
                                            .getAsString());
                                    queryExecutor.getStorageByType("MessageParameterValue")
                                            .store(jsonValue, queryExecutor);
                                }
                            }
                        }
                    }
                }
            }
            return spId;
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor); // parentType is NOT valuable here
        }

        private void processMessage(JsonObject object, ExecutionReportQueryExecutor queryExecutor,
                                    String elementName, Long messageId)
                throws SQLException {
            log.debug("SpContextStorage - processMessage {}", elementName);
            if (!object.get(elementName).isJsonNull()) {
                if (messageId == 0) {
                    JsonObject msgObject = object.getAsJsonObject(elementName);
                    msgObject.add(PART_NUM_PROPERTY_NAME, object.get(PART_NUM_PROPERTY_NAME));
                    messageId = (Long) queryExecutor.getStorageByType("Message")
                            .store(object.getAsJsonObject(elementName), queryExecutor);
                }
                object.remove(elementName);
                object.add(elementName, messageId == null || messageId == 0
                        ? JsonNull.INSTANCE : new JsonPrimitive(messageId));
            }
        }
    }

    private static class CombinedStepInstanceSituationInstance implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor)
                throws SQLException {
            log.debug("CombinedStepInstanceSituationInstance");
            JsonElement partNum = object.get(PART_NUM_PROPERTY_NAME);
            JsonObject situationInstanceObject = object.get("SituationInstance").getAsJsonObject();
            situationInstanceObject.add(PART_NUM_PROPERTY_NAME, partNum);
            queryExecutor.execute(UPSERT_SITUATION_INSTANCE, trunc_propValues(situationInstanceObject), true);

            JsonObject stepInstanceObject = object.get("StepInstance").getAsJsonObject();
            stepInstanceObject.add(PART_NUM_PROPERTY_NAME, partNum);
            return queryExecutor.getStorageByType(stepInstanceObject.get("type").getAsString())
                    .store(stepInstanceObject, queryExecutor);
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor); // parentType is NOT valuable here
        }
    }

    private static class CombinedSituationInstanceStepInstances implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor)
                throws SQLException {
            log.debug("CombinedStepInstanceSituationInstance");
            JsonElement partNum = object.get(PART_NUM_PROPERTY_NAME);
            JsonObject situationInstanceObject = object.get("SituationInstance").getAsJsonObject();
            situationInstanceObject.add(PART_NUM_PROPERTY_NAME, partNum);
            boolean isInitiator = object.get("isInitiator").getAsBoolean();
            Object id = queryExecutor.execute(isInitiator ? UPSERT_SITUATION_INSTANCE : INSERT_SITUATION_INSTANCE,
                    trunc_propValues(situationInstanceObject), true);

            JsonArray stepInstances = object.get("StepInstances").getAsJsonArray();
            if (!stepInstances.isEmpty()) {
                for (JsonElement item : stepInstances) {
                    JsonObject stepInstanceObject = item.getAsJsonObject();
                    stepInstanceObject.add(PART_NUM_PROPERTY_NAME, partNum);
                    queryExecutor.getStorageByType(stepInstanceObject.get("type").getAsString())
                            .store(stepInstanceObject, queryExecutor);
                }
            }
            return id;
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor); // parentType is NOT valuable here
        }
    }

    private static class StepInstanceStorage implements Storage {

        /*
         * Prerequisites:
         *  1. Parent instance (SituationInstance) is stored earlier, and its id is present in the StepInstance object,
         *  2. partNum property is present in the StepInstance object.
         */
        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor)
                throws SQLException {
            log.debug("StepInstanceStorage");
            JsonElement partNum = object.get(PART_NUM_PROPERTY_NAME);
            Object id = queryExecutor.execute(STORE_STEP_INSTANCE, trunc_propValues(object), true);
            if (id != null && (Long) id != 0L) {
                object.addProperty("id", id.toString());
                JsonObject contextObj = object.getAsJsonObject("context");
                contextObj.add(PART_NUM_PROPERTY_NAME, partNum);
                if (!contextObj.isJsonNull()) {
                    contextObj.addProperty("instance", id.toString());
                    queryExecutor.getStorageByType(contextObj.get("type").getAsString())
                            .store(contextObj, queryExecutor);
                }
            }
            return id;
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor); // parentType is NOT valuable here
        }
    }

    private static class SituationInstanceStorage implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor)
                throws SQLException {
            log.info("SituationInstanceStorage (to check before deletion)");
            return queryExecutor.execute(UPSERT_SITUATION_INSTANCE, trunc_propValues(object), true);
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor);
        }
    }

    private static class CallChainInstanceStorage implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor)
                throws SQLException {
            log.debug("CallChainInstanceStorage");
            return queryExecutor.execute(STORE_CALL_CHAIN_INSTANCE, trunc_propValues(object), true);
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor);
        }
    }

    private static class MessageStorage implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor) throws SQLException {
            log.debug("MessageStorage");
            Object messageId = queryExecutor.execute(STORE_MESSAGE, object, true);
            if (messageId != null && (Long) messageId != 0L) {
                object.remove("id");
                object.add("id", new JsonPrimitive((Long) messageId));
                /*
                    Please note:
                        - the last parameter - onConflict - is empty in the following commands,
                        because the current java code and messages sending process guarantee
                        that a message is stored once.
                        So, no conflicts can arise.
                * */
                queryExecutor.forEachMap(object, "headers",
                        new StatementContext().appendQueryPart(STORE_MESSAGE_HEADERS.getQuery().toString()),
                        false, StringUtils.EMPTY);
                queryExecutor.forEachMap(object, "connectionProperties",
                        new StatementContext().appendQueryPart(STORE_CONNECTION_PROPS.getQuery().toString()),
                        false, StringUtils.EMPTY);
            }
            return messageId;
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor); // parentType is NOT valuable here
        }
    }

    private static class MessageParameterStorage implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor) throws SQLException {
            log.debug("MessageParameterStorage - before execute");
            return queryExecutor.execute(STORE_MESSAGE_PARAMETER, object, true);
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor); // parentType is NOT valuable here
        }
    }

    private static class MessageParameterValueStorage implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor) throws SQLException {
            log.debug("MessageParameterValueStorage - before execute");
            JsonElement value = object.get("value");
            if (!value.isJsonNull()) {
                object.add("value", new JsonPrimitive(fixUnicodeZeroByteSequence(
                        value.isJsonPrimitive() ? value.getAsString() : value.toString())));
            }
            return queryExecutor.execute(STORE_MESSAGE_PARAMETER_VALUE, object, true);
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor);
        }
    }

    private static class NoStorage implements Storage {

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor) {
            log.warn("Type storage is no");
            return "NoStorage";
        }

        @Override
        public Object store(JsonObject object, ExecutionReportQueryExecutor queryExecutor, String parentType)
                throws SQLException {
            return store(object, queryExecutor);
        }
    }
}
