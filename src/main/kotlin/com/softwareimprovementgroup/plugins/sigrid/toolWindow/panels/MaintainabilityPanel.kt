package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.mappers.RefactoringCandidateMapper
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCandidate
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService

class MaintainabilityPanel(project: Project) : SigridPanel<RefactoringCandidate>(project, arrayOf("Risk", "Location", "Description", "Status")) {
    override val emptyMessage = "No refactoring candidates found."

    override fun fetch(subsystem: String): List<RefactoringCandidate> =
        RefactoringCandidateMapper.map(SigridApiService.getInstance().getAllRefactoringCandidates(project), subsystem)

    override fun RefactoringCandidate.matchesSearch(query: String) =
        displayLocation.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true) ||
        statusLabel.contains(query, ignoreCase = true)

    override fun RefactoringCandidate.toRow(): Array<String> = arrayOf(
        severity.name,
        displayLocation,
        description,
        statusLabel,
    )
}