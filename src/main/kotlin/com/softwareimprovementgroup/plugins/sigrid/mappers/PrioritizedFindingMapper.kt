package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.FindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilityFindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFindingResult
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCandidate
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCategory
import com.softwareimprovementgroup.plugins.sigrid.models.SecurityFinding
import com.softwareimprovementgroup.plugins.sigrid.models.toPrioritizedFinding

object PrioritizedFindingMapper {
    private val byPriorityThenLocation =
        compareByDescending<PrioritizedFinding> { it.priorityRank.ordinal }.thenBy { it.displayLocation }

    fun map(
        maintainability: List<RefactoringCandidate>,
        security: List<SecurityFinding>,
        reliability: List<SecurityFinding>,
        openSourceHealth: List<OpenSourceHealthDependency>,
    ): PrioritizedFindingResult {
        val rawMaintainability = maintainability.filter { it.status == MaintainabilityFindingStatus.Raw }
        val merged = mergeRawFindings(rawMaintainability, security, reliability, openSourceHealth)
        val (testCode, everythingElse) = merged.partition { PriorityExclusionFilter.isTestCode(it) }
        val visible = everythingElse.filterNot { PriorityExclusionFilter.exclude(it) }

        return PrioritizedFindingResult(
            findings = rank(visible, rawMaintainability),
            testCodeFindings = testCode.sortedWith(byPriorityThenLocation),
        )
    }

    private fun mergeRawFindings(
        rawMaintainability: List<RefactoringCandidate>,
        security: List<SecurityFinding>,
        reliability: List<SecurityFinding>,
        openSourceHealth: List<OpenSourceHealthDependency>,
    ): List<PrioritizedFinding> =
        rawMaintainability.map { it.toPrioritizedFinding() } +
            security.filter { it.status == FindingStatus.Raw }.map { it.toPrioritizedFinding(PriorityCapability.Security) } +
            reliability.filter { it.status == FindingStatus.Raw }.map { it.toPrioritizedFinding(PriorityCapability.Reliability) } +
            PriorityDeduplicator.collapseByLibraryName(openSourceHealth).map { it.toPrioritizedFinding() }

    // Product decision, confirmed with the epic's author: Security and Reliability findings are always
    // ranked ahead of every other capability, full stop - not just weighted higher within a combined
    // severity sort. Only once those two are placed does duplication-first sequencing apply to what's left.
    private fun rank(visible: List<PrioritizedFinding>, rawMaintainability: List<RefactoringCandidate>): List<PrioritizedFinding> {
        val (urgent, everythingElse) = visible.partition {
            it.capability == PriorityCapability.Security || it.capability == PriorityCapability.Reliability
        }
        return urgent.sortedWith(byPriorityThenLocation) + rankRemaining(everythingElse, rawMaintainability)
    }

    // Duplication findings get sequenced first, by magnitude with a per-component diversity rule, only
    // when this system's own data supports Pascal's rule (PriorityGrouper.duplicationFirstEligible).
    // Otherwise every finding - Duplication included - falls back to the plain severity sort.
    private fun rankRemaining(findings: List<PrioritizedFinding>, rawMaintainability: List<RefactoringCandidate>): List<PrioritizedFinding> {
        if (!PriorityGrouper.duplicationFirstEligible(rawMaintainability)) {
            return findings.sortedWith(byPriorityThenLocation)
        }
        val byId = findings.associateBy { it.id }
        val sequencedDuplication = PriorityGrouper.sequenceDuplicationFirst(rawMaintainability).mapNotNull { byId[it.id] }
        val rest = findings.filterNot { it.refactoringCategory == RefactoringCategory.Duplication }
        return sequencedDuplication + rest.sortedWith(byPriorityThenLocation)
    }
}
