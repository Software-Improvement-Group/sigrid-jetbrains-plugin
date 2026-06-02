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
    ) = SecurityFindingResponse(
        id = id,
        href = "",
        firstSeenAnalysisDate = "",
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
        remark = "",
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
    fun map_statusLabel_snakeCaseToTitleCase() {
        val result = SecurityFindingMapper.map(listOf(makeResponse(status = "WILL_FIX")), "")
        assertEquals("Will Fix", result[0].statusLabel)
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
}
