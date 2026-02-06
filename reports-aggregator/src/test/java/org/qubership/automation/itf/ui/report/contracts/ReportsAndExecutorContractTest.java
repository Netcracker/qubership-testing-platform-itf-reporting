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

package org.qubership.automation.itf.ui.report.contracts;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.qubership.automation.itf.ui.controls.ContextController;
import org.qubership.automation.itf.ui.services.ContextService;
import lombok.extern.slf4j.Slf4j;

@Provider("atp-itf-reports")
@PactUrl(urls = {"classpath:pacts/atp-itf-executor-atp-itf-reports.json"})
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = {ContextController.class})
@ContextConfiguration(classes = {ReportsAndExecutorContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        ContextController.class})
@TestPropertySource(locations = "classpath:bootstrap-test.properties")
@Slf4j
public class ReportsAndExecutorContractTest {

    @Configuration
    public static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContextService contextService;

    public void beforeAll() {
        log.info("ReportsAndExecutorContractTest tests started");
        String contextId = "9167234930111872000";

        when(contextService.getContextVariables(contextId)).thenReturn(getResponseContextVariablesBody());
        when(contextService.getKeys(contextId)).thenReturn(getResponseKeysBody());
        when(contextService.getContextProperties(contextId)).thenReturn(getResponsePropertiesBody());
        when(contextService.getCurrentPartitionNumbers()).thenReturn(getCurrentPartitionsBody());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        beforeAll();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("all ok")
    public void allPass() {
    }

    private String getResponseContextVariablesBody() {
        return "testVariable";
    }

    private List<Object[]> getResponsePropertiesBody() {
        List<Object[]> objectList = new ArrayList<>();
        Object[] objects = new Object[1];
        objects[0] = "testProperty";
        objectList.add(objects);
        return objectList;
    }

    private Set<String> getResponseKeysBody() {
        Set<String> strings = new HashSet<>();
        strings.add("testKey");
        return strings;
    }

    private Map<String, Integer> getCurrentPartitionsBody() {
        Map<String, Integer> map = new HashMap<>();
        map.put("Default", 1);
        //map.put(UUID.randomUUID().toString(), 2);
        map.put("39cae351-9e3b-4fb6-a384-1c3616f4e76f", 2);
        return map;
    }

}
