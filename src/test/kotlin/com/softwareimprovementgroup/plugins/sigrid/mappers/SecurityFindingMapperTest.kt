package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecurityFindingMapperTest {

    private fun makeResponse(
        id: String = "id1",
        severity: String = "HIGH",
        status: String = "RAW",
        filePath: String? = "svc/src/Foo.kt",
        component: String = "svc",
        startLine: Int = 1,
        endLine: Int = 5,
        ruleId: String? = "rule1",
        firstSeenAnalysisDate: String = "",
        remark: String? = "",
    ) = SecurityFindingResponse(
        id = id,
        href = "",
        firstSeenAnalysisDate = firstSeenAnalysisDate,
        lastSeenAnalysisDate = "",
        firstSeenSnapshotDate = "",
        lastSeenSnapshotDate = "",
        filePath = filePath,
        startLine = startLine,
        endLine = endLine,
        component = component,
        type = "SQL_INJECTION",
        cweId = "89",
        severity = severity,
        impact = "high",
        exploitability = "low",
        severityScore = 7f,
        impactScore = 5f,
        exploitabilityScore = 2f,
        status = status,
        remark = remark,
        ruleId = ruleId,
        toolName = null,
        isManualFinding = false,
        isSeverityOverridden = false,
        weaknessIds = emptyList(),
        categories = emptyList(),
    )

    @Test
    fun map_emptyList_returnsEmptyList() {
        assertTrue(SecurityFindingMapper.map(emptyList(), "svc").isEmpty())
    }

    @Test
    fun map_blankSubsystem_returnsAllFindings() {
        val responses = listOf(makeResponse(component = "svcA"), makeResponse(component = "svcB"))
        assertEquals(2, SecurityFindingMapper.map(responses, "").size)
    }

    @Test
    fun map_subsystemFilter_excludesOtherComponents() {
        val responses = listOf(makeResponse(component = "svc"), makeResponse(component = "other"))
        val result = SecurityFindingMapper.map(responses, "svc")
        assertEquals(1, result.size)
        assertEquals("svc", result[0].fileLocations[0].component)
    }

    @Test
    fun map_nullFilePath_producesEmptyFilePath() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(filePath = null)), "")
        assertEquals("", result[0].filePath)
    }

    @Test
    fun map_nullFilePath_producesEmptyDisplayFilePath() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(filePath = null)), "")
        assertEquals("", result[0].displayFilePath)
    }

    @Test
    fun map_filePathWithDirectory_producesDisplayFilePath() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(filePath = "svc/src/main/Foo.kt")), "")
        assertEquals(".../Foo.kt", result[0].displayFilePath)
    }

    @Test
    fun map_filePathNormalizationWithSubsystem() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(filePath = "svc/src/Foo.kt", component = "svc")), "svc")
        assertEquals("src/Foo.kt", result[0].fileLocations[0].filePath)
    }

    @Test
    fun map_filePathNoSubsystemPrefixMatch_unchanged() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(filePath = "other/src/Foo.kt", component = "svc")), "svc")
        assertEquals("other/src/Foo.kt", result[0].fileLocations[0].filePath)
    }

    @Test
    fun map_severityMapped() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(severity = "CRITICAL")), "")
        assertEquals(RiskSeverity.Critical, result[0].severity)
    }

    @Test
    fun map_statusMapped() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(status = "WILL_FIX")), "")
        assertEquals(FindingStatus.WillFix, result[0].status)
    }

    @Test
    fun map_statusLabel_includesIconAndTitleCase() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(status = "WILL_FIX")), "")
        assertEquals("🔧 Will Fix", result[0].statusLabel)
    }

    @Test
    fun map_remark_preservedFromResponse() {
        val response = makeResponse().copy(remark = "needs review")
        val result = SecurityFindingMapper.map(listOf(response), "")
        assertEquals("needs review", result[0].remark)
    }

    @Test
    fun map_startAndEndLinePreservedInFileLocations() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(startLine = 10, endLine = 20)), "")
        assertEquals(10, result[0].fileLocations[0].startLine)
        assertEquals(20, result[0].fileLocations[0].endLine)
    }

    @Test
    fun map_sortedDescendingBySeverityThenAscendingByDisplayFilePath() {
        val responses = listOf(
            makeResponse(id = "A", severity = "HIGH",     filePath = "svc/z/Z.kt", component = "svc"),
            makeResponse(id = "B", severity = "CRITICAL", filePath = "svc/a/A.kt", component = "svc"),
            makeResponse(id = "C", severity = "HIGH",     filePath = "svc/a/A.kt", component = "svc"),
        )
        val result = SecurityFindingMapper.map(responses, "svc")
        assertEquals(listOf("B", "C", "A"), result.map { it.id })
    }

    // region duplicate suppression

    @Test
    fun map_sameLocationAndRuleWithDifferentIds_collapsesToOne() {
        val responses = listOf(makeResponse(id = "A"), makeResponse(id = "B"))
        assertEquals(1, SecurityFindingMapper.map(responses, "").size)
    }

    @Test
    fun map_differentRuleId_notCollapsed() {
        val responses = listOf(makeResponse(id = "A", ruleId = "r1"), makeResponse(id = "B", ruleId = "r2"))
        assertEquals(2, SecurityFindingMapper.map(responses, "").size)
    }

    @Test
    fun map_differentStartLine_notCollapsed() {
        val responses = listOf(makeResponse(id = "A", startLine = 1), makeResponse(id = "B", startLine = 2))
        assertEquals(2, SecurityFindingMapper.map(responses, "").size)
    }

    @Test
    fun map_differentSeverity_notCollapsed() {
        val responses = listOf(makeResponse(id = "A", severity = "HIGH"), makeResponse(id = "B", severity = "CRITICAL"))
        assertEquals(2, SecurityFindingMapper.map(responses, "").size)
    }

    @Test
    fun map_differentFilePath_notCollapsed() {
        val responses = listOf(makeResponse(id = "A", filePath = "svc/A.kt"), makeResponse(id = "B", filePath = "svc/B.kt"))
        assertEquals(2, SecurityFindingMapper.map(responses, "").size)
    }

    @Test
    fun map_nullRuleIdOnBoth_stillCollapsed() {
        val responses = listOf(makeResponse(id = "A", ruleId = null), makeResponse(id = "B", ruleId = null))
        assertEquals(1, SecurityFindingMapper.map(responses, "").size)
    }

    @Test
    fun map_duplicatesWithOneTriaged_keepsTriagedFinding() {
        val responses = listOf(
            makeResponse(id = "raw1", firstSeenAnalysisDate = "2024-01-01"),
            makeResponse(id = "refined", status = "REFINED", remark = "checked", firstSeenAnalysisDate = "2025-01-01"),
            makeResponse(id = "raw2", firstSeenAnalysisDate = "2024-02-01"),
        )
        val result = SecurityFindingMapper.map(responses, "")
        assertEquals(listOf("refined"), result.map { it.id })
        assertEquals("checked", result[0].remark)
    }

    @Test
    fun map_duplicatesWithRemarkOnly_keepsFindingWithRemark() {
        val responses = listOf(makeResponse(id = "A", firstSeenAnalysisDate = "2024-01-01"), makeResponse(id = "B", remark = "note"))
        assertEquals(listOf("B"), SecurityFindingMapper.map(responses, "").map { it.id })
    }

    @Test
    fun map_untriagedDuplicates_keepsEarliestFirstSeen() {
        val responses = listOf(
            makeResponse(id = "late", firstSeenAnalysisDate = "2026-06-11"),
            makeResponse(id = "early", firstSeenAnalysisDate = "2024-08-30"),
        )
        assertEquals(listOf("early"), SecurityFindingMapper.map(responses, "").map { it.id })
    }

    @Test
    fun map_untriagedDuplicatesWithSameDate_keepsSmallestId() {
        val responses = listOf(makeResponse(id = "b"), makeResponse(id = "a"))
        assertEquals(listOf("a"), SecurityFindingMapper.map(responses, "").map { it.id })
    }

    @Test
    fun map_duplicatesInOtherSubsystem_doNotAffectSelectedSubsystem() {
        val responses = listOf(makeResponse(id = "A", component = "svc"), makeResponse(id = "B", component = "other"))
        assertEquals(listOf("A"), SecurityFindingMapper.map(responses, "svc").map { it.id })
    }

    // endregion
}
