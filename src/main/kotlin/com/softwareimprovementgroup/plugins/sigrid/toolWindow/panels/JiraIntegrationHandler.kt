package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding

class JiraIntegrationHandler<T>(
    private val project: Project,
    private val table: FindingTreeTable<T>,
    private val toIssueFinding: (T) -> IssueFinding,
) {
    fun openCreateJiraIssueDialog() {
        val jiraFindings = table.selectedFindings().map(toIssueFinding)
        if (jiraFindings.isEmpty()) return
        CreateJiraIssueDialog(project, jiraFindings).show()
    }
}
