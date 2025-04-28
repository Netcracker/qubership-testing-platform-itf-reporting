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

package org.qubership.automation.itf.ui.service;

import java.time.LocalDateTime;

import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.reports.TcContextObjectManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@Slf4j
public class JobRunner {

    private static final String UTC_TIMEZONE = "UTC";
    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;
    private TcContextObjectManager tcContextObjectManager;

    @Autowired
    public JobRunner(TcContextObjectManager tcContextObjectManager) {
        this.tcContextObjectManager = tcContextObjectManager;
    }

    /**
     * Job that removes irrelevant data from the change history.
     * In fact, script execution is commented now, due to incorrectness of algorithm.
     * The job itself is not removed, for future use.
     */
    @Scheduled(cron = "${atp-reporting.cron.expression}", zone = UTC_TIMEZONE)
    @SchedulerLock(name = "${atp-reporting.job.name}", lockAtMostFor = "5m")
    public void run() {
        log.info("Schedule task start execute at {}", LocalDateTime.now());
        try {
            if (multiTenancyEnabled) {
                for (String tenantId : TenantContext.getTenantIds(true)) {
                    TenantContext.setTenantInfo(tenantId);
                    // Commented, to turn off changing of status here. It's performed on the database side now.
                    int contextCount = 0; // tcContextObjectManager.updateStatusContextWithStatusInProgress();
                    log.info("Schedule task: Updated {} context(s) status for projectUuid {}", contextCount, tenantId);
                }
                TenantContext.setDefaultTenantInfo();
            }
            // Commented, to turn off changing of status here. It's performed on the database side now.
            int contextCount = 0; // tcContextObjectManager.updateStatusContextWithStatusInProgress();
            log.info("Schedule task: Updated {} context(s) status", contextCount);
        } catch (Exception e) {
            log.error("Error while executing a scheduled task {}, message: {}", LocalDateTime.now(), e.getMessage());
        } finally {
            log.info("Schedule task finish execute at {}", LocalDateTime.now());
        }
    }
}
