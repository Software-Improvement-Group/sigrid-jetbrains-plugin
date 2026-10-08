package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
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
        vulnerabilities: List<OshVulnerability> = emptyList(),
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
        vulnerabilities = vulnerabilities,
    )

    private fun makeVulnerability(id: String = "CVE-2024-1234", severity: RiskSeverity = RiskSeverity.Critical) =
        OshVulnerability(id = id, severity = severity, score = null, method = null)

    private fun makeActivity(hasHistory: Boolean = true, fileActivities: List<FileActivity> = emptyList()) =
        FileActivityData(hasHistory = hasHistory, historyStartDate = null, historyEndDate = null, fileActivities = fileActivities)

    private fun makeObjective(feature: String, targetMetAtEnd: String?) = ObjectiveEvaluationResponse(
        type = "irrelevant",
        feature = feature,
        target = null,
        targetMetAtStart = null,
        targetMetAtEnd = targetMetAtEnd,
        delta = null,
        stateAtEnd = null,
        level = "SYSTEM",
        parentId = null,
    )

    private fun map(
        maintainability: List<RefactoringCandidate> = emptyList(),
        security: List<SecurityFinding> = emptyList(),
        reliability: List<SecurityFinding> = emptyList(),
        openSourceHealth: List<OpenSourceHealthDependency> = emptyList(),
        fileActivity: FileActivityData = makeActivity(hasHistory = false),
        objectives: List<ObjectiveEvaluationResponse> = emptyList(),
        objectivesConfig: Map<String, Any> = emptyMap(),
        currentRatings: Map<PriorityCapability, Double> = emptyMap(),
    ) = PrioritizedFindingMapper.map(
        maintainability, security, reliability, openSourceHealth, fileActivity, objectives, objectivesConfig, currentRatings,
    )

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

    // gate 1: urgency override (OSH CVE/GHSA + high severity, within the non-Security/Reliability bucket)

    @Test
    fun map_urgentOsh_promotedAheadOfMaintainabilityButNotAheadOfSecurity() {
        val urgentOsh = makeOshDependency(name = "urgent-dep", risk = RiskSeverity.Low, vulnerabilities = listOf(makeVulnerability(severity = RiskSeverity.Critical)))
        val maint = makeCandidate(id = "maint-critical", severity = MaintainabilitySeverity.VeryHigh)
        val sec = makeSecurityFinding(id = "sec-critical", severity = RiskSeverity.Critical)

        val result = map(maintainability = listOf(maint), security = listOf(sec), openSourceHealth = listOf(urgentOsh))

        // Security first no matter what, then the urgent OSH finding ahead of Maintainability - even
        // though its own OSH risk field (Low) would otherwise rank it last.
        assertEquals(listOf("sec-critical", "urgent-dep", "maint-critical"), result.findings.map { it.id })
    }

    @Test
    fun map_oshWithoutRealIdentifier_notTreatedAsUrgent() {
        val notUrgent = makeOshDependency(name = "not-urgent", risk = RiskSeverity.Low, vulnerabilities = listOf(makeVulnerability(id = "internal-1", severity = RiskSeverity.Critical)))
        val maint = makeCandidate(id = "maint-critical", severity = MaintainabilitySeverity.VeryHigh)

        val result = map(maintainability = listOf(maint), openSourceHealth = listOf(notUrgent))

        // Plain severity sort applies: Maintainability's Critical outranks OSH's Low.
        assertEquals(listOf("maint-critical", "not-urgent"), result.findings.map { it.id })
    }

    // gate 2: activity (Maintainability only, fails open with no/unknown history)

    @Test
    fun map_maintainabilityInDormantFile_gatedOutWhenHistoryKnown() {
        val candidates = listOf(makeCandidate(id = "dormant", fileLocations = listOf(FileLocation("svc", "svc/Dormant.kt"))))
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Dormant.kt", 0.0, 0.0)))

        val result = map(maintainability = candidates, fileActivity = activity)

        assertTrue(result.findings.isEmpty())
    }

    @Test
    fun map_maintainabilityInActiveFile_kept() {
        val candidates = listOf(makeCandidate(id = "active", fileLocations = listOf(FileLocation("svc", "svc/Active.kt"))))
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Active.kt", 4.0, 2.0)))

        val result = map(maintainability = candidates, fileActivity = activity)

        assertEquals(listOf("active"), result.findings.map { it.id })
    }

    @Test
    fun map_maintainabilityNoActivityDataForSystem_failsOpen() {
        val candidates = listOf(makeCandidate(id = "unknown-activity", fileLocations = listOf(FileLocation("svc", "svc/Unknown.kt"))))

        val result = map(maintainability = candidates, fileActivity = makeActivity(hasHistory = false))

        assertEquals(listOf("unknown-activity"), result.findings.map { it.id })
    }

    @Test
    fun map_securityInDormantFile_notGated() {
        // Gate 2 is Maintainability-only - Security findings are never activity-gated.
        val findings = listOf(makeSecurityFinding(id = "sec", fileLocations = listOf(FileLocation("svc", "svc/Dormant.kt"))))
        val activity = makeActivity(fileActivities = listOf(FileActivity("svc/Dormant.kt", 0.0, 0.0)))

        val result = map(security = findings, fileActivity = activity)

        assertEquals(listOf("sec"), result.findings.map { it.id })
    }

    // gate 3: objectives (dampens by one tier when the capability's objective is already met)

    @Test
    fun map_maintainabilityActualAtConfiguredTarget_allMaintainabilityFindingsDampenedOneTier() {
        val critical = makeCandidate(id = "was-critical", severity = MaintainabilitySeverity.VeryHigh, displayLocation = "a.kt")
        val high = makeCandidate(id = "was-high", severity = MaintainabilitySeverity.High, displayLocation = "b.kt")
        val config = mapOf("MAINTAINABILITY" to 4.0)
        val ratings = mapOf(PriorityCapability.Maintainability to 4.0)

        val result = map(maintainability = listOf(critical, high), objectivesConfig = config, currentRatings = ratings)

        // The target is met for the whole Maintainability capability, so both findings are dampened by
        // one tier (Critical->High, High->Medium) - relative order is unaffected, but the ranks shift down.
        assertEquals(listOf("was-critical", "was-high"), result.findings.map { it.id })
        assertEquals(PriorityRank.High, result.findings.first { it.id == "was-critical" }.priorityRank)
        assertEquals(PriorityRank.Medium, result.findings.first { it.id == "was-high" }.priorityRank)
    }

    @Test
    fun map_maintainabilityActualBelowConfiguredTarget_noDampening() {
        // The design doc's own worked example: 3.48 actual vs. 3.5 target is a documented miss.
        val maint = makeCandidate(id = "maint-critical", severity = MaintainabilitySeverity.VeryHigh)
        val config = mapOf("MAINTAINABILITY" to 3.5)
        val ratings = mapOf(PriorityCapability.Maintainability to 3.48)

        val result = map(maintainability = listOf(maint), objectivesConfig = config, currentRatings = ratings)

        assertEquals(PriorityRank.Critical, result.findings.first().priorityRank)
    }

    @Test
    fun map_informationSeveritySecurityFinding_metObjective_dampensToLowNotUnknown() {
        // Regression test for a reported bug: an Information-severity Security finding maps to
        // PriorityRank.Low. With its objective met, Gate 3 must dampen it to Low (its floor), not
        // Unknown - Unknown is reserved for "severity couldn't be determined" elsewhere in the mapper.
        val info = makeSecurityFinding(id = "sec-info", severity = RiskSeverity.Information)
        val objectives = listOf(makeObjective("SECURITY", "MET"))

        val result = map(security = listOf(info), objectives = objectives)

        assertEquals(PriorityRank.Low, result.findings.first().priorityRank)
    }

    @Test
    fun map_noConfiguredObjective_ratingAboveMarketAverage_dampensViaFallback() {
        val maint = makeCandidate(id = "maint-critical", severity = MaintainabilitySeverity.VeryHigh)
        val ratings = mapOf(PriorityCapability.Maintainability to 3.8)

        val result = map(maintainability = listOf(maint), objectives = emptyList(), currentRatings = ratings)

        assertEquals(PriorityRank.High, result.findings.first().priorityRank)
    }

    @Test
    fun map_noConfiguredObjective_ratingBelowMarketAverage_noDampening() {
        val maint = makeCandidate(id = "maint-critical", severity = MaintainabilitySeverity.VeryHigh)
        val ratings = mapOf(PriorityCapability.Maintainability to 2.5)

        val result = map(maintainability = listOf(maint), objectives = emptyList(), currentRatings = ratings)

        assertEquals(PriorityRank.Critical, result.findings.first().priorityRank)
    }

    @Test
    fun map_noConfiguredObjectiveAndNoRating_noDampening() {
        val maint = makeCandidate(id = "maint-critical", severity = MaintainabilitySeverity.VeryHigh)

        val result = map(maintainability = listOf(maint), objectives = emptyList(), currentRatings = emptyMap())

        assertEquals(PriorityRank.Critical, result.findings.first().priorityRank)
    }

    @Test
    fun map_securityAndReliability_unaffectedByMaintainabilityObjective() {
        val sec = makeSecurityFinding(id = "sec-critical", severity = RiskSeverity.Critical)
        val objectives = listOf(makeObjective("MAINTAINABILITY", "MET"))

        val result = map(security = listOf(sec), objectives = objectives)

        assertEquals(PriorityRank.Critical, result.findings.first().priorityRank)
    }

    // promotionReason: each pipeline stage that does something non-obvious to a finding's position or
    // severity must label why, per Epic 432 section 3's "why it's here" requirement.

    @Test
    fun map_securityFinding_hasAlwaysFirstPromotionReason() {
        val result = map(security = listOf(makeSecurityFinding(id = "sec-critical", severity = RiskSeverity.Critical)))
        assertEquals(
            listOf(SigridBundle["prioritized.reason.always.first", PriorityCapability.Security.label]),
            result.findings.first().promotionReason,
        )
    }

    @Test
    fun map_reliabilityFinding_hasAlwaysFirstPromotionReasonLabeledReliability() {
        val result = map(reliability = listOf(makeSecurityFinding(id = "rel-critical", severity = RiskSeverity.Critical)))
        assertEquals(
            listOf(SigridBundle["prioritized.reason.always.first", PriorityCapability.Reliability.label]),
            result.findings.first().promotionReason,
        )
    }

    @Test
    fun map_urgentOsh_hasUrgencyOverridePromotionReason() {
        val urgentOsh = makeOshDependency(name = "urgent-dep", risk = RiskSeverity.Low, vulnerabilities = listOf(makeVulnerability(severity = RiskSeverity.Critical)))
        val result = map(openSourceHealth = listOf(urgentOsh))
        assertEquals(listOf(SigridBundle["prioritized.reason.urgency.override"]), result.findings.first().promotionReason)
    }

    @Test
    fun map_duplicationFirstEligible_hasDuplicationFirstPromotionReasonOnDuplicationFindingsOnly() {
        val dupSmall = makeCandidate(id = "dupA", category = RefactoringCategory.Duplication, severity = MaintainabilitySeverity.VeryHigh, weight = 50, fileLocations = listOf(FileLocation("svc", "svc/A.kt")))
        val dupBig = makeCandidate(id = "dupB", category = RefactoringCategory.Duplication, severity = MaintainabilitySeverity.VeryHigh, weight = 900, fileLocations = listOf(FileLocation("svc", "svc/B.kt")))
        val otherOnA = makeCandidate(id = "otherOnA", category = RefactoringCategory.UnitSize, severity = MaintainabilitySeverity.Low, fileLocations = listOf(FileLocation("svc", "svc/A.kt")))

        val byId = map(maintainability = listOf(dupSmall, dupBig, otherOnA)).findings.associateBy { it.id }

        assertEquals(listOf(SigridBundle["prioritized.reason.duplication.first"]), byId["dupB"]?.promotionReason)
        assertEquals(listOf(SigridBundle["prioritized.reason.duplication.first"]), byId["dupA"]?.promotionReason)
        assertTrue(byId["otherOnA"]?.promotionReason?.isEmpty() ?: false)
    }

    @Test
    fun map_plainlyRankedFinding_hasEmptyPromotionReason() {
        // No gate/rule did anything unusual here - the UI falls back to a default "ranked by severity"
        // label for this case rather than showing a blank cell (see PrioritizedPanel.reasonLabel).
        val result = map(maintainability = listOf(makeCandidate(id = "m1")))
        assertTrue(result.findings.first().promotionReason.isEmpty())
    }

    @Test
    fun map_objectiveMetFinding_carriesBothObjectiveMetAndAlwaysFirstReasons() {
        // A Security finding that's both "always first" AND objective-dampened must carry both
        // explanations, proving the append-only design doesn't lose either one. Gate 3 runs before rank(),
        // so its reason is appended first.
        val sec = makeSecurityFinding(id = "sec-critical", severity = RiskSeverity.Critical)
        val objectives = listOf(makeObjective("SECURITY", "MET"))

        val result = map(security = listOf(sec), objectives = objectives)

        assertEquals(
            listOf(
                SigridBundle["prioritized.reason.objective.met", PriorityCapability.Security.label],
                SigridBundle["prioritized.reason.always.first", PriorityCapability.Security.label],
            ),
            result.findings.first().promotionReason,
        )
    }

    // capToTopN: never drop a Critical-rank finding, no matter how long the ranked list is, but otherwise
    // cap what's shown (Epic 432 OUTPUT step + Open Decision #2).

    @Test
    fun map_atOrBelowMaxVisibleFindings_notCapped() {
        val candidates = (0 until 50).map { i -> makeCandidate(id = "m$i", displayLocation = "m%02d.kt".format(i)) }
        val result = map(maintainability = candidates)
        assertEquals(50, result.findings.size)
    }

    @Test
    fun map_moreThanMaxVisibleFindings_neverDropsCriticalRankFindings() {
        val criticals = (0 until 55).map { i ->
            makeCandidate(id = "crit$i", severity = MaintainabilitySeverity.VeryHigh, displayLocation = "c%02d.kt".format(i))
        }
        val lows = (0 until 10).map { i ->
            makeCandidate(id = "low$i", severity = MaintainabilitySeverity.Low, displayLocation = "z%02d.kt".format(i))
        }

        val result = map(maintainability = criticals + lows)

        // All 55 Critical-rank findings survive even though the combined list (65) exceeds the cap; the
        // 10 Low-rank findings are correctly dropped since they fall outside the cap and aren't Critical.
        assertEquals(55, result.findings.size)
        assertTrue(result.findings.all { it.priorityRank == PriorityRank.Critical })
        assertTrue(result.findings.none { it.id.startsWith("low") })
    }

    @Test
    fun map_moreThanMaxVisibleFindings_preservesVisibleWindowOrderThenAppendsTruncatedCriticals() {
        val criticals = (0 until 55).map { i ->
            makeCandidate(id = "crit$i", severity = MaintainabilitySeverity.VeryHigh, displayLocation = "c%02d.kt".format(i))
        }

        val result = map(maintainability = criticals)

        // First 50 appear in their normal ranked order (ascending displayLocation, since all tie on rank);
        // the last 5, which fell past the cap, are appended afterward rather than reordered to the front.
        val expectedVisible = (0 until 50).map { "crit$it" }
        val expectedAppended = (50 until 55).map { "crit$it" }
        assertEquals(expectedVisible + expectedAppended, result.findings.map { it.id })
    }
}
