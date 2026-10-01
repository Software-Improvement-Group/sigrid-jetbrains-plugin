package com.softwareimprovementgroup.plugins.sigrid.models

import com.softwareimprovementgroup.plugins.sigrid.promptBuilders.FindingCategory

enum class PriorityCapability(val label: String) {
    Maintainability(FindingCategory.MAINTAINABILITY),
    Security(FindingCategory.SECURITY),
    Reliability(FindingCategory.RELIABILITY),
    OpenSourceHealth(FindingCategory.OPEN_SOURCE_HEALTH),
}

// `findings` is the main ranked list; `testCodeFindings` is the same shape but laned out separately
// (per Epic 432 section 3: test code is excluded from the main list, not hidden entirely). No UI surfaces
// testCodeFindings yet - PrioritizedPanel currently only renders `findings`.
data class PrioritizedFindingResult(
    val findings: List<PrioritizedFinding>,
    val testCodeFindings: List<PrioritizedFinding>,
)

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
    // Null for every capability except Maintainability. Lets PriorityGrouper identify Duplication
    // findings after the merge, since Duplication's severity is uninformative on its own (always
    // VERY_HIGH - see design doc section 2.3) and needs separate "fix first" sequencing instead.
    val refactoringCategory: RefactoringCategory? = null,
    val promotionReason: List<String> = emptyList(),
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
    refactoringCategory = category,
)

// Reliability findings share this exact response/domain shape with Security (both come from
// SecurityFindingMapper.map() - see SigridApiService.getReliabilityFindings()), so the capability is
// passed in rather than hardcoded, letting the same finding type serve both tabs.
fun SecurityFinding.toPrioritizedFinding(capability: PriorityCapability = PriorityCapability.Security) = PrioritizedFinding(
    id = id,
    capability = capability,
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
