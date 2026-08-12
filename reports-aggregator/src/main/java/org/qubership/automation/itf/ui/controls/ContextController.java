/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.ui.services.ContextService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ContextController {

    private final ContextService contextService;

    /**
     * Get context properties by tcContext id.
     *
     * @param contextId - tc context id,
     * @param projectUuid - project Uuid,
     * @return context properties list.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @GetMapping(value = "/context/getProperties", produces = APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get Context Properties of tc-context id {{#contextId}} in the project {{#projectUuid}}")
    public List<Object[]> getContextProperties(
            @RequestParam(value = "contextId") String contextId,
            @RequestParam(required = false) UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), contextId);
        return contextService.getContextProperties(contextId);
    }

    /**
     * Get context variables in json format by tcContext id.
     *
     * @param contextId - tc context id,
     * @param projectUuid - project Uuid,
     * @return context variables in json format.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, 'READ')")
    @GetMapping(value = "/context/getContextVariables", produces = TEXT_PLAIN_VALUE)
    @AuditAction(auditAction = "Get Context Variables of tc-context id {{#contextId}} in the project {{#projectUuid}}")
    public String getContextVariables(
            @RequestParam(value = "contextId") String contextId,
            @RequestParam(required = false) UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), contextId);
        return contextService.getContextVariables(contextId);
    }

    /**
     * Get context keys by tcContext id.
     *
     * @param contextId - tc context id,
     * @param projectUuid - project Uuid,
     * @return context keys set.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/context/getKeys", produces = APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get Context Keys of tc-context id {{#contextId}} in the project {{#projectUuid}}")
    public Set<String> getKeys(
            @RequestParam(value = "contextId") String contextId,
            @RequestParam(required = false) UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), contextId);
        return contextService.getKeys(contextId);
    }

    /**
     * Get tenantId vs. current partition number map.
     *
     * @return map of current partition numbers by tenant id.
     */
    @Transactional(readOnly = true)
    @GetMapping("/partition/current")
    public Map<String, Integer> getCurrentPartitionNumbers() {
        return contextService.getCurrentPartitionNumbers();
    }
}
