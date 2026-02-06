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

package org.qubership.automation.itf.ui.controls;

import static org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants.LOG_APPENDER_DATE_FORMAT;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.ContextManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.MonitoringManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.reports.TcContextBriefInfoObjectManager;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContextBriefInfo;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.exceptions.ContextNotFoundException;
import org.qubership.automation.itf.exceptions.IncorrectParameterFormatException;
import org.qubership.automation.itf.report.model.ReportObject;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.monitoring.UIGetReportList;
import org.qubership.automation.itf.ui.messages.objects.UIContextErrors;
import org.qubership.automation.itf.ui.messages.objects.UIReportItem;
import org.qubership.automation.itf.ui.tree.TreeNode;
import org.qubership.automation.itf.ui.tree.TreeNodeTypes;
import org.qubership.automation.itf.ui.tree.TreeNodeUtils;
import org.qubership.automation.itf.util.Maps2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
public class MonitoringController {

    private static final String DATE_FORMAT = Config.getConfig().getString(LOG_APPENDER_DATE_FORMAT);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_FORMAT).withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private static final String DATE_CONDITIONS_FORMAT = "dd.MM.yy";
    private static final String DURATION_CONDITIONS_FORMAT = "HH:mm:ss";

    @Autowired
    public void setEnv(Environment env) {
        this.env = env;
    }

    private Environment env;
    private final LoadingCache<String, String> urls = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(@Nonnull String key) {
                    return env.getProperty(key);
                }
            });

    private static UIReportItem buildUIReportItem(TcContextBriefInfo item) {
        UIReportItem uiReportItem = new UIReportItem();
        uiReportItem.setId(item.getID().toString());
        uiReportItem.setName(item.getName());
        uiReportItem.setEnvironment(item.getEnvname() != null
                ? item.getEnvname().replaceAll("\"", "") : "Environment not set");
        uiReportItem.setSystem(item.getSystemName() != null
                ? item.getSystemName().replaceAll("\"", "") : "System not set");
        uiReportItem.setOperation(item.getOperationName() != null
                ? item.getOperationName().replaceAll("\"", "") : "Operation not set");
        uiReportItem.setInitiator(item.getInitiator() != null ? item.getIniname() : "Initiator not set");
        uiReportItem.setInitiatorType(item.getInitiatortype() != null
                ? item.getInitiatortype() : "Initiator type not set");
        uiReportItem.setStatus(item.getStatus().toString());
        uiReportItem.setStartTime((item.getStartTime() == null) ? "" :
                item.getStartTime().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER));
        uiReportItem.setEndTime((item.getEndTime() == null) ? "" :
                item.getEndTime().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER));
        uiReportItem.setDuration(computeAndFormatDuration(item.getDuration(), item.getStartTime(), item.getEndTime()));
        uiReportItem.setClient(item.getClient());
        uiReportItem.setPartNum(item.getPartNum());
        return uiReportItem;
    }

    /**
     *  Calculate duration for UI.
     *  Temporary, for transition period only:
     *      - When the query, computing duration, is already removed from TcContextBriefInfo mapping,
     *      (calculation of duration is implemented in Postgres triggers)
     *      - but data in mb_tccontext table contain null 'duration' column.
     *      (the corresponding update is not included into migration scripts due to huge amounts of data).
     *  Parameters: Long duration, Date startTime, Date endTime.
     *  Return value: formatted duration String, in "HH:mm:ss" format.
     */
    private static String computeAndFormatDuration(Long duration, Date startTime, Date endTime) {
        long computedDuration;
        if (duration != null) {
            computedDuration = duration;
        } else if (startTime != null) {
            computedDuration = (endTime == null ? new Date().getTime() : endTime.getTime()) - startTime.getTime();
        } else {
            return "";
        }
        return DurationFormatUtils.formatDuration(computedDuration,"HH:mm:ss", true);
    }

    /**
     * Get n-th page of tc contexts.
     *
     * @param projectId - project Id,
     * @param projectUuid - project Uuid,
     * @param pageSize - size of page,
     * @return n-th page of tc contexts.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @RequestMapping(value = "/monitoring/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get 1st page of Monitoring data (page size {{#pageSize}}) "
            + "in the project {{#projectId}}/{{#projectUuid}}")
    public UIGetReportList getPage(@RequestParam BigInteger projectId,
                                   @RequestParam(value = "projectUuid", required = false) UUID projectUuid,
                                   @RequestParam(required = false, defaultValue = "20") int pageSize) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        return getPage(0, pageSize, false, "", "", "", "", "",
                "", "", "", "", "", "", "",
                false, projectId, projectUuid);
    }

    /**
     * Method makes dynamic query and returns n-th page of the result.
     * Returns: n-th page of the query result.
     */
    @SuppressWarnings("CPD-START")
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @RequestMapping(value = "/monitoring/page", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get #{{#page}} page of Monitoring data (page size {{#pageSize}}) "
            + "in the project {{#projectId}}/{{#projectUuid}}."
            + " Search conditions: name {{#name}}, initiator {{#initiator}}, status {{#status}}, "
            + "environment {{#environment}}, startDate {{#startDate}}, startDateCondition {{#startDateCondition}}, "
            + "finishDate {{#finishDate}}, finishDateCondition {{#finishDateCondition}}, duration {{#duration}}, "
            + "durationCondition {{#durationCondition}}, client {{#client}}, sortProperty {{#sortProperty}}")
    public UIGetReportList getPage(@RequestParam int page,
                                   @RequestParam(required = false, defaultValue = "20") int pageSize,
                                   @RequestParam(required = false, defaultValue = "false") boolean search,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) String initiator,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String environment,
                                   @RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String startDateCondition,
                                   @RequestParam(required = false) String finishDate,
                                   @RequestParam(required = false) String finishDateCondition,
                                   @RequestParam(required = false) String duration,
                                   @RequestParam(required = false) String durationCondition,
                                   @RequestParam(required = false) String client,
                                   @RequestParam(required = false) String sortProperty,
                                   @RequestParam(required = false, defaultValue = "false") boolean sortOrder,
                                   @RequestParam BigInteger projectId,
                                   @RequestParam(value = "projectUuid", required = false) UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        UIGetReportList uiReportList = new UIGetReportList();
        Page<TcContextBriefInfo> curPage;
        if (search) {
            Date stDate = checkDateParameter(startDate, "Start date");
            Date finDate = checkDateParameter(finishDate, "Finish date");
            Long durate = checkLongParameter(duration, "Duration");
            curPage = TcContextBriefInfoObjectManager.getPageByFilter(pageSize,
                    page, Boolean.TRUE, name, initiator, status, environment, stDate, startDateCondition, finDate,
                    finishDateCondition, durate, durationCondition, client, sortProperty, sortOrder, projectId);
        } else {
            curPage = TcContextBriefInfoObjectManager.getPage(pageSize, page, projectId);
        }
        List<UIReportItem> uiReportItems = new ArrayList<>();
        for (TcContextBriefInfo context : curPage.getContent()) {
            UIReportItem uiReportItem = buildUIReportItem(context);
            uiReportItems.add(uiReportItem);
        }
        uiReportList.setReportItems(uiReportItems);
        uiReportList.setTotalItems(curPage.getTotalElements());
        uiReportList.setTotalPages(curPage.getTotalPages());
        return uiReportList;
    }

    /**
     * Method makes dynamic query and returns the whole result.
     * Returns: the query result.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @RequestMapping(value = "/monitoring/simpleSearch", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get simple Monitoring search results, parameters: initiator {{#initiator}}, "
            + "status {{#status}}, environment {{#environment}}, minStartDate {{#minStartDate}}, "
            + "maxStartDate {{#maxStartDate}} in the project {{#projectUuid}}")
    public List<UIReportItem> simpleSearch(
            @RequestParam String initiator,
            @RequestParam String status,
            @RequestParam String environment,
            @RequestParam String minStartDate,
            @RequestParam String maxStartDate,
            @RequestParam(value = "projectUuid", required = false) UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        Date minStDate = checkDateParameter(minStartDate, "Start date");
        Date maxStDate = checkDateParameter(maxStartDate, "Finish date");
        Iterable<TcContextBriefInfo> tcc = TcContextBriefInfoObjectManager.simpleSearch(initiator, status,
                environment, minStDate, maxStDate);
        List<UIReportItem> uiReportItems = new ArrayList<>();
        for (TcContextBriefInfo context : tcc) {
            uiReportItems.add(buildUIReportItem(context));
        }
        return uiReportItems;
    }

    /**
     * Method makes dynamic query and deletes tc-contexts returned by the query.
     * Returns: count of deleted contexts.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'EXECUTE')")
    @RequestMapping(value = "/monitoring/deleteContextsByFilter", method = RequestMethod.GET)
    @AuditAction(auditAction = "Delete Monitoring data by conditions: name {{#name}}, initiator {{#initiator}}, "
            + "status {{#status}}, environment {{#environment}}, startDate {{#startDate}}, "
            + "startDateCondition {{#startDateCondition}}, finishDate {{#finishDate}}, "
            + "finishDateCondition {{#finishDateCondition}}, duration {{#duration}}, "
            + "durationCondition {{#durationCondition}}, client {{#client}} "
            + "in the project {{#projectId}}/{{#projectUuid}}")
    public int deleteByFilter(@RequestParam String name,
                              @RequestParam String initiator,
                              @RequestParam String status,
                              @RequestParam String environment,
                              @RequestParam String startDate,
                              @RequestParam String startDateCondition,
                              @RequestParam String finishDate,
                              @RequestParam String finishDateCondition,
                              @RequestParam(required = false) String duration,
                              @RequestParam(required = false) String durationCondition,
                              @RequestParam(required = false) String client,
                              @RequestParam BigInteger projectId,
                              @RequestParam(value = "projectUuid", required = false) UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        Date stDate = checkDateParameter(startDate, "Start date");
        Date finDate = checkDateParameter(finishDate, "Finish date");
        Long durate = checkLongParameter(duration, "Duration");

        ContextManager manager = CoreObjectManager.getInstance()
                .getSpecialManager(TcContext.class, ContextManager.class);

        Iterable<TcContextBriefInfo> tcc = TcContextBriefInfoObjectManager.findByFilter(name, initiator, status,
                environment, stDate, startDateCondition, finDate, finishDateCondition, durate, durationCondition,
                client, projectId);
        Iterator<TcContextBriefInfo> iterator = tcc.iterator();
        int deletedCount = 0;
        while (iterator.hasNext()) {
            TcContextBriefInfo item = iterator.next();
            manager.deleteById(item.getID().toString(), item.getPartNum());
            deletedCount++;
        }
        return deletedCount;
    }

    /**
     * The method retrieves tccontext and its initiator information for context popup.
     * Parameters: id - tccontext id,
     * Returns: UIReportItem.
     * Due to performance problems while getting TcContext by id (experienced at itf_ert on 4.3.1),
     * and because, in fact, TcContext object contains much more information than necessary here,
     * separate native queries are used to retrieve exactly what is needed, nothing more.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @RequestMapping(value = "/monitoring/get", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Tc-context by id {{#id}} in the project {{#projectId}}/{{#projectUuid}}")
    public @ResponseBody UIReportItem getItem(@RequestParam String id,
                         @RequestParam(value = "projectUuid") UUID projectUuid,
                         @RequestParam BigInteger projectId,
                         @RequestParam(value = "standalone", defaultValue = "false") boolean standalone,
                         @RequestParam(required = false) Integer partNum) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), id);
        TcContextBriefInfo briefInfo = CoreObjectManager.getInstance()
                .getSpecialManager(InstanceContext.class, MonitoringManager.class).getTcContextInformation(id, partNum);
        if (Objects.isNull(briefInfo)) {
            throw new ContextNotFoundException(id);
        }
        UIReportItem item = buildUIReportItem(briefInfo);
        setExtraProperties(item, briefInfo);
        Map<String, String> reportLinks = CoreObjectManager.getInstance()
                .getSpecialManager(InstanceContext.class, MonitoringManager.class)
                .getTcContextReportLinks(id, briefInfo.getPartNum());
        replaceReportLinks(reportLinks, standalone, projectUuid, projectId);
        item.setReportLinks(reportLinks);
        item.setReportSituations(CoreObjectManager.getInstance().getSpecialManager(InstanceContext.class,
                MonitoringManager.class).getTcContextStepsSituations(id, briefInfo.getPartNum()));
        item.setBindingKeys(CoreObjectManager.getInstance().getSpecialManager(InstanceContext.class,
                MonitoringManager.class).getTcContextBindingKeys(id, briefInfo.getPartNum()));
        if (briefInfo.getInitiatortype() != null && briefInfo.getInitiatortype().equals("CallChainInstance")) {
            item.setCallchainExecutionData(briefInfo.getExecutiondata());
        }
        return item;
    }

    /**
     * Method gets context contents by id and partNum (in case partitioning is used).
     * Returns: UIReportItem with context set only.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @RequestMapping(value = "/monitoring/getContextVariables", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Context variables of tc-context id {{#id}} in the project {{#projectUuid}}")
    public UIReportItem getContextVariables(@RequestParam(defaultValue = "0") String id,
                                            @RequestParam(value = "projectUuid", required = false) UUID projectUuid,
                                            @RequestParam Integer partNum) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), id);
        UIReportItem uiReportItem = new UIReportItem();
        String contextVariables = CoreObjectManager.getInstance()
                .getSpecialManager(InstanceContext.class, MonitoringManager.class).getContextVariables(id, partNum);
        uiReportItem.setContextVariable(contextVariables);
        return uiReportItem;
    }

    /**
     * Method gets context errors by id and partNum (in case partitioning is used).
     * Returns: context errors.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @RequestMapping(value = "/monitoring/getContextErrors", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Context errors of tc-context id {{#id}} in the project {{#projectUuid}}")
    public UIContextErrors getContextErrors(@RequestParam(defaultValue = "0") String id,
                                            @RequestParam(value = "projectUuid", required = false) UUID projectUuid,
                                            @RequestParam Integer partNum) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), id);
        UIContextErrors errors = new UIContextErrors();
        StringBuilder sbErrorName = new StringBuilder();
        StringBuilder sbErrorMessage = new StringBuilder();
        List<Object[]> errorList = CoreObjectManager.getInstance().getSpecialManager(InstanceContext.class,
                MonitoringManager.class).allTcContextInstancesErrors(id, partNum);
        for (Object[] error : errorList) {
            if (error[2] != null || error[3] != null) {
                sbErrorName.append("<b>Instance: ");
                if (error[2] != null) {
                    sbErrorName.append(error[2]);
                }
                if (error[3] != null) {
                    sbErrorName.append(" [").append(error[3]).append("]");
                }
                sbErrorName.append("</b><br>");
            }
            if (error[0] != null) {
                sbErrorName.append(error[0]).append("<br>");
            }
            if (error[1] != null) {
                sbErrorMessage.append(error[1]).append("<br>");
            }
        }
        errors.setErrorName(sbErrorName.toString());
        errors.setErrorMessage(sbErrorMessage.toString());
        return errors;
    }

    /**
     * Method gets context steps tree by id and partNum (in case partitioning is used).
     * Returns: List of tree nodes.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @RequestMapping(value = "/monitoring/getmessagetree", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Messages Tree of tc-context id {{#id}} in the project {{#projectUuid}}")
    public List<TreeNode> getMessagesTree(@RequestParam(defaultValue = "0") String id,
                                          @RequestParam(value = "projectUuid", required = false) UUID projectUuid,
                                          @RequestParam Integer partNum) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), id);
        List<ReportObject> reportObjects = TreeNodeUtils.getReportObjects(CoreObjectManager.getInstance()
                .getSpecialManager(InstanceContext.class, MonitoringManager.class)
                .getTreeMessagesFromTcContext(id, partNum));
        return TreeNodeUtils.createTreeNodesByReport(reportObjects,
                TreeNodeTypes.SITUATION_INSTANCE.toString(),
                TreeNodeTypes.STEP_INSTANCE.toString(),
                TreeNodeTypes.INCOMING_MESSAGE.toString(),
                TreeNodeTypes.OUTGOING_MESSAGE.toString());
    }

    /**
     * Alexander Kapustin, 2017-10-13, Service getting FULL information about message
     *  Instead of these 5 methods (due to performance degradation while using them):
     *      1. 'monitoring/getconnectionparameters' getConnectionParameters()
     *      2. 'monitoring/getmessage'              getMessage()
     *      3. 'monitoring/getmessageheaders'       getMessageHeaders()
     *      4. 'monitoring/getmessageparameters'    getMessageParameters()
     *      5. 'monitoring/getexception'            getException()
     * Method retrieves all step related information:
     *  - Request (message, headers, configured properties (if any),
     *  - Response (message, headers, configured properties (if any),
     *  - Step context,
     *  - Validation results (if any),
     *  - Runtime errors.
     *  Returns: Map of step information.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @RequestMapping(value = "/monitoring/getmessageinfo", method = RequestMethod.GET, produces = "application/json")
    @AuditAction(auditAction = "Get Messages Info of tc-context id {{#id}} in the project {{#projectUuid}}")
    public @ResponseBody Map<String, Object> getMessageInfo(@RequestParam String id,
                                       @RequestParam(value = "projectUuid", required = false) UUID projectUuid,
                                       @RequestParam Integer partNum) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        MonitoringManager specManager = CoreObjectManager.getInstance()
                .getSpecialManager(InstanceContext.class, MonitoringManager.class);
        HashMap<String, Object> errorInfo = specManager.getStepInstanceError(id, partNum);
        String errorName = (String) errorInfo.get("errorName");
        String errorMessage = (String) errorInfo.get("errorMessage");
        if (StringUtils.isBlank(errorName) && !StringUtils.isBlank(errorMessage)) {
            errorName = errorMessage.substring(0, Math.min(30, errorMessage.length())) + "...";
        }
        Map<String, Object> mapMessageInfo = new HashMap<>();
        mapMessageInfo.put("exception", Maps2.map("errorName", errorName).val("errorMessage", errorMessage).build());
        HashMap<String, Object> ids = specManager.getStepInstanceMessageIds(id, partNum);

        Map<String, String> messageMap = new HashMap<>();
        Object msgId = ids.get("incomingMessageId");
        if (msgId != null) {
            String incoming = specManager.getMessageText(msgId, partNum);
            messageMap.put("Incoming message", incoming);
            Map<String, Object> incomingProps =
                    Maps.newHashMap(specManager.getMessageConnectionProperties(msgId, partNum));
            if (!incomingProps.isEmpty()) {
                mapMessageInfo.put("connectionparameters_incoming", incomingProps);
            }
            Map<String, Object> incomingHeaders =
                    Maps.newHashMap(specManager.getMessageHeaders(msgId, partNum));
            if (!incomingHeaders.isEmpty()) {
                mapMessageInfo.put("messageheaders_incoming", incomingHeaders);
            }
        }
        msgId = ids.get("outgoingMessageId");
        if (msgId != null) {
            String outgoing = specManager.getMessageText(msgId, partNum);
            messageMap.put("Outgoing message", outgoing);
            Map<String, Object> outgoingProps =
                    Maps.newHashMap(specManager.getMessageConnectionProperties(msgId, partNum));
            if (!outgoingProps.isEmpty()) {
                mapMessageInfo.put("connectionparameters_outgoing", outgoingProps);
            }
            Map<String, Object> outgoingHeaders =
                    Maps.newHashMap(specManager.getMessageHeaders(msgId, partNum));
            if (!outgoingHeaders.isEmpty()) {
                mapMessageInfo.put("messageheaders_outgoing", outgoingHeaders);
            }
        }
        Object stepContext = ids.get("stepContext");
        if (stepContext != null) {
            mapMessageInfo.put("stepContext", stepContext.toString());
        }
        mapMessageInfo.put("message", messageMap);
        Object spContextId = ids.get("spContextId");
        if (spContextId != null) {
            String validationResults = specManager.getValidationResults(spContextId, partNum);
            mapMessageInfo.put("validation_results", validationResults);

            Map<String, Object> spMessageParameters =
                    Maps.newHashMap(specManager.getSpMessageParameters(spContextId, partNum));
            if (!spMessageParameters.isEmpty()) {
                mapMessageInfo.put("messageparameters", spMessageParameters);
            }
        }
        return mapMessageInfo;
    }

    /**
     * Method deletes contexts by ids array received.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'EXECUTE')")
    @RequestMapping(value = "/monitoring/deleteSelectedContexts", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete selected contexts from project {{#projectUuid}}")
    public void delete(@RequestBody UIIds uiDeleteObjectReq,
                       @RequestParam(value = "projectUuid", required = false) UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        ContextManager manager = CoreObjectManager.getInstance()
                .getSpecialManager(TcContext.class, ContextManager.class);
        for (String id : uiDeleteObjectReq.getIds()) {
            manager.deleteById(id, null);
        }
    }

    /**
     * Method makes dynamic query, executes it and composes html report
     * which can be used alone.
     * Returns StringBuilder with report prepared.
     */
    //noinspection Duplicates
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @RequestMapping(value = "/monitoring/report", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Monitoring report with parameters: name {{#name}}, initiator {{#initiator}}, "
            + "status {{#status}}, environment {{#environment}}, startDate {{#startDate}}, "
            + "startDateCondition {{#startDateCondition}}, finishDate {{#finishDate}}, "
            + "finishDateCondition {{#finishDateCondition}}, duration {{#duration}}, "
            + "durationCondition {{#durationCondition}}, client {{#client}}, sortProperty {{#sortProperty}} "
            + "in the project {{#projectId}}/{{#projectUuid}}")
    public StringBuilder getReport(@RequestParam(required = false) String name,
                                   @RequestParam(required = false) String initiator,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String environment,
                                   @RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String startDateCondition,
                                   @RequestParam(required = false) String finishDate,
                                   @RequestParam(required = false) String finishDateCondition,
                                   @RequestParam(required = false) String duration,
                                   @RequestParam(required = false) String durationCondition,
                                   @RequestParam(required = false) String client,
                                   @RequestParam(required = false) String sortProperty,
                                   @RequestParam(defaultValue = "false", required = false) boolean sortOrder,
                                   @RequestParam(value = "standalone", defaultValue = "false") boolean standalone,
                                   @RequestParam BigInteger projectId,
                                   @RequestParam(value = "projectUuid") UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        return get_Report(name, initiator, status, environment, startDate, startDateCondition, finishDate,
                finishDateCondition, duration, durationCondition, client, sortProperty, sortOrder,
                standalone, projectId, projectUuid);
    }

    private StringBuilder get_Report(String name,
                                     String initiator,
                                     String status,
                                     String environment,
                                     String startDate,
                                     String startDateCondition,
                                     String finishDate,
                                     String finishDateCondition,
                                     String duration,
                                     String durationCondition,
                                     String client,
                                     String sortProperty,
                                     boolean sortOrder,
                                     boolean standalone,
                                     BigInteger projectId,
                                     UUID projectUuid) {
        Iterable<TcContextBriefInfo> curPage;

        String escName = StringEscapeUtils.escapeHtml4(name);
        String escInitiator = StringEscapeUtils.escapeHtml4(initiator);
        String escStatus = StringEscapeUtils.escapeHtml4(status);
        String escEnvironment = StringEscapeUtils.escapeHtml4(environment);
        String escStartDateCondition = StringEscapeUtils.escapeHtml4(startDateCondition);
        String escFinishDateCondition = StringEscapeUtils.escapeHtml4(finishDateCondition);
        String escDuration = StringEscapeUtils.escapeHtml4(duration);
        String escDurationCondition = StringEscapeUtils.escapeHtml4(durationCondition);
        String escClient = StringEscapeUtils.escapeHtml4(client);
        String escSortProperty = StringEscapeUtils.escapeHtml4(sortProperty);

        Date stDate = checkDateParameter(startDate, "Start date");
        Date finDate = checkDateParameter(finishDate, "Finish date");
        Long durate = checkLongParameter(escDuration, "Duration");

        curPage = TcContextBriefInfoObjectManager.getReportByFilter(escName, escInitiator, escStatus, escEnvironment,
                stDate, escStartDateCondition, finDate, escFinishDateCondition, durate, escDurationCondition, escClient,
                escSortProperty, sortOrder, projectId);
        StringBuilder conditionForGeneratingReport = getConditionForGeneratingReport(escName, escInitiator, escStatus,
                escEnvironment, stDate, escStartDateCondition, finDate, escFinishDateCondition, escDuration,
                escDurationCondition, escClient, escSortProperty);

        StringBuilder htmlReport = new StringBuilder();
        htmlReport.append("<!DOCTYPE html><html><head><title>ITF validations report</title>"
                + "<style> .EXTRA {background-color:orange;} .SIMILAR {color: #000000;background-color: #FFFF77;} "
                + ".BROKEN_STEP_INDEX {background-color:yellow;color:red} .MODIFIED {background-color:#ff9999;} "
                + ".MISSED {background-color:#C7EDFC;}. ERROR {background-color:yellow;color:red} "
                + ".EMPTY_ROW {background: lightgrey;}</style></head>"
                + "<body><h4>ITF validations report (generated at ")
                .append(new Date(System.currentTimeMillis()).toInstant().atZone(ZoneId.systemDefault())
                        .format(DATE_FORMATTER))
                .append(")</h4><table style=\"width: 100%; border-collapse: collapse; margin-left: auto; ")
                .append("margin-right: auto;\" border=\"1\"><thead><tr>")
                .append("<th style=\"width: auto%;\">&#8470;</th>")
                .append("<th style=\"width: auto%;\">Name</th>")
                .append("<th style=\"width: auto%;\">Initiator</th>")
                .append("<th style=\"width: auto%;\">Status</th>")
                .append("<th style=\"width: auto%;\">BV Param</th>")
                .append("<th style=\"width: auto%;\">BV Status</th>")
                .append("<th style=\"width: auto%;\">ER</th>")
                .append("<th style=\"width: auto%;\">AR</th>")
                .append("<th style=\"width: auto%;\">Environment</th>")
                .append("<th style=\"width: auto%;\">Start</th>")
                .append("<th style=\"width: auto%;\">Finish</th>")
                .append("<th style=\"width: auto%;\">Duration</th>")
                .append("<th style=\"width: auto%;\">Client</th>")
                .append("</tr></thead><tbody>");
        int count = 0;
        String linkPrefix = composeLinkToObject(projectId, projectUuid, "", "#/context/", standalone);
        for (TcContextBriefInfo context : curPage) {
            String stat = context.getStatus().toString();
            if (stat.equals("Stopped")) {
                continue;
            }
            StringBuilder bvParam = new StringBuilder();
            StringBuilder bvStatus = new StringBuilder();
            StringBuilder expectedValueBv = new StringBuilder();
            StringBuilder actualValueBv = new StringBuilder();
            if (!stat.equals("Passed")) {
                List<Object[]> errorList = CoreObjectManager.getInstance()
                        .getSpecialManager(InstanceContext.class, MonitoringManager.class)
                        .allTcContextInstancesErrors(context.getID().toString(), context.getPartNum());
                if (!errorList.isEmpty()) {
                    Object[] objects = errorList.get(0);
                    if (objects[1] == null) {
                        continue;
                    }
                    String htmlString = "<!DOCTYPE html><html><head><title>To parse via JSoup</title></head><body>"
                            + objects[1] + "</body></html>";
                    Document html = Jsoup.parse(htmlString);
                    Elements panelHeadings = html.body().getElementsByAttributeValue("class", "panel-heading");
                    for (int i = 0; i < panelHeadings.size(); i++) {
                        Element element = panelHeadings.get(i);
                        String statusBv = element.getElementsByTag("span").get(0).className();
                        if ("IDENTICAL".equals(statusBv)) {
                            continue;
                        }
                        String paramBv = element.ownText();
                        Elements tdElements = html.body()
                                .getElementsByAttributeValue("style", "vertical-align: top;").get(i)
                                .getElementsByTag("td");
                        processDiffs(tdElements.get(0), expectedValueBv);
                        processDiffs(tdElements.get(1), actualValueBv);
                        bvParam.append("<span>").append(paramBv).append("</span><br>");
                        bvStatus.append("<span>").append(statusBv).append("</span><br>");
                    }
                }
            }

            count++;
            String color = stat.equalsIgnoreCase("Passed") ? "#dff0d8;" :
                    stat.equalsIgnoreCase("Failed")
                            || stat.equalsIgnoreCase("Failed by timeout") ? "#f2dede" : ("#ffffff");
            htmlReport.append("<tr style=\"background: ").append(color).append("\"> <td>").append(count)
                    .append("</td>");
            htmlReport.append("<td style=\"text-align: left;\"><a href=\"")
                    .append(linkPrefix).append(context.getID()).append("\" target=\"_blank\">")
                    .append(context.getName()).append("</a></td>");
            htmlReport.append("<td style=\"text-align: left;\">").append(context.getInitiator() != null
                    ? context.getIniname() : "Initiator not set").append("</td>");
            htmlReport.append("<td style=\"text-align: center;\"><span>").append(context.getStatus())
                    .append("</span></td>");
            htmlReport.append("<td style=\"text-align: center;\">").append(bvParam).append("</td>");
            htmlReport.append("<td style=\"text-align: center;\">").append(bvStatus).append("</td>");
            htmlReport.append("<td style=\"text-align: left; vertical-align: top; background: white;\">")
                    .append(expectedValueBv).append("</td>");
            htmlReport.append("<td style=\"text-align: left; vertical-align: top; background: white;\">")
                    .append(actualValueBv).append("</td>");
            htmlReport.append("<td>").append(context.getEnvironment() != null
                    ? context.getEnvname() : "Environment not set").append("</td>");
            htmlReport.append("<td>").append((context.getStartTime() == null) ? "" :
                    context.getStartTime().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER))
                    .append("</td>");
            htmlReport.append("<td>").append((context.getEndTime() == null) ? "" :
                    context.getEndTime().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER))
                    .append("</td>");
            htmlReport.append("<td>").append((context.getDuration() == null) ? "" :
                    DurationFormatUtils.formatDuration(context.getDuration(),
                            "HH:mm:ss", true)).append("</td>");
            htmlReport.append("<td>").append(context.getClient() == null ? "" : context.getClient())
                    .append("</td></tr>");
        }
        htmlReport.append("</tbody></table><h4>Total:").append(count).append("</h4><div>Report conditions & sorting: ")
                .append(conditionForGeneratingReport).append("</div></body></html>");
        return htmlReport;
    }

    private static void processDiffs(Element highlightedValue, StringBuilder stringValueBv) {
        Elements diffElements = highlightedValue.getElementsByAttributeValueMatching(
                "class", "[^(NORMAL|IDENTICAL)]");
        // process expected/actual value BV
        /* Attempt to enumerate diffs - commented, because diffs of XML nodes highlighted by BV separately
            (i.e. if the whole node is MISSED, MISSED spans are added for the node itself and for all children).
            Contrary to XML, diffs in JSON are highlighted as one span with children...
         */
        //int orderNum = 1;
        for (Element value : diffElements) {
            Elements thisChildElement = value.children();
            if (thisChildElement.isEmpty() || value.hasText()) {
                stringValueBv.append("<nobr>")
                        //.append(orderNum++).append(": ")
                        .append(value)
                        .append("</nobr><br>");
            }
        }
    }

    @SuppressWarnings("CPD-END")
    private static void setExtraProperties(UIReportItem uiReportItem, TcContextBriefInfo item) {
        if (Objects.isNull(item.getInitiator())) {
            return;
        }
        if (item.getInitiatortype().equals("SituationInstance")) {
            if (Objects.isNull(item.getSituationId())) {
                return;
            }
            if (StringUtils.isNotEmpty(item.getOperationName())) {
                uiReportItem.setOperation(item.getOperationName());
            }
            if (StringUtils.isNotEmpty(item.getSystemName())) {
                uiReportItem.setSystem(item.getSystemName());
            }
        } else {
            uiReportItem.setInitiatorId((item.getChainId() != null)
                    ? item.getChainId().toString() : "TestCase not set");
        }
    }

    private Date checkDateParameter(String stringValue, String parameterName)
            throws IncorrectParameterFormatException {
        Date dateValue = null;
        if (stringValue != null && !stringValue.isEmpty()) {
            try {
                String value = stringValue.trim();
                if (value.length() == 8) {
                    value += " 00:00:00.000";
                }
                dateValue = Date.from(Instant.from(DATE_TIME_FORMATTER.parse(value)));
            } catch (Exception ex) {
                throw new IncorrectParameterFormatException(parameterName, DATE_CONDITIONS_FORMAT, ex.getMessage());
            }
        }
        return dateValue;
    }

    private static Long checkLongParameter(String stringValue, String parameterName)
            throws IncorrectParameterFormatException {
        Long duration = null;
        if (stringValue != null && !stringValue.isEmpty()) {
            try {
                String[] value = stringValue.trim().split(":");
                duration = (Integer.parseInt(value[0]) * 3600L + Integer.parseInt(value[1]) * 60L
                        + Integer.parseInt(value[2])) * 1000;
            } catch (Exception ex) {
                throw new IncorrectParameterFormatException(parameterName, DURATION_CONDITIONS_FORMAT, ex.getMessage());
            }
        }
        return duration;
    }

    private static StringBuilder getConditionForGeneratingReport(String name, String initiator, String status,
                                                                String environment, Date startDate,
                                                                String startDateCondition, Date finishDate,
                                                                String finishDateCondition, String duration,
                                                                String durationCondition, String client,
                                                                String sortProperty) throws IllegalArgumentException {
        StringBuilder conditionForGeneratingReport = new StringBuilder();
        conditionForGeneratingReport.append(StringUtils.isBlank(name) ? "" : "name - " + name + ", ");
        conditionForGeneratingReport.append(StringUtils.isBlank(initiator) ? "" : "initiator - " + initiator + ", ");
        conditionForGeneratingReport.append(StringUtils.isBlank(status) ? "" : "status - " + status + ", ");
        conditionForGeneratingReport.append(StringUtils.isBlank(environment) ? "" : "environment - "
                + environment + ", ");
        conditionForGeneratingReport.append(Objects.isNull(startDate) ? "" : "start date " + startDateCondition
                + " " + startDate + ", ");
        conditionForGeneratingReport.append(Objects.isNull(finishDate) ? "" : "finish date "
                + finishDateCondition + " " + finishDate + ", ");
        conditionForGeneratingReport.append(StringUtils.isBlank(duration) ? "" : "duration " + durationCondition
                + " " + duration + ", ");
        conditionForGeneratingReport.append(StringUtils.isBlank(client) ? "" : "client - " + client + ", ");
        conditionForGeneratingReport.append("sortProperty - ").append(sortProperty);
        return conditionForGeneratingReport;
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "In fact, null check is performed in the Objects.requireNonNull enclosing method")
    private void replaceReportLinks(Map<String, String> reportLinks, boolean standalone, UUID projectUuid,
                                    BigInteger projectId) {
        if (reportLinks == null || reportLinks.isEmpty() || !standalone || projectUuid == null || projectId == null) {
            return;
        }
        String reg = urls.getUnchecked("atp.catalogue.url") + "/project/" + projectUuid + "/itf";
        String repl = urls.getUnchecked("configurator.url") + "/project/" + projectId;
        for (Map.Entry<String, String> reportLink : reportLinks.entrySet()) {
            if (StringUtils.isBlank(reportLink.getKey()) || reportLink.getValue() == null) {
                continue;
            }
            reportLinks.put(reportLink.getKey(), reportLink.getValue().replaceFirst(reg, repl));
        }
    }

    /**
     * Compose link to a configuration object or reported object.
     */
    private String composeLinkToObject(BigInteger projectId, UUID projectUuid, Object objectId, String endpoint,
                                       boolean standalone) {
        return standalone
                ? urls.getUnchecked("configurator.url") + "/project/" + projectId + endpoint + objectId
                : urls.getUnchecked("atp.catalogue.url") + "/project/" + projectUuid + "/itf" + endpoint + objectId;
    }

}
