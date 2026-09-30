package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrioritizedFindingMapperTest {

    private fun makeCandidate(
        id: String = "m1",
        category: RefactoringCategory = RefactoringCategory.UnitSize,
        severity: MaintainabilitySeverity = MaintainabilitySeverity.High,
        status: MaintainabilityFindingStatus = MaintainabilityFindingStatus.Raw,
        displayLocation: String = "Foo.kt",
        weight: Int = 100,
        fileLocations: List<FileLocation> = emptyList(),
    ) = RefactoringCandidate(
        id = id,
        category = category,
        severity = severity,
        status = status,
        statusLabel = "",
        weight = weight,
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
        fileLocations = fileLocations,
        href = null,
    )

    private fun makeSecurityFinding(
        id: String = "s1",
        severity: RiskSeverity = RiskSeverity.High,
        status: FindingStatus = FindingStatus.Raw,
        displayFilePath: String = "Bar.kt",
        fileLocations: List<FileLocation> = emptyList(),
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
        fileLocations = fileLocations,
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

    private fun map(
        maintainability: List<RefactoringCandidate> = emptyList(),
        security: List<SecurityFinding> = emptyList(),
        reliability: List<SecurityFinding> = emptyList(),
        openSourceHealth: List<OpenSourceHealthDependency> = emptyList(),
    ) = PrioritizedFindingMapper.map(maintainability, security, reliability, openSourceHealth)

    @Test
    fun map_emptyInputs_returnsEmptyList() {
        assertTrue(map().findings.isEmpty())
    }

    @Test
    fun map_maintainability_onlyRawStatusIncluded() {
        val candidates = listOf(
            makeCandidate(id = "raw", status = MaintainabilityFindingStatus.Raw),
            makeCandidate(id = "willFix", status = MaintainabilityFindingStatus.WillFix),
            makeCandidate(id = "accepted", status = MaintainabilityFindingStatus.Accepted),
        )
        val result = map(maintainability = candidates)
        assertEquals(listOf("raw"), result.findings.map { it.id })
    }

    @Test
    fun map_security_onlyRawStatusIncluded() {
        val findings = listOf(
            makeSecurityFinding(id = "raw", status = FindingStatus.Raw),
            makeSecurityFinding(id = "fixed", status = FindingStatus.Fixed),
            makeSecurityFinding(id = "falsePositive", status = FindingStatus.FalsePositive),
        )
        val result = map(security = findings)
        assertEquals(listOf("raw"), result.findings.map { it.id })
    }

    @Test
    fun map_reliability_onlyRawStatusIncluded() {
        val findings = listOf(
            makeSecurityFinding(id = "raw", status = FindingStatus.Raw),
            makeSecurityFinding(id = "fixed", status = FindingStatus.Fixed),
            makeSecurityFinding(id = "falsePositive", status = FindingStatus.FalsePositive),
        )
        val result = map(reliability = findings)
        assertEquals(listOf("raw"), result.findings.map { it.id })
    }

    @Test
    fun map_reliability_taggedAsReliabilityNotSecurity() {
        val result = map(reliability = listOf(makeSecurityFinding(id = "r1")))
        assertEquals(PriorityCapability.Reliability, result.findings[0].capability)
    }

    @Test
    fun map_openSourceHealth_includedRegardlessOfStatus() {
        val result = map(openSourceHealth = listOf(makeOshDependency()))
        assertEquals(1, result.findings.size)
        assertEquals(PriorityCapability.OpenSourceHealth, result.findings[0].capability)
    }

    @Test
    fun map_capabilityTaggedCorrectlyPerSource() {
        val result = map(
            maintainability = listOf(makeCandidate()),
            security = listOf(makeSecurityFinding()),
            reliability = listOf(makeSecurityFinding(id = "r1")),
            openSourceHealth = listOf(makeOshDependency()),
        )
        val byCapability = result.findings.associateBy { it.capability }
        assertEquals(PriorityCapability.Maintainability, byCapability[PriorityCapability.Maintainability]?.capability)
        assertEquals(PriorityCapability.Security, byCapability[PriorityCapability.Security]?.capability)
        assertEquals(PriorityCapability.Reliability, byCapability[PriorityCapability.Reliability]?.capability)
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
        val result = map(maintainability = candidates).findings.associateBy { it.id }
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
        val result = map(security = findings).findings.associateBy { it.id }
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

        val result = map(maintainability = candidates, security = findings, openSourceHealth = osh)

        assertEquals(listOf("S-critical", "b-dep", "M-high"), result.findings.map { it.id })
    }

    // exclusion: vendored/generated/config

    @Test
    fun map_vendoredPath_excludedFromFindings() {
        val candidates = listOf(makeCandidate(id = "vendored", fileLocations = listOf(FileLocation("svc", "svc/node_modules/lib/index.js"))))
        val result = map(maintainability = candidates)
        assertTrue(result.findings.isEmpty())
    }

    @Test
    fun map_configFile_excludedForMaintainabilityOnly() {
        val candidates = listOf(makeCandidate(id = "cfg", fileLocations = listOf(FileLocation("svc", "svc/application.yml"))))
        val findings = listOf(makeSecurityFinding(id = "cfg-sec", fileLocations = listOf(FileLocation("svc", "svc/application.yml"))))
        val result = map(maintainability = candidates, security = findings)
        assertEquals(listOf("cfg-sec"), result.findings.map { it.id })
    }

    @Test
    fun map_normalApplicationFile_notExcluded() {
        val candidates = listOf(makeCandidate(id = "normal", fileLocations = listOf(FileLocation("svc", "svc/src/main/Foo.kt"))))
        val result = map(maintainability = candidates)
        assertEquals(listOf("normal"), result.findings.map { it.id })
    }

    // test-code lane

    @Test
    fun map_testCodePath_excludedFromFindingsButKeptInTestCodeFindings() {
        val candidates = listOf(makeCandidate(id = "test", fileLocations = listOf(FileLocation("svc", "svc/src/test/java/FooTest.java"))))
        val result = map(maintainability = candidates)
        assertTrue(result.findings.isEmpty())
        assertEquals(listOf("test"), result.testCodeFindings.map { it.id })
    }

    @Test
    fun map_mixOfTestAndMainCode_partitionedCorrectly() {
        val candidates = listOf(
            makeCandidate(id = "main", fileLocations = listOf(FileLocation("svc", "svc/src/main/Foo.kt"))),
            makeCandidate(id = "test", fileLocations = listOf(FileLocation("svc", "svc/src/test/FooTest.kt"))),
        )
        val result = map(maintainability = candidates)
        assertEquals(listOf("main"), result.findings.map { it.id })
        assertEquals(listOf("test"), result.testCodeFindings.map { it.id })
    }

    @Test
    fun map_noFileLocations_notExcludedAndNotTestCode() {
        val result = map(openSourceHealth = listOf(makeOshDependency()))
        assertEquals(1, result.findings.size)
        assertTrue(result.testCodeFindings.isEmpty())
    }

    // dedup: OSH library-name collapsing

    @Test
    fun map_openSourceHealth_sameLibraryDifferentPurl_collapsedToOneFinding() {
        val deps = listOf(makeOshDependency(name = "lib", purl = "pkg:pypi/lib@1"), makeOshDependency(name = "lib", purl = "pkg:pypi/lib@2"))
        val result = map(openSourceHealth = deps)
        assertEquals(1, result.findings.size)
    }

    // rank: Security and Reliability always first (product decision, confirmed with the epic's author)

    @Test
    fun map_lowSeveritySecurity_stillRankedAheadOfCriticalOtherFindings() {
        val secLow = makeSecurityFinding(id = "sec-low", severity = RiskSeverity.Low)
        val oshCritical = makeOshDependency(name = "critical-dep", risk = RiskSeverity.Critical)
        val candidateCritical = makeCandidate(id = "maint-critical", severity = MaintainabilitySeverity.VeryHigh)

        val result = map(maintainability = listOf(candidateCritical), security = listOf(secLow), openSourceHealth = listOf(oshCritical))

        assertEquals("sec-low", result.findings.first().id)
    }

    @Test
    fun map_lowSeverityReliability_stillRankedAheadOfCriticalOtherFindings() {
        val relLow = makeSecurityFinding(id = "rel-low", severity = RiskSeverity.Low)
        val oshCritical = makeOshDependency(name = "critical-dep", risk = RiskSeverity.Critical)

        val result = map(reliability = listOf(relLow), openSourceHealth = listOf(oshCritical))

        assertEquals("rel-low", result.findings.first().id)
    }

    @Test
    fun map_securityAndReliability_mixedAndSortedTogetherBySeverity() {
        val secLow = makeSecurityFinding(id = "sec-low", severity = RiskSeverity.Low, displayFilePath = "b.kt")
        val relCritical = makeSecurityFinding(id = "rel-critical", severity = RiskSeverity.Critical, displayFilePath = "a.kt")

        val result = map(security = listOf(secLow), reliability = listOf(relCritical))

        // Both sit in the same "always first" bucket, sorted by severity - Reliability's Critical finding
        // outranks Security's Low one, proving the bucket isn't secretly still Security-first internally.
        assertEquals(listOf("rel-critical", "sec-low"), result.findings.map { it.id })
    }

    // group: duplication-first sequencing, end-to-end (applies within the remaining bucket only)

    @Test
    fun map_duplicationFirstEligible_sequencedAheadOfOtherFindingsButNotAheadOfSecurityOrReliability() {
        // Overlap: "svc/A.kt" has both a Duplication finding and another Maintainability finding ->
        // 1 of 2 duplication files overlaps = 50%, meeting PriorityGrouper's eligibility threshold.
        val dupSmall = makeCandidate(id = "dupA", category = RefactoringCategory.Duplication, severity = MaintainabilitySeverity.VeryHigh, weight = 50, fileLocations = listOf(FileLocation("svc", "svc/A.kt")))
        val dupBig = makeCandidate(id = "dupB", category = RefactoringCategory.Duplication, severity = MaintainabilitySeverity.VeryHigh, weight = 900, fileLocations = listOf(FileLocation("svc", "svc/B.kt")))
        val otherOnA = makeCandidate(id = "otherOnA", category = RefactoringCategory.UnitSize, severity = MaintainabilitySeverity.Low, fileLocations = listOf(FileLocation("svc", "svc/A.kt")))
        val secCritical = makeSecurityFinding(id = "sec-critical", severity = RiskSeverity.Critical, displayFilePath = "0-first-alphabetically.kt")
        val relCritical = makeSecurityFinding(id = "rel-critical", severity = RiskSeverity.Critical, displayFilePath = "1-second-alphabetically.kt")

        val result = map(maintainability = listOf(dupSmall, dupBig, otherOnA), security = listOf(secCritical), reliability = listOf(relCritical))

        // Security/Reliability first no matter what, then Duplication sequenced ahead of the remaining
        // Maintainability finding within what's left.
        assertEquals(listOf("sec-critical", "rel-critical", "dupB", "dupA", "otherOnA"), result.findings.map { it.id })
    }

    @Test
    fun map_duplicationFirstNotEligible_fallsBackToPlainSeveritySort() {
        // No overlap between the Duplication file and any other Maintainability finding -> not eligible.
        // Uses OSH rather than Security here so the assertion isolates the duplication-first fallback
        // behavior, not the separate Security-always-first rule covered above.
        val dup = makeCandidate(id = "dup", category = RefactoringCategory.Duplication, severity = MaintainabilitySeverity.VeryHigh, weight = 999, fileLocations = listOf(FileLocation("svc", "svc/A.kt")))
        val oshCritical = makeOshDependency(name = "critical-dep", risk = RiskSeverity.Critical)

        val result = map(maintainability = listOf(dup), openSourceHealth = listOf(oshCritical))

        // Both map to PriorityRank.Critical, so the plain sort's displayLocation tie-break decides:
        // "Foo.kt" (dup's default displayLocation) sorts before "critical-dep"'s displayLocation.
        assertEquals(listOf("dup", "critical-dep"), result.findings.map { it.id })
    }
}
