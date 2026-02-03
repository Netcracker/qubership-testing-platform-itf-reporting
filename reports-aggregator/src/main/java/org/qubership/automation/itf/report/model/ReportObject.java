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

package org.qubership.automation.itf.report.model;

import java.math.BigInteger;

import lombok.Getter;
import lombok.Setter;

/*@SqlResultSetMapping(name="ReportObject",
    entities={ @EntityResult(entityClass=org.qubership.automation.itf.core.report.ReportObject.class) }
)*/

@Setter
@Getter
public class ReportObject {

    private BigInteger id;
    private BigInteger parent;
    private String type;
    private String description;
    private String path;
    private Integer level;
    private String status;
    private String duration;
    private String startTime;
    private String endTime;

    public ReportObject() {
    }

    /**
     * Constructor.
     */
    public ReportObject(BigInteger id, BigInteger parent, String type, String description, String path, Integer level,
            String status, String duration) {
        this.id = id;
        this.parent = parent;
        this.type = type;
        this.description = description;
        this.path = path;
        this.level = level;
        this.status = status;
        this.duration = duration;
        this.startTime = "";
        this.endTime = "";
    }

}
