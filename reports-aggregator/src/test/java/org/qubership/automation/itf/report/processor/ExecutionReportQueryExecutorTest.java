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

package org.qubership.automation.itf.report.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExecutionReportQueryExecutorTest {

    /**
     * Test of zero character replacement against the following PostgreSQL exception:
     * Caused by: org.postgresql.util.PSQLException:
     * ERROR: invalid byte sequence for encoding "UTF8": 0x00.
     */
    @Test
    public void fixUnicodeZeroByteSequenceTest() {
        Assertions.assertEquals("Test:.",
                ExecutionReportQueryExecutor.fixUnicodeZeroByteSequence("Test:\u0000."));
        Assertions.assertEquals("Test:.",
                ExecutionReportQueryExecutor.fixUnicodeZeroByteSequence("Test:" + (char) 0 + "."));
    }
}
