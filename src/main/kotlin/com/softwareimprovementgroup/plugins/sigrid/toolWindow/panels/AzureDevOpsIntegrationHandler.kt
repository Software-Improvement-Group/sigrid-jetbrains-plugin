package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.table.JBTable
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import javax.swing.JButton

class AzureDevOpsIntegrationHandler<T>(
    private val project: Project,
    private val table: JBTable,
    private val getDisplayedFindings: () -> List<T>,
    private val toIssueFinding: (T) -> IssueFinding,
) {
    val button = JButton(SigridBundle["azuredevops.create.button"]).apply {
        isEnabled = false
        isFocusable = false
        addActionListener { openCreateAzureDevOpsWorkItemDialog() }
    }

    fun updateButtonState() {
        ApplicationManager.getApplication().invokeLater {
            val config = SigridProjectConfiguration.getInstance(project)
            button.isEnabled = table.selectedRows.isNotEmpty() && config.isAzureDevOpsConfigured
            button.toolTipText = when {
                config.isAzureDevOpsUrlOverrideWithoutPatOverride ->
                    SigridBundle["azuredevops.create.button.tooltip.url.override.no.pat"]
                !config.isAzureDevOpsConfigured ->
                    SigridBundle["azuredevops.create.button.tooltip.not.configured"]
                else ->
                    SigridBundle["azuredevops.create.button.tooltip"]
            }
        }
    }

    fun openCreateAzureDevOpsWorkItemDialog() {
        val findings = table.selectedRows
            .map { table.convertRowIndexToModel(it) }
            .mapNotNull { getDisplayedFindings().getOrNull(it)?.let { finding -> toIssueFinding(finding) } }
        if (findings.isEmpty()) return
        CreateAzureDevOpsWorkItemDialog(project, findings).show()
    }
}
