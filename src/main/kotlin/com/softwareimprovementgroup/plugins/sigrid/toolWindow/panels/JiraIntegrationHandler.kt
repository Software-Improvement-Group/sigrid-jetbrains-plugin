package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.intellij.ui.table.JBTable
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding

class JiraIntegrationHandler<T>(
    private val project: Project,
    private val table: JBTable,
    private val getDisplayedFindings: () -> List<T>,
    private val toIssueFinding: (T) -> IssueFinding,
) {
    fun openCreateJiraIssueDialog() {
        val jiraFindings = table.selectedRows
            .map { table.convertRowIndexToModel(it) }
            .mapNotNull { getDisplayedFindings().getOrNull(it)?.let { finding -> toIssueFinding(finding) } }
        if (jiraFindings.isEmpty()) return
        CreateJiraIssueDialog(project, jiraFindings).show()
    }
}
