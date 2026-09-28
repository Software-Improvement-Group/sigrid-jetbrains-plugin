package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.mappers.OpenSourceHealthMapper
import com.softwareimprovementgroup.plugins.sigrid.mappers.PrioritizedFindingMapper
import com.softwareimprovementgroup.plugins.sigrid.mappers.RefactoringCandidateMapper
import com.softwareimprovementgroup.plugins.sigrid.mappers.SecurityFindingMapper
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.FixItContext
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityRank
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import com.softwareimprovementgroup.plugins.sigrid.models.toSeverityEmoji
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService

class PrioritizedPanel(project: Project) : SigridPanel<PrioritizedFinding>(
    project,
    arrayOf(SigridBundle["column.capability"], SigridBundle["column.risk"], SigridBundle["column.location"], SigridBundle["column.description"], SigridBundle["column.status"]),
    centeredColumns = setOf(SigridBundle["column.capability"], SigridBundle["column.risk"], SigridBundle["column.status"]),
    columnFilters = listOf(
        ColumnFilterDef(
            columnName = SigridBundle["column.capability"],
            options = PriorityCapability.entries.map { FilterOption(it.label, it.name) },
            getOptionId = { capability.name },
        ),
        ColumnFilterDef(
            columnName = SigridBundle["column.risk"],
            options = PriorityRank.entries.map { FilterOption(it.toRiskIcon().label, it.name) },
            getOptionId = { priorityRank.name },
        ),
    ),
) {
    override val emptyMessage = SigridBundle["prioritized.empty"]

    override fun fetch(subsystem: String): List<PrioritizedFinding> {
        val api = SigridApiService.getInstance()
        val maintainability = RefactoringCandidateMapper.map(api.getAllRefactoringCandidates(project), subsystem)
        val security = SecurityFindingMapper.map(api.getSecurityFindings(project), subsystem)
        val reliability = SecurityFindingMapper.map(api.getReliabilityFindings(project), subsystem)
        val openSourceHealth = OpenSourceHealthMapper.map(api.getOpenSourceHealthFindings(project), subsystem)
        return PrioritizedFindingMapper.map(maintainability, security, openSourceHealth)
    }

    override fun PrioritizedFinding.matchesSearch(query: String) =
        capability.label.contains(query, ignoreCase = true) ||
        displayLocation.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true) ||
        statusLabel.contains(query, ignoreCase = true)

    override fun PrioritizedFinding.toRow(): Array<Any> = arrayOf(
        capability.label,
        priorityRank.toRiskIcon(),
        displayLocation,
        description,
        statusLabel,
    )

    override fun PrioritizedFinding.getFileLocations(): List<FileLocation> = fileLocations

    override fun PrioritizedFinding.toIssueFinding() = IssueFinding(
        title = description,
        severityEmoji = priorityRank.toSeverityEmoji(),
        fileLocations = fileLocations,
    )

    override fun PrioritizedFinding.toFixItContext() = FixItContext(
        category = capability.label,
        severity = priorityRank.toRiskIcon().label,
        title = "$displayLocation: $description",
        fileLocations = fileLocations,
        href = href,
    )

    override fun PrioritizedFinding.getHref() = href
    override fun PrioritizedFinding.isEditable() = editable
    override fun PrioritizedFinding.getId() = id
    override fun PrioritizedFinding.getDisplayLocation() = displayLocation
    override fun PrioritizedFinding.getEditDescription() = description
    override fun PrioritizedFinding.getStatusOptions() = statusOptions
    override fun PrioritizedFinding.getCurrentStatus() = currentStatusValue
    override fun PrioritizedFinding.getCurrentRemark() = remark
}
