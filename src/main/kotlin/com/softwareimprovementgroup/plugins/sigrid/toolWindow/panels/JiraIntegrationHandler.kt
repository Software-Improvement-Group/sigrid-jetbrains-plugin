package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.table.JBTable
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.JiraFinding
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import javax.swing.JButton

class JiraIntegrationHandler<T>(
    private val project: Project,
    private val table: JBTable,
    private val getDisplayedFindings: () -> List<T>,
    private val toJiraFinding: (T) -> JiraFinding,
) {
    val button = JButton(SigridBundle["jira.create.button"]).apply {
        isEnabled = false
        isFocusable = false
        addActionListener { openCreateJiraIssueDialog() }
    }

    fun updateButtonState() {
        ApplicationManager.getApplication().invokeLater {
            val isJiraConfigured = SigridProjectConfiguration.getInstance(project).isJiraConfigured
            button.isEnabled = table.selectedRows.isNotEmpty() && isJiraConfigured
            button.toolTipText = if (isJiraConfigured)
                SigridBundle["jira.create.button.tooltip"]
            else
                SigridBundle["jira.create.button.tooltip.not.configured"]
        }
    }

    fun openCreateJiraIssueDialog() {
        val jiraFindings = table.selectedRows
            .map { table.convertRowIndexToModel(it) }
            .mapNotNull { getDisplayedFindings().getOrNull(it)?.let { finding -> toJiraFinding(finding) } }
        if (jiraFindings.isEmpty()) return
        CreateJiraIssueDialog(project, jiraFindings).show()
    }
}
