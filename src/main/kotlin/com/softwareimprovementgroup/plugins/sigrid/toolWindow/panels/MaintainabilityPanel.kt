package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.mappers.RefactoringCandidateMapper
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilityFindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCandidate
import com.softwareimprovementgroup.plugins.sigrid.models.snakeCaseToTitleCase
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService

class MaintainabilityPanel(project: Project) : SigridPanel<RefactoringCandidate>(
    project,
    arrayOf(SigridBundle["column.risk"], SigridBundle["column.location"], SigridBundle["column.description"], SigridBundle["column.status"]),
    setOf(SigridBundle["column.risk"], SigridBundle["column.status"])
) {
    override val emptyMessage = SigridBundle["maintainability.empty"]

    override fun fetch(subsystem: String): List<RefactoringCandidate> =
        RefactoringCandidateMapper.map(SigridApiService.getInstance().getAllRefactoringCandidates(project), subsystem)

    override fun RefactoringCandidate.matchesSearch(query: String) =
        displayLocation.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true) ||
        statusLabel.contains(query, ignoreCase = true)

    override fun RefactoringCandidate.toRow(): Array<Any> = arrayOf(
        severity.toRiskIcon(),
        displayLocation,
        description,
        statusLabel,
    )

    override fun RefactoringCandidate.getFileLocations(): List<FileLocation> = fileLocations

    override fun RefactoringCandidate.isEditable() = true
    override fun RefactoringCandidate.getId() = id
    override fun RefactoringCandidate.getEditDescription() = description
    override fun RefactoringCandidate.getStatusOptions() = MaintainabilityFindingStatus.entries.map { "${it.icon} ${snakeCaseToTitleCase(it.apiValue)}" to it.apiValue }
    override fun RefactoringCandidate.getCurrentStatus() = status.apiValue
    override fun RefactoringCandidate.getCurrentRemark() = remark
}