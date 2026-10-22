package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.ChangedFiles
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

    private fun isKept(finding: PrioritizedFinding, activity: FileActivityData) = ActivityGate(activity).apply(listOf(finding)).single() == finding

    @Test
    fun apply_nonMaintainabilityCapability_neverDemoted() {
        val finding = makeFinding(capability = PriorityCapability.Security)
        assertTrue(isKept(finding, makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, 0.0)))))
    }

    @Test
    fun apply_noHistoryAtAll_failsOpen() {
        assertTrue(isKept(makeFinding(), makeActivity(hasHistory = false)))
    }

    @Test
    fun apply_noFileLocations_failsOpen() {
        assertTrue(isKept(makeFinding(paths = emptyList()), makeActivity()))
    }

    @Test
    fun apply_fileNotInActivityData_failsOpen() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 5.0, 0.0)))
        assertTrue(isKept(makeFinding(paths = listOf("svc/Unknown.kt")), activity))
    }

    @Test
    fun apply_fileWithPositiveChurn_notDemoted() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 5.0, 0.0)))
        assertTrue(isKept(makeFinding(), activity))
    }

    @Test
    fun apply_fileWithZeroChurn_demoted() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, 0.0)))
        assertFalse(isKept(makeFinding(), activity))
    }

    @Test
    fun apply_commitsWithoutChurn_notDemoted() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", churn = 0.0, commits = 2.0)))
        assertTrue(isKept(makeFinding(), activity))
    }

    @Test
    fun apply_multipleLocationsOneActive_notDemoted() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/A.kt", 0.0, 0.0), FileActivity("svc/B.kt", 3.0, 0.0)))
        assertTrue(isKept(makeFinding(paths = listOf("svc/A.kt", "svc/B.kt")), activity))
    }

    @Test
    fun apply_multipleLocationsAllDormant_demoted() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/A.kt", 0.0, 0.0), FileActivity("svc/B.kt", 0.0, 0.0)))
        assertFalse(isKept(makeFinding(paths = listOf("svc/A.kt", "svc/B.kt")), activity))
    }

    @Test
    fun apply_oneLocationDormantOtherUnknown_demoted() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/A.kt", 0.0, 0.0)))
        assertFalse(isKept(makeFinding(paths = listOf("svc/A.kt", "svc/Unknown.kt")), activity))
    }

    @Test
    fun apply_demotesOnlyDormantMaintainabilityFindings() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, 0.0)))
        val dormant = makeFinding()
        val security = makeFinding(capability = PriorityCapability.Security)

        val result = ActivityGate(activity).apply(listOf(dormant, security))

        assertEquals(listOf(PriorityRank.Medium, PriorityRank.High), result.map { it.priorityRank })
        assertEquals(security, result[1])
    }

    @Test
    fun apply_dormantFile_dropsOneTierKeepsFindingAndExplainsWhy() {
        val activity = FileActivityData(true, "2026-09-01", "2026-10-12", listOf(FileActivity("svc/Foo.kt", 0.0, 0.0)))

        val result = ActivityGate(activity).apply(listOf(makeFinding())).single()

        assertEquals(PriorityRank.Medium, result.priorityRank)
        assertEquals(1, result.promotionReason.size)
        assertTrue(result.promotionReason.single().contains("2026-09-01"))
    }

    @Test
    fun apply_dormantFileChangedOnBranch_notDemoted() {
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, 0.0)))
        val changed = ChangedFiles(setOf("repo/svc/Foo.kt"))
        val finding = makeFinding()

        assertEquals(listOf(finding), ActivityGate(activity, changed).apply(listOf(finding)))
    }

    @Test
    fun apply_fileUnknownToExportButChangedOnBranch_notDemoted() {
        val changed = ChangedFiles(setOf("repo/svc/Unknown.kt"))
        val finding = makeFinding(paths = listOf("svc/Unknown.kt"))

        assertEquals(listOf(finding), ActivityGate(makeActivity(), changed).apply(listOf(finding)))
    }

    private val dormantInExport = makeActivity(fileActivities = listOf(FileActivity("svc/Foo.kt", 0.0, 0.0)))

    @Test
    fun apply_dormantInExportButTouchedInLocalHistory_notDemoted() {
        val history = ChangedFiles(setOf("repo/svc/Foo.kt"))
        val finding = makeFinding()

        assertEquals(listOf(finding), ActivityGate(dormantInExport, localHistory = history).apply(listOf(finding)))
    }

    @Test
    fun apply_dormantInExportAndLocalHistory_demotedWithYearLongReason() {
        val history = ChangedFiles(setOf("repo/svc/Other.kt"))

        val result = ActivityGate(dormantInExport, localHistory = history).apply(listOf(makeFinding())).single()

        assertEquals(PriorityRank.Medium, result.priorityRank)
        assertTrue(result.promotionReason.single().contains("12 months"))
    }

    @Test
    fun apply_localHistoryUnavailable_fallsBackToExportWindow() {
        val result = ActivityGate(dormantInExport, localHistory = null).apply(listOf(makeFinding())).single()

        assertEquals(PriorityRank.Medium, result.priorityRank)
        assertFalse(result.promotionReason.single().contains("12 months"))
    }
}
