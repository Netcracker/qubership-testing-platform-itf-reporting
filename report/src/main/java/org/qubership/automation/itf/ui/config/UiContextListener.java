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

package org.qubership.automation.itf.ui.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;

import javax.annotation.PreDestroy;

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.UpgradeHistoryObjectManager;
import org.qubership.automation.itf.core.model.jpa.versions.UpgradeHistory;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.AbstractJmsListeningContainer;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UiContextListener {

    private final ApplicationContext myContext;
    private final LockManager lockManager;
    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;

    /**
     * Context init event handler.
     */
    public void contextInitialized() {
        lockManager.executeWithLock("upgrade_history", this::upgradeHistory);
        startingJmsListenerContainer();
        log.info("Reporting service initialization completed");
    }

    public void contextDestroyed() {
    }

    /**
     * Context init event listener.
     */
    @EventListener
    public void init(ContextRefreshedEvent event) {
        if (event.getSource().equals(myContext)) {
            contextInitialized();
        }
    }

    @PreDestroy
    public void destroyed() {
        //TODO: do we need some destroy actions?
        contextDestroyed();
    }

    private void startingJmsListenerContainer() {
        log.info("Getting JmsListenerContainerFactories...");
        DefaultJmsListenerContainerFactory factory = (DefaultJmsListenerContainerFactory)
                myContext.getBean("reportingDefaultJmsListenerContainerFactory");
        factory.setAutoStartup(true);
        log.info("JmsListenerContainerFactories: setAutoStartup is set to true.");
        log.info("Getting JmsListenerEndpointRegistry...");
        JmsListenerEndpointRegistry jmsListenerEndpointRegistry
                = myContext.getBean(JmsListenerEndpointRegistry.class);
        log.info("Setting 'autoStartup' to true for all JMS Listeners...");
        jmsListenerEndpointRegistry.getListenerContainers().forEach(messageListenerContainer ->
                ((AbstractJmsListeningContainer) messageListenerContainer).setAutoStartup(true)
        );
        log.info("All JMS Listeners are ready. Starting jmsListenerEndpointRegistry...");
        jmsListenerEndpointRegistry.start();
        log.info("JmsListenerEndpointRegistry is started.");
    }

    private void upgradeHistory() {
        if (multiTenancyEnabled) {
            Collection<String> clusters = TenantContext.getTenantIds(true);
            for (String cluster : clusters) {
                TenantContext.setTenantInfo(cluster);
                doUpgradeHistory();
            }
            TenantContext.setDefaultTenantInfo();
        }
        doUpgradeHistory();
    }

    private void doUpgradeHistory() {
        UpgradeHistory upgradeHistories = CoreObjectManager.getInstance()
                .getSpecialManager(UpgradeHistory.class, UpgradeHistoryObjectManager.class).findLastVersion();
        String buildVersionString = getCurrentBuildVersion();
        if (buildVersionString != null) {
            String currentBuildVersion = buildVersionString.replaceFirst("application.version=", "");
            if (Objects.isNull(upgradeHistories) || !currentBuildVersion.contains(upgradeHistories.getName())) {
                UpgradeHistory lastVersion = CoreObjectManager.getInstance().getManager(UpgradeHistory.class).create();
                lastVersion.setUpgradeDatetime(Timestamp.valueOf(LocalDateTime.now()));
                lastVersion.setName(currentBuildVersion);
                lastVersion.store();
            }
        }
    }

    private String getCurrentBuildVersion() {
        String filename = "./buildVersion.properties";
        try {
            byte[] str;
            try (FileInputStream inFile = new FileInputStream(filename)) {
                str = new byte[inFile.available()];
                if (inFile.read(str) < 1) {
                    log.warn("File {} is empty, or EOF was reached suddenly", filename);
                    return null;
                }
            }
            return new String(str, StandardCharsets.UTF_8);
        } catch (IOException | SecurityException | NullPointerException e) {
            log.error("Error while getting current build version", e);
            return null;
        }
    }
}
