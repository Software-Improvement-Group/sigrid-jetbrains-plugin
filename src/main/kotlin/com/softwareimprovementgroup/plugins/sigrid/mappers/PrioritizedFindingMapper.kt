package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FileActivityData
import com.softwareimprovementgroup.plugins.sigrid.models.FindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilityFindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.ObjectiveEvaluationResponse
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityRank
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFindingResult
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCandidate
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCategory
import com.softwareimprovementgroup.plugins.sigrid.models.SecurityFinding
import com.softwareimprovementgroup.plugins.sigrid.models.toPrioritizedFinding

object PrioritizedFindingMapper {
    // "No history known" default - ActivityGate fails open on this, so callers that don't have
    // Architecture Quality data yet (e.g. existing tests) see unchanged, ungated behavior.
    private val NO_ACTIVITY_DATA = FileActivityData(hasHistory = false, historyStartDate = null, historyEndDate = null, fileActivities = emptyList())

    // Epic 432 OUTPUT step + Open Decision #2: "a capped, explainable top-N list... the data doesn't
    // support an unbounded 'true' global rank". The exact number is explicitly flagged in the design doc
    // as a product call, not a data-backed one ("yours to make, not a data question") - this is a
    // provisional placeholder, not a validated figure. Change freely; see capToTopN's doc comment for how
    // it's applied (every Critical-rank finding is always shown, regardless of this number).
    private const val MAX_VISIBLE_FINDINGS = 50

    private val byPriorityThenLocation =
        compareByDescending<PrioritizedFinding> { it.priorityRank.ordinal }.thenBy { it.displayLocation }

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
        val collapsedOsh = PriorityDeduplicator.collapseByLibraryName(openSourceHealth)
        val merged = mergeRawFindings(rawMaintainability, security, reliability, collapsedOsh)

        val (testCode, everythingElse) = merged.partition { PriorityExclusionFilter.isTestCode(it) }
        val gated = ActivityGate(fileActivity).filter(everythingElse.filterNot { PriorityExclusionFilter.exclude(it) }) // GATE 2
        val dampened = gated.map { ObjectivesGate.applyIfMet(it, objectives, objectivesConfig, currentRatings) } // GATE 3

        return PrioritizedFindingResult(
            findings = capToTopN(rank(dampened, rawMaintainability, collapsedOsh)),
            testCodeFindings = testCode.sortedWith(byPriorityThenLocation),
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

    // Product decision, confirmed with the epic's author: Security and Reliability findings are always
    // ranked ahead of every other capability, full stop - not just weighted higher within a combined
    // severity sort. Only once those two are placed does Gate 1 (urgency) and duplication-first
    // sequencing apply to what's left.
    private fun rank(
        visible: List<PrioritizedFinding>,
        rawMaintainability: List<RefactoringCandidate>,
        collapsedOsh: List<OpenSourceHealthDependency>,
    ): List<PrioritizedFinding> {
        val (urgent, everythingElse) = visible.partition {
            it.capability == PriorityCapability.Security || it.capability == PriorityCapability.Reliability
        }
        val labeledUrgent = urgent.map { withReason(it, SigridBundle["prioritized.reason.always.first", it.capability.label]) }
        return labeledUrgent.sortedWith(byPriorityThenLocation) + rankRemaining(everythingElse, rawMaintainability, collapsedOsh)
    }

    // Within the non-Security/Reliability bucket: Gate 1's urgency-override OSH findings go first, then
    // Duplication findings are sequenced ahead of the rest when this system's own data supports Pascal's
    // rule, otherwise everything falls back to the plain severity sort.
    private fun rankRemaining(
        findings: List<PrioritizedFinding>,
        rawMaintainability: List<RefactoringCandidate>,
        collapsedOsh: List<OpenSourceHealthDependency>,
    ): List<PrioritizedFinding> {
        val urgentIds = UrgencyGate.urgentFindingIds(collapsedOsh)
        val (urgentOsh, rest) = findings.partition { it.capability == PriorityCapability.OpenSourceHealth && it.id in urgentIds }
        val labeledUrgentOsh = urgentOsh.map { withReason(it, SigridBundle["prioritized.reason.urgency.override"]) }
        return labeledUrgentOsh.sortedWith(byPriorityThenLocation) + sequenceOrSort(rest, rawMaintainability)
    }

    private fun sequenceOrSort(findings: List<PrioritizedFinding>, rawMaintainability: List<RefactoringCandidate>): List<PrioritizedFinding> {
        if (!PriorityGrouper.duplicationFirstEligible(rawMaintainability)) {
            return findings.sortedWith(byPriorityThenLocation)
        }
        val byId = findings.associateBy { it.id }
        val sequencedDuplication = PriorityGrouper.sequenceDuplicationFirst(rawMaintainability)
            .mapNotNull { byId[it.id] }
            .map { withReason(it, SigridBundle["prioritized.reason.duplication.first"]) }
        val rest = findings.filterNot { it.refactoringCategory == RefactoringCategory.Duplication }
        return sequencedDuplication + rest.sortedWith(byPriorityThenLocation)
    }

    private fun withReason(finding: PrioritizedFinding, reason: String): PrioritizedFinding =
        finding.copy(promotionReason = finding.promotionReason + reason)

    // OUTPUT step (design doc section 3): never drop a Critical-rank finding, no matter how long the
    // ranked list is, but otherwise cap what's shown. Takes the first MAX_VISIBLE_FINDINGS exactly as
    // ranked (preserving the Security/Reliability-always-first and Gate 1/duplication-first placement
    // rules above), then appends any Critical-rank findings that fell past the cut - still visible, just
    // not reordered to the very front of the capped list.
    private fun capToTopN(ranked: List<PrioritizedFinding>): List<PrioritizedFinding> {
        if (ranked.size <= MAX_VISIBLE_FINDINGS) return ranked
        val visible = ranked.take(MAX_VISIBLE_FINDINGS)
        val truncatedCriticals = ranked.drop(MAX_VISIBLE_FINDINGS).filter { it.priorityRank == PriorityRank.Critical }
        return visible + truncatedCriticals
    }
}
