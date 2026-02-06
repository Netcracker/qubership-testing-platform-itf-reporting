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

package org.qubership.automation.itf.ui.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.MonitoringManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.reports.TcContextBriefInfoObjectManager;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContextService {
    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;

    public static final ConcurrentHashMap<String, Integer> currentPartitionNumbers = initPartitionNumbers();
    private final ScheduledExecutorService refreshPartitionsService = initRefreshPartitionsService();

    private ScheduledExecutorService initRefreshPartitionsService() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(() -> {
            String title = "Refresh of current partition numbers from reporting database(s)";
            try {
                log.info("{} is started.", title);
                refreshPartitionNumbers(getCurrentPartitionNumbers());
                log.info("{} is completed.", title);
            } catch (Throwable t) {
                log.error("{}: failed with exception(s)", title, t);
            }
        }, 20, 3600, TimeUnit.SECONDS);
        return service;
    }

    private static ConcurrentHashMap<String, Integer> initPartitionNumbers() {
        ConcurrentHashMap<String, Integer> currentPartitionNumbers = new ConcurrentHashMap<>();
        currentPartitionNumbers.put("Default", 1);
        return currentPartitionNumbers;
    }

    public static int getCurrentPartitionNumberByProject(UUID projectUuid) {
        Integer i = currentPartitionNumbers.get(projectUuid.toString());
        return (i == null) ? currentPartitionNumbers.get("Default") : i;
    }

    public static int getCurrentPartitionNumberByProject(String projectUuid) {
        Integer i = currentPartitionNumbers.get(projectUuid);
        return (i == null) ? currentPartitionNumbers.get("Default") : i;
    }

    public void refreshPartitionNumbers(Map<String, Integer> newData) {
        currentPartitionNumbers.putAll(newData);
    }

    public List<Object[]> getContextProperties(String contextId) {
        return CoreObjectManager.getInstance().getSpecialManager(InstanceContext.class, MonitoringManager.class)
                .getTcContextInfo(contextId);
    }

    public String getContextVariables(String contextId) {
        return CoreObjectManager.getInstance().getSpecialManager(InstanceContext.class, MonitoringManager.class)
                .getContextVariables(contextId);
    }

    public Set<String> getKeys(String contextId) {
        return CoreObjectManager.getInstance().getSpecialManager(InstanceContext.class, MonitoringManager.class)
                .getTcContextBindingKeys(contextId);
    }

    /**
     * Get tenantId vs. current partition number map.
     *
     * @return map of current partition numbers by tenant id.
     */
    public Map<String, Integer> getCurrentPartitionNumbers() {
        Map<String, Integer> currentPartitions = new HashMap<>();
        try {
            if (multiTenancyEnabled) {
                for (String tenantId : TenantContext.getTenantIds(false)) {
                    TenantContext.setTenantInfo(tenantId);
                    currentPartitions.put(tenantId, TcContextBriefInfoObjectManager.getCurrentPartitionNumber());
                }
                TenantContext.setDefaultTenantInfo();
            }
            currentPartitions.put("Default", TcContextBriefInfoObjectManager.getCurrentPartitionNumber());
        } catch (Exception e) {
            log.error("Error while current partition number getting", e);
        } finally {
            TenantContext.setDefaultTenantInfo();
        }
        return currentPartitions;
    }
}
