package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilityFindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.FindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCandidate
import com.softwareimprovementgroup.plugins.sigrid.models.SecurityFinding
import com.softwareimprovementgroup.plugins.sigrid.models.toPrioritizedFinding

object PrioritizedFindingMapper {
    fun map(
        maintainability: List<RefactoringCandidate>,
        security: List<SecurityFinding>,
        openSourceHealth: List<OpenSourceHealthDependency>,
    ): List<PrioritizedFinding> {
        val merged = maintainability.filter { it.status == MaintainabilityFindingStatus.Raw }.map { it.toPrioritizedFinding() } +
            security.filter { it.status == FindingStatus.Raw }.map { it.toPrioritizedFinding() } +
            openSourceHealth.map { it.toPrioritizedFinding() }

        return merged.sortedWith(
            compareByDescending<PrioritizedFinding> { it.priorityRank.ordinal }.thenBy { it.displayLocation },
        )
    }
}
