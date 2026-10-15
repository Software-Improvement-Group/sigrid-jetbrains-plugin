package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FileActivityData
import com.softwareimprovementgroup.plugins.sigrid.models.FindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilityFindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.ObjectiveEvaluationResponse
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityTier
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFindingResult
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCandidate
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCategory
import com.softwareimprovementgroup.plugins.sigrid.models.RiskSeverity
import com.softwareimprovementgroup.plugins.sigrid.models.SecurityFinding
import com.softwareimprovementgroup.plugins.sigrid.models.toPrioritizedFinding

object PrioritizedFindingMapper {
    // "No history known" default - ActivityGate fails open on this, so callers that don't have
    // Architecture Quality data yet (e.g. existing tests) see unchanged, ungated behavior.
    private val NO_ACTIVITY_DATA = FileActivityData(hasHistory = false, historyStartDate = null, historyEndDate = null, fileActivities = emptyList())

    private val byPriorityThenMetric =
        compareByDescending<PrioritizedFinding> { it.priorityRank.ordinal }
            .thenBy(nullsLast()) { it.refactoringCategory }
            .thenByDescending { it.sortMetric }
            .thenBy { it.displayLocation }
            .thenBy(nullsLast()) { it.startLine }

    fun map(
        maintainability: List<RefactoringCandidate>,
        security: List<SecurityFinding>,
        reliability: List<SecurityFinding>,
        openSourceHealth: List<OpenSourceHealthDependency>,
        fileActivity: FileActivityData = NO_ACTIVITY_DATA,
        objectives: List<ObjectiveEvaluationResponse> = emptyList(),
        objectivesConfig: Map<String, Any> = emptyMap(),
        currentRatings: Map<PriorityCapability, Double> = emptyMap(),
    ): PrioritizedFindingResult {
        val rawMaintainability = maintainability.filter { it.status == MaintainabilityFindingStatus.Raw }
        val collapsedOsh = PriorityDeduplicator.collapseByLibraryName(openSourceHealth).filter { it.hasVulnerability() }
        val merged = mergeRawFindings(rawMaintainability, security, reliability, collapsedOsh)

        val (testCode, everythingElse) = merged.partition { PriorityExclusionFilter.isTestCode(it) }
        val gated = ActivityGate(fileActivity).filter(everythingElse.filterNot { PriorityExclusionFilter.exclude(it) }) // GATE 2
        val dampened = gated.map { ObjectivesGate.applyIfMet(it, objectives, objectivesConfig, currentRatings) } // GATE 3

        return PrioritizedFindingResult(
            findings = rank(dampened, rawMaintainability),
            testCodeFindings = testCode.sortedWith(byPriorityThenMetric),
        )
    }

    private fun mergeRawFindings(
        rawMaintainability: List<RefactoringCandidate>,
        security: List<SecurityFinding>,
        reliability: List<SecurityFinding>,
        collapsedOsh: List<OpenSourceHealthDependency>,
    ): List<PrioritizedFinding> =
        rawMaintainability.map { it.toPrioritizedFinding() } +
            security.filter { it.status == FindingStatus.Raw }.map { it.toPrioritizedFinding(PriorityCapability.Security) } +
            reliability.filter { it.status == FindingStatus.Raw }.map { it.toPrioritizedFinding(PriorityCapability.Reliability) } +
            collapsedOsh.map { it.toPrioritizedFinding() }

    // Product decision: Security findings and Open Source Health vulnerabilities are always ranked ahead
    // of everything else, then Reliability, regardless of severity. Only once those are placed does
    // duplication-first sequencing apply to what's left.
    private fun rank(
        visible: List<PrioritizedFinding>,
        rawMaintainability: List<RefactoringCandidate>,
    ): List<PrioritizedFinding> {
        val (vulnerabilities, others) = visible.partition { it.capability.isVulnerabilityCapability() }
        val (reliability, rest) = others.partition { it.capability == PriorityCapability.Reliability }
        return promoteAndSort(vulnerabilities, PriorityTier.VulnerabilityFirst) +
            promoteAndSort(reliability, PriorityTier.AlwaysFirst) +
            sequenceOrSort(rest, rawMaintainability)
    }

    private fun promoteAndSort(findings: List<PrioritizedFinding>, tier: PriorityTier): List<PrioritizedFinding> =
        findings
            .map { promote(it, tier, SigridBundle["prioritized.reason.always.first", it.capability.label]) }
            .sortedWith(byPriorityThenMetric)

    private fun PriorityCapability.isVulnerabilityCapability() =
        this == PriorityCapability.Security || this == PriorityCapability.OpenSourceHealth

    private fun sequenceOrSort(findings: List<PrioritizedFinding>, rawMaintainability: List<RefactoringCandidate>): List<PrioritizedFinding> {
        if (!PriorityGrouper.duplicationFirstEligible(rawMaintainability)) {
            return findings.sortedWith(byPriorityThenMetric)
        }
        val byId = findings.associateBy { it.id }
        val sequencedDuplication = PriorityGrouper.sequenceDuplicationFirst(rawMaintainability)
            .mapNotNull { byId[it.id] }
            .map { promote(it, PriorityTier.DuplicationFirst, SigridBundle["prioritized.reason.duplication.first"]) }
        val rest = findings.filterNot { it.refactoringCategory == RefactoringCategory.Duplication }
        return sequencedDuplication + rest.sortedWith(byPriorityThenMetric)
    }

    private fun promote(finding: PrioritizedFinding, tier: PriorityTier, reason: String): PrioritizedFinding =
        finding.copy(promotionReason = finding.promotionReason + reason, tier = tier)

    private fun OpenSourceHealthDependency.hasVulnerability() = vulnerabilityRisk >= RiskSeverity.Low
}
