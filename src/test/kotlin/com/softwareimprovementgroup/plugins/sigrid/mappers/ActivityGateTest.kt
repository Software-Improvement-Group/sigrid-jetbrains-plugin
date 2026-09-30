package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.FileActivity
import com.softwareimprovementgroup.plugins.sigrid.models.FileActivityData
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityRank
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActivityGateTest {

    private fun makeFinding(
        capability: PriorityCapability = PriorityCapability.Maintainability,
        paths: List<String> = listOf("svc/Foo.kt"),
    ) = PrioritizedFinding(
        id = "id",
        capability = capability,
        priorityRank = PriorityRank.High,
        displayLocation = "Foo.kt",
        description = "",
        statusLabel = "",
        remark = "",
        fileLocations = paths.map { FileLocation("svc", it) },
        href = null,
        editable = false,
        statusOptions = emptyList(),
        currentStatusValue = "",
    )

    private fun makeActivity(hasHistory: Boolean = true, fileActivities: List<FileActivity> = emptyList()) =
        FileActivityData(hasHistory = hasHistory, historyStartDate = null, historyEndDate = null, fileActivities = fileActivities)

    @Test
    fun isActiveOrUnknown_nonMaintainabilityCapability_alwaysTrue() {
        val finding = makeFinding(capability = PriorityCapability.Security)
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, emptyMap())))
        assertTrue(ActivityGate.isActiveOrUnknown(finding, activity))
    }

    @Test
    fun isActiveOrUnknown_noHistoryAtAll_failsOpen() {
        assertTrue(ActivityGate.isActiveOrUnknown(makeFinding(), makeActivity(hasHistory = false)))
    }

    @Test
    fun isActiveOrUnknown_noFileLocations_failsOpen() {
        assertTrue(ActivityGate.isActiveOrUnknown(makeFinding(paths = emptyList()), makeActivity()))
    }

    @Test
    fun isActiveOrUnknown_fileNotInActivityData_failsOpen() {
        val finding = makeFinding(paths = listOf("svc/Unknown.kt"))
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 5.0, emptyMap())))
        assertTrue(ActivityGate.isActiveOrUnknown(finding, activity))
    }

    @Test
    fun isActiveOrUnknown_fileWithPositiveChurn_isTrue() {
        val finding = makeFinding(paths = listOf("svc/Foo.kt"))
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 5.0, emptyMap())))
        assertTrue(ActivityGate.isActiveOrUnknown(finding, activity))
    }

    @Test
    fun isActiveOrUnknown_fileWithZeroChurn_isFalse() {
        val finding = makeFinding(paths = listOf("svc/Foo.kt"))
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, emptyMap())))
        assertFalse(ActivityGate.isActiveOrUnknown(finding, activity))
    }

    @Test
    fun isActiveOrUnknown_multipleLocationsOneActive_isTrue() {
        val finding = makeFinding(paths = listOf("svc/A.kt", "svc/B.kt"))
        val activity = makeActivity(
            fileActivities = listOf(
                FileActivity("svc/A.kt", 0.0, emptyMap()),
                FileActivity("svc/B.kt", 3.0, emptyMap()),
            ),
        )
        assertTrue(ActivityGate.isActiveOrUnknown(finding, activity))
    }
}
