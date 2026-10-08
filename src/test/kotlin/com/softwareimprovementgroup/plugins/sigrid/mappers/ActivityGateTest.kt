package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.FileActivity
import com.softwareimprovementgroup.plugins.sigrid.models.FileActivityData
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityRank
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
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

    private fun isKept(finding: PrioritizedFinding, activity: FileActivityData) = ActivityGate(activity).isActiveOrUnknown(finding)

    @Test
    fun isActiveOrUnknown_nonMaintainabilityCapability_alwaysTrue() {
        val finding = makeFinding(capability = PriorityCapability.Security)
        assertTrue(isKept(finding, makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, 0.0)))))
    }

    @Test
    fun isActiveOrUnknown_noHistoryAtAll_failsOpen() {
        assertTrue(isKept(makeFinding(), makeActivity(hasHistory = false)))
    }

    @Test
    fun isActiveOrUnknown_noFileLocations_failsOpen() {
        assertTrue(isKept(makeFinding(paths = emptyList()), makeActivity()))
    }

    @Test
    fun isActiveOrUnknown_fileNotInActivityData_failsOpen() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 5.0, 0.0)))
        assertTrue(isKept(makeFinding(paths = listOf("svc/Unknown.kt")), activity))
    }

    @Test
    fun isActiveOrUnknown_fileWithPositiveChurn_isTrue() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 5.0, 0.0)))
        assertTrue(isKept(makeFinding(), activity))
    }

    @Test
    fun isActiveOrUnknown_fileWithZeroChurn_isFalse() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, 0.0)))
        assertFalse(isKept(makeFinding(), activity))
    }

    @Test
    fun isActiveOrUnknown_commitsWithoutChurn_isTrue() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", churn = 0.0, commits = 2.0)))
        assertTrue(isKept(makeFinding(), activity))
    }

    @Test
    fun isActiveOrUnknown_multipleLocationsOneActive_isTrue() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/A.kt", 0.0, 0.0), FileActivity("svc/B.kt", 3.0, 0.0)))
        assertTrue(isKept(makeFinding(paths = listOf("svc/A.kt", "svc/B.kt")), activity))
    }

    @Test
    fun isActiveOrUnknown_multipleLocationsAllDormant_isFalse() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/A.kt", 0.0, 0.0), FileActivity("svc/B.kt", 0.0, 0.0)))
        assertFalse(isKept(makeFinding(paths = listOf("svc/A.kt", "svc/B.kt")), activity))
    }

    @Test
    fun isActiveOrUnknown_oneLocationDormantOtherUnknown_isFalse() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/A.kt", 0.0, 0.0)))
        assertFalse(isKept(makeFinding(paths = listOf("svc/A.kt", "svc/Unknown.kt")), activity))
    }

    @Test
    fun filter_removesOnlyDormantMaintainabilityFindings() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, 0.0)))
        val dormant = makeFinding()
        val security = makeFinding(capability = PriorityCapability.Security)

        val kept = ActivityGate(activity).filter(listOf(dormant, security))

        assertEquals(listOf(security), kept)
    }
}
