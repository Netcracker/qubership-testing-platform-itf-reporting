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

package org.qubership.automation.itf.ui.messages.objects;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UIReportItem {

    private String id;
    private String name;
    private String initiator;
    private String initiatorId;
    private String initiatorType;
    private String system;
    private String operation;
    private String status;
    private String environment;
    private String startTime;
    private String endTime;
    private String duration;
    private String client;
    private Map<String, String> reportLinks;
    private String[] bindingKeys;
    private String contextVariable;
    private String callchainExecutionData;
    private Map<String, String> reportSituations;
    private Integer partNum;

    public UIReportItem() {
    }

    public void setReportLinks(Map<String, String> reportLinks) {
        this.reportLinks = Maps.newHashMap(reportLinks);
    }

    public void setBindingKeys(Set<String> bindingKeys) {
        this.bindingKeys = bindingKeys.toArray(new String[0]);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Returned value is used for representation only")
    public String[] getBindingKeys() {
        return this.bindingKeys;
    }

    public void setReportSituations(Map<String, String> reportSituations) {
        this.reportSituations = Maps.newHashMap(reportSituations);
    }
}
