package org.qubership.automation.itf.report.processor;

import org.junit.Assert;
import org.junit.Test;

public class ExecutionReportQueryExecutorTest {

    /**
     * Test of zero character replacement against the following PostgreSQL exception:
     * Caused by: org.postgresql.util.PSQLException:
     * ERROR: invalid byte sequence for encoding "UTF8": 0x00.
     */
    @Test
    public void fixUnicodeZeroByteSequenceTest() {
        Assert.assertEquals("Test:.",
                ExecutionReportQueryExecutor.fixUnicodeZeroByteSequence("Test:\u0000."));
        Assert.assertEquals("Test:.",
                ExecutionReportQueryExecutor.fixUnicodeZeroByteSequence("Test:" + (char) 0 + "."));
    }
}
