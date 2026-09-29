package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.FindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilityFindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFindingResult
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCandidate
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
        val merged = mergeRawFindings(maintainability, security, reliability, openSourceHealth)
        val (testCode, everythingElse) = merged.partition { PriorityExclusionFilter.isTestCode(it) }
        val visible = everythingElse.filterNot { PriorityExclusionFilter.exclude(it) }

        return PrioritizedFindingResult(
            findings = visible.sortedWith(byPriorityThenLocation),
            testCodeFindings = testCode.sortedWith(byPriorityThenLocation),
        )
    }

    private fun mergeRawFindings(
        maintainability: List<RefactoringCandidate>,
        security: List<SecurityFinding>,
        reliability: List<SecurityFinding>,
        openSourceHealth: List<OpenSourceHealthDependency>,
    ): List<PrioritizedFinding> =
        maintainability.filter { it.status == MaintainabilityFindingStatus.Raw }.map { it.toPrioritizedFinding() } +
            security.filter { it.status == FindingStatus.Raw }.map { it.toPrioritizedFinding(PriorityCapability.Security) } +
            reliability.filter { it.status == FindingStatus.Raw }.map { it.toPrioritizedFinding(PriorityCapability.Reliability) } +
            openSourceHealth.map { it.toPrioritizedFinding() }
}
