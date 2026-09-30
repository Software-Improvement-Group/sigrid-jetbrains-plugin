package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.FileActivityData
import com.softwareimprovementgroup.plugins.sigrid.models.FindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilityFindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.ObjectiveEvaluationResponse
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
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
        val gated = everythingElse
            .filterNot { PriorityExclusionFilter.exclude(it) }
            .filter { ActivityGate.isActiveOrUnknown(it, fileActivity) } // GATE 2
        val dampened = gated.map { ObjectivesGate.applyIfMet(it, objectives, objectivesConfig, currentRatings) } // GATE 3

        return PrioritizedFindingResult(
            findings = rank(dampened, rawMaintainability, collapsedOsh),
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
        return urgent.sortedWith(byPriorityThenLocation) + rankRemaining(everythingElse, rawMaintainability, collapsedOsh)
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
        return urgentOsh.sortedWith(byPriorityThenLocation) + sequenceOrSort(rest, rawMaintainability)
    }

    private fun sequenceOrSort(findings: List<PrioritizedFinding>, rawMaintainability: List<RefactoringCandidate>): List<PrioritizedFinding> {
        if (!PriorityGrouper.duplicationFirstEligible(rawMaintainability)) {
            return findings.sortedWith(byPriorityThenLocation)
        }
        val byId = findings.associateBy { it.id }
        val sequencedDuplication = PriorityGrouper.sequenceDuplicationFirst(rawMaintainability).mapNotNull { byId[it.id] }
        val rest = findings.filterNot { it.refactoringCategory == RefactoringCategory.Duplication }
        return sequencedDuplication + rest.sortedWith(byPriorityThenLocation)
    }
}
