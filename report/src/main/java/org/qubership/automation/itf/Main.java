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

package org.qubership.automation.itf;

import org.javers.spring.boot.sql.JaversSqlAutoConfiguration;
import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.annotation.EnableM2MRestTemplate;
import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.annotation.EnableOauth2FeignClientInterceptor;
import org.qubership.atp.common.lock.annotation.EnableAtpLockManager;
import org.qubership.atp.common.probes.controllers.DeploymentController;
import org.qubership.atp.multitenancy.hibernate.annotation.EnableMultiTenantDataSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                MongoAutoConfiguration.class,
                LiquibaseAutoConfiguration.class,
                JaversSqlAutoConfiguration.class
        })
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX,
                pattern = "org.qubership.automation.itf.core.config.ExecutorHibernateConfiguration"),
        @ComponentScan.Filter(type = FilterType.REGEX,
                pattern = "org.qubership.automation.itf.core.hibernate.spring.managers.executor"
                        + ".(?!(UpgradeHistoryObjectManager)).*")
})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableDiscoveryClient
@EnableJpaRepositories(includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "org.qubership.automation.itf.core.hibernate.spring.repositories.reports.*"),
                @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "org.qubership.automation.itf.core.hibernate.spring.repositories.executor"
                        + ".UpgradeHistoryRepository")}
)
@EnableTransactionManagement
//@EnableConfigurationProperties
@EnableM2MRestTemplate
@EnableOauth2FeignClientInterceptor
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableFeignClients(basePackages = {"org.qubership.atp.integration.configuration.feign"})
@Import({
        WebMvcAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        ServletWebServerFactoryAutoConfiguration.class,
        DeploymentController.class
})
@EnableMultiTenantDataSource
@EnableAtpLockManager
public class Main {

    /**
     * Main.
     *
     * @param args as usual
     */
    public static void main(String[] args) {
        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(Main.class);
        springApplicationBuilder
                .build()
                .addListeners(new ApplicationPidFileWriter("application.pid"));
        springApplicationBuilder.run(args);
    }
}
