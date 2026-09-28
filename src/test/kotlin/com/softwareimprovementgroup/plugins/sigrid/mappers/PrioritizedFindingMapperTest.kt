package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrioritizedFindingMapperTest {

    private fun makeCandidate(
        id: String = "m1",
        severity: MaintainabilitySeverity = MaintainabilitySeverity.High,
        status: MaintainabilityFindingStatus = MaintainabilityFindingStatus.Raw,
        displayLocation: String = "Foo.kt",
    ) = RefactoringCandidate(
        id = id,
        category = RefactoringCategory.UnitSize,
        severity = severity,
        status = status,
        statusLabel = "",
        weight = 100,
        technology = "Kotlin",
        snapshotDate = "",
        name = "foo",
        mcCabe = null,
        fanIn = null,
        component = "svc",
        parameters = null,
        displayLocation = displayLocation,
        description = "Foo.kt is too long.",
        remark = "",
        fileLocations = emptyList(),
        href = null,
    )

    private fun makeSecurityFinding(
        id: String = "s1",
        severity: RiskSeverity = RiskSeverity.High,
        status: FindingStatus = FindingStatus.Raw,
        displayFilePath: String = "Bar.kt",
    ) = SecurityFinding(
        id = id,
        href = "",
        severity = severity,
        filePath = displayFilePath,
        displayFilePath = displayFilePath,
        type = "SQL_INJECTION",
        status = status,
        statusLabel = "",
        remark = "",
        fileLocations = emptyList(),
    )

    private fun makeOshDependency(
        name: String = "left-pad",
        risk: RiskSeverity = RiskSeverity.Critical,
        purl: String? = null,
    ) = OpenSourceHealthDependency(
        name = name,
        displayName = name,
        version = "1.0.0",
        group = null,
        dependencyType = DependencyType.DIRECT,
        purl = purl,
        risk = risk,
        licenseRisk = RiskSeverity.None,
        vulnerabilityRisk = risk,
        freshnessRisk = RiskSeverity.None,
        activityRisk = RiskSeverity.None,
        stabilityRisk = RiskSeverity.None,
        managementRisk = RiskSeverity.None,
        fileLocations = emptyList(),
        href = null,
    )

    @Test
    fun map_emptyInputs_returnsEmptyList() {
        assertTrue(PrioritizedFindingMapper.map(emptyList(), emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun map_maintainability_onlyRawStatusIncluded() {
        val candidates = listOf(
            makeCandidate(id = "raw", status = MaintainabilityFindingStatus.Raw),
            makeCandidate(id = "willFix", status = MaintainabilityFindingStatus.WillFix),
            makeCandidate(id = "accepted", status = MaintainabilityFindingStatus.Accepted),
        )
        val result = PrioritizedFindingMapper.map(candidates, emptyList(), emptyList())
        assertEquals(listOf("raw"), result.map { it.id })
    }

    @Test
    fun map_security_onlyRawStatusIncluded() {
        val findings = listOf(
            makeSecurityFinding(id = "raw", status = FindingStatus.Raw),
            makeSecurityFinding(id = "fixed", status = FindingStatus.Fixed),
            makeSecurityFinding(id = "falsePositive", status = FindingStatus.FalsePositive),
        )
        val result = PrioritizedFindingMapper.map(emptyList(), findings, emptyList())
        assertEquals(listOf("raw"), result.map { it.id })
    }

    @Test
    fun map_openSourceHealth_includedRegardlessOfStatus() {
        val result = PrioritizedFindingMapper.map(emptyList(), emptyList(), listOf(makeOshDependency()))
        assertEquals(1, result.size)
        assertEquals(PriorityCapability.OpenSourceHealth, result[0].capability)
    }

    @Test
    fun map_capabilityTaggedCorrectlyPerSource() {
        val result = PrioritizedFindingMapper.map(listOf(makeCandidate()), listOf(makeSecurityFinding()), listOf(makeOshDependency()))
        val byCapability = result.associateBy { it.capability }
        assertEquals(PriorityCapability.Maintainability, byCapability[PriorityCapability.Maintainability]?.capability)
        assertEquals(PriorityCapability.Security, byCapability[PriorityCapability.Security]?.capability)
        assertEquals(PriorityCapability.OpenSourceHealth, byCapability[PriorityCapability.OpenSourceHealth]?.capability)
    }

    @Test
    fun map_maintainabilitySeverityRankedProvisionally() {
        val candidates = listOf(
            makeCandidate(id = "unknown", severity = MaintainabilitySeverity.Unknown),
            makeCandidate(id = "low", severity = MaintainabilitySeverity.Low),
            makeCandidate(id = "medium", severity = MaintainabilitySeverity.Medium),
            makeCandidate(id = "moderate", severity = MaintainabilitySeverity.Moderate),
            makeCandidate(id = "high", severity = MaintainabilitySeverity.High),
            makeCandidate(id = "veryHigh", severity = MaintainabilitySeverity.VeryHigh),
        )
        val result = PrioritizedFindingMapper.map(candidates, emptyList(), emptyList()).associateBy { it.id }
        assertEquals(PriorityRank.Unknown, result["unknown"]?.priorityRank)
        assertEquals(PriorityRank.Low, result["low"]?.priorityRank)
        assertEquals(PriorityRank.Medium, result["medium"]?.priorityRank)
        assertEquals(PriorityRank.Medium, result["moderate"]?.priorityRank)
        assertEquals(PriorityRank.High, result["high"]?.priorityRank)
        assertEquals(PriorityRank.Critical, result["veryHigh"]?.priorityRank)
    }

    @Test
    fun map_riskSeverityRankedProvisionally() {
        val findings = listOf(
            makeSecurityFinding(id = "none", severity = RiskSeverity.None),
            makeSecurityFinding(id = "unknown", severity = RiskSeverity.Unknown),
            makeSecurityFinding(id = "information", severity = RiskSeverity.Information),
            makeSecurityFinding(id = "low", severity = RiskSeverity.Low),
            makeSecurityFinding(id = "medium", severity = RiskSeverity.Medium),
            makeSecurityFinding(id = "high", severity = RiskSeverity.High),
            makeSecurityFinding(id = "critical", severity = RiskSeverity.Critical),
        )
        val result = PrioritizedFindingMapper.map(emptyList(), findings, emptyList()).associateBy { it.id }
        assertEquals(PriorityRank.Unknown, result["none"]?.priorityRank)
        assertEquals(PriorityRank.Unknown, result["unknown"]?.priorityRank)
        assertEquals(PriorityRank.Low, result["information"]?.priorityRank)
        assertEquals(PriorityRank.Low, result["low"]?.priorityRank)
        assertEquals(PriorityRank.Medium, result["medium"]?.priorityRank)
        assertEquals(PriorityRank.High, result["high"]?.priorityRank)
        assertEquals(PriorityRank.Critical, result["critical"]?.priorityRank)
    }

    @Test
    fun map_sortedDescendingByRankThenAscendingByDisplayLocation() {
        val candidates = listOf(makeCandidate(id = "M-high", severity = MaintainabilitySeverity.High, displayLocation = "z.kt"))
        val findings = listOf(makeSecurityFinding(id = "S-critical", severity = RiskSeverity.Critical, displayFilePath = "a.kt"))
        val osh = listOf(makeOshDependency(name = "b-dep", risk = RiskSeverity.Critical))

        val result = PrioritizedFindingMapper.map(candidates, findings, osh)

        assertEquals(listOf("S-critical", "b-dep", "M-high"), result.map { it.id })
    }
}
