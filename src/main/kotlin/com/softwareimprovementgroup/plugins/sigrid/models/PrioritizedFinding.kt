package com.softwareimprovementgroup.plugins.sigrid.models

import com.softwareimprovementgroup.plugins.sigrid.promptBuilders.FindingCategory

enum class PriorityCapability(val label: String) {
    Maintainability(FindingCategory.MAINTAINABILITY),
    Security(FindingCategory.SECURITY),
    OpenSourceHealth(FindingCategory.OPEN_SOURCE_HEALTH),
}

data class PrioritizedFinding(
    val id: String,
    val capability: PriorityCapability,
    val priorityRank: PriorityRank,
    val displayLocation: String,
    val description: String,
    val statusLabel: String,
    val remark: String,
    val fileLocations: List<FileLocation>,
    val href: String?,
    val editable: Boolean,
    val statusOptions: List<Pair<String, String>>,
    val currentStatusValue: String,
)

fun RefactoringCandidate.toPrioritizedFinding() = PrioritizedFinding(
    id = id,
    capability = PriorityCapability.Maintainability,
    priorityRank = severity.toPriorityRank(),
    displayLocation = displayLocation,
    description = description,
    statusLabel = statusLabel,
    remark = remark,
    fileLocations = fileLocations,
    href = href,
    editable = true,
    statusOptions = MaintainabilityFindingStatus.entries.map { "${it.icon} ${snakeCaseToTitleCase(it.apiValue)}" to it.apiValue },
    currentStatusValue = status.apiValue,
)

fun SecurityFinding.toPrioritizedFinding() = PrioritizedFinding(
    id = id,
    capability = PriorityCapability.Security,
    priorityRank = severity.toPriorityRank(),
    displayLocation = displayFilePath,
    description = type,
    statusLabel = statusLabel,
    remark = remark,
    fileLocations = fileLocations,
    href = href,
    editable = true,
    statusOptions = FindingStatus.entries.map { "${it.icon} ${snakeCaseToTitleCase(it.apiValue)}" to it.apiValue },
    currentStatusValue = status.apiValue,
)

fun OpenSourceHealthDependency.toPrioritizedFinding() = PrioritizedFinding(
    id = purl ?: name,
    capability = PriorityCapability.OpenSourceHealth,
    priorityRank = risk.toPriorityRank(),
    displayLocation = displayName,
    description = "$displayName $version",
    statusLabel = "",
    remark = "",
    fileLocations = fileLocations,
    href = href,
    editable = false,
    statusOptions = emptyList(),
    currentStatusValue = "",
)
