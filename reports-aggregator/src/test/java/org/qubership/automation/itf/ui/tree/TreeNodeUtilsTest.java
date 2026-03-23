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

package org.qubership.automation.itf.ui.tree;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.automation.itf.report.model.ReportObject;

import com.google.common.collect.Lists;

public class TreeNodeUtilsTest extends TreeNodeUtils {

    private static final List<BigInteger> listIds = List.of(
            new BigInteger("0"),
            new BigInteger("60420003267"),
            new BigInteger("60420003271"),
            new BigInteger("60420003272"),
            new BigInteger("60420003273"),
            new BigInteger("60420003274"),
            new BigInteger("60420003275"),
            new BigInteger("62420003271"),
            new BigInteger("62420003273"),
            new BigInteger("62420003274"),
            new BigInteger("62420003275")
    );

    @Test
    public void getReportObjectTest() {
        Object[] objects = new Object[10];
        objects[6] = "TEST";
        ReportObject reportObject = getReportObject(objects);
        Assertions.assertEquals("TEST", reportObject.getStatus());
    }

    @Test
    public void getReportObjectExceptionTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            Object[] objects = new Object[7];
            getReportObject(objects);
        });
    }

    @Test
    public void cutExcessTest() {
        List<ReportObject> objects = Lists.newArrayList();
        objects.add(new ReportObject(listIds.get(1), listIds.get(0),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(2), listIds.get(1),
                "SituationInstance", "send", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(3), listIds.get(2),
                "InstanceContext", "send", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(4), listIds.get(2),
                "StepInstance", "send", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(5), listIds.get(4),
                "InstanceContext", "send", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(6), listIds.get(5),
                "SpContext", "send", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(new BigInteger("1133"), listIds.get(6),
                "incoming message", "send", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(new BigInteger("1134"), listIds.get(6),
                "outgoing message", "send", "", 2, "PASSED", "01.069"));

        objects = cutExcess(objects,
                TreeNodeTypes.SITUATION_INSTANCE.toString(),
                TreeNodeTypes.STEP_INSTANCE.toString());
        Assertions.assertEquals(2, objects.size());
    }

    @Test
    public void setCorrectMethod() {
        List<ReportObject> objects = Lists.newArrayList();
        objects.add(new ReportObject(listIds.get(1), listIds.get(0),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069"));
        ReportObject reportObject1 = new ReportObject(listIds.get(2), listIds.get(1),
                "SituationInstance", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject1);
        ReportObject reportObject2 = new ReportObject(listIds.get(3), listIds.get(2),
                "InstanceContext", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject2);
        setCorrectParent(objects, reportObject1);
        Assertions.assertEquals(listIds.get(1), reportObject2.getParent());
    }

    @Test
    public void moveToSecondIfHasStatusTest() {
        List<ReportObject> objects = Lists.newArrayList();
        List<ReportObject> savedObjects = Lists.newArrayList();
        objects.add(new ReportObject(listIds.get(1), listIds.get(0),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(2), listIds.get(1),
                "SituationInstance", "send", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(3), listIds.get(2),
                "InstanceContext", "send", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(4), listIds.get(3),
                "StepInstance", "send", "", 2, "PASSED", "01.069"));
        moveToSavedCollectionIfHasType(objects, savedObjects, Lists.newArrayList(TreeNodeTypes.STEP_INSTANCE.toString()));
        Assertions.assertEquals(1, savedObjects.size());
    }

    @Test
    public void createTreeNodesByReportTest() {
        List<ReportObject> objects = Lists.newArrayList();
        objects.add(new ReportObject(listIds.get(1), listIds.get(0),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(2), listIds.get(1),
                "SituationInstance", "send1", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(3), listIds.get(2),
                "InstanceContext", "send2", "", 3, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(4), listIds.get(2),
                "StepInstance", "send3", "", 3, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(5), listIds.get(4),
                "InstanceContext", "send4", "", 4, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(6), listIds.get(5),
                "SpContext", "send5", "", 5, "PASSED", "01.069"));
        objects.add(new ReportObject(new BigInteger("1133"), listIds.get(6),
                "incoming message", "send6", "", 6, "PASSED", "01.069"));
        objects.add(new ReportObject(new BigInteger("1134"), listIds.get(6),
                "outgoing message", "send7", "", 6, "PASSED", "01.069"));
        List<TreeNode> treeNodesByReport = TreeNodeUtils.createTreeNodesByReport(objects,
                TreeNodeTypes.SITUATION_INSTANCE.toString(),
                TreeNodeTypes.STEP_INSTANCE.toString(),
                TreeNodeTypes.OUTGOING_MESSAGE.toString());

        Assertions.assertEquals(1, treeNodesByReport.size());
    }

    @Test
    public void createTreeNodesByReportTest2() {
        List<ReportObject> objects = Lists.newArrayList();
        objects.add(new ReportObject(listIds.get(1), listIds.get(0),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(2), listIds.get(1),
                "SituationInstance", "send1", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(3), listIds.get(2),
                "InstanceContext", "send2", "", 3, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(4), listIds.get(2),
                "StepInstance", "send3", "", 3, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(5), listIds.get(4),
                "InstanceContext", "send4", "", 4, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(6), listIds.get(5),
                "SpContext", "send5", "", 5, "PASSED", "01.069"));
        objects.add(new ReportObject(new BigInteger("1133"), listIds.get(6),
                "incoming message", "send6", "", 6, "PASSED", "01.069"));
        objects.add(new ReportObject(new BigInteger("1134"), listIds.get(6),
                "outgoing message", "send7", "", 6, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(7), listIds.get(1),
                "SituationInstance", "send1", "", 2, "PASSED", "01.069"));
        objects.add(new ReportObject(new BigInteger("62420003272"), listIds.get(7),
                "InstanceContext", "send2", "", 3, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(8), listIds.get(7),
                "StepInstance", "send3", "", 3, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(9), listIds.get(8),
                "InstanceContext", "send4", "", 4, "PASSED", "01.069"));
        objects.add(new ReportObject(listIds.get(10), listIds.get(9),
                "SpContext", "send5", "", 5, "PASSED", "01.069"));
        objects.add(new ReportObject(new BigInteger("51133"), listIds.get(10),
                "incoming message", "send6", "", 6, "PASSED", "01.069"));
        objects.add(new ReportObject(new BigInteger("51134"), listIds.get(10),
                "outgoing message", "send7", "", 6, "PASSED", "01.069"));
        List<TreeNode> treeNodesByReport = TreeNodeUtils.createTreeNodesByReport(objects,
                TreeNodeTypes.SITUATION_INSTANCE.toString(),
                TreeNodeTypes.STEP_INSTANCE.toString(),
                TreeNodeTypes.OUTGOING_MESSAGE.toString(),
                TreeNodeTypes.INCOMING_MESSAGE.toString());

        Assertions.assertEquals(2, treeNodesByReport.size());
    }

}
