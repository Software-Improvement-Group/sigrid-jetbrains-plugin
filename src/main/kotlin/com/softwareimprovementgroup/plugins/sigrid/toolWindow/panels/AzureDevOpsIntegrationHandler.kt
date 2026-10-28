package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding

class AzureDevOpsIntegrationHandler<T>(
    private val project: Project,
    private val table: FindingTreeTable<T>,
    private val toIssueFinding: (T) -> IssueFinding,
) {
    fun openCreateAzureDevOpsWorkItemDialog() {
        val findings = table.selectedFindings().map(toIssueFinding)
        if (findings.isEmpty()) return
        CreateAzureDevOpsWorkItemDialog(project, findings).show()
    }
}
