package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBOptionButton
import com.intellij.ui.table.JBTable
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridIssueTrackersConfigurable
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

private const val TRACKER_JIRA = "jira"
private const val TRACKER_AZURE = "azuredevops"

class CreateIssueButton<T>(
    private val project: Project,
    private val jiraHandler: JiraIntegrationHandler<T>,
    private val azureDevOpsHandler: AzureDevOpsIntegrationHandler<T>,
    private val table: JBTable,
) {
    private val jiraOption = createTrackerOption(SigridBundle["jira.create.button"], TRACKER_JIRA) {
        jiraHandler.openCreateJiraIssueDialog()
    }

    private val azureOption = createTrackerOption(SigridBundle["azuredevops.create.button"], TRACKER_AZURE) {
        azureDevOpsHandler.openCreateAzureDevOpsWorkItemDialog()
    }

    private val settingsOption = object : AnAction(SigridBundle["create.issue.settings"]) {
        override fun actionPerformed(e: AnActionEvent) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, SigridIssueTrackersConfigurable::class.java)
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    val button = JBOptionButton(createMainAction(), emptyArray()).also { btn ->
        btn.isFocusable = false
        btn.addSeparator = false
        btn.setOptions(listOf(jiraOption, azureOption, Separator.getInstance(), settingsOption))
    }

    fun updateButtonState() {
        ApplicationManager.getApplication().invokeLater {
            val config = SigridProjectConfiguration.getInstance(project)
            val hasSelection = table.selectedRows.isNotEmpty()
            button.isEnabled = hasSelection && resolveActiveTracker(config) != null
            button.toolTipText = buildTooltip(config, hasSelection)
        }
    }

    private fun createMainAction(): Action = object : AbstractAction(SigridBundle["create.issue.button"]) {
        override fun actionPerformed(e: ActionEvent) {
            val config = SigridProjectConfiguration.getInstance(project)
            when (resolveActiveTracker(config)) {
                TRACKER_JIRA -> jiraHandler.openCreateJiraIssueDialog()
                TRACKER_AZURE -> azureDevOpsHandler.openCreateAzureDevOpsWorkItemDialog()
            }
        }
    }

    private fun createTrackerOption(label: String, trackerType: String, open: () -> Unit): AnAction =
        object : AnAction(label) {
            override fun actionPerformed(e: AnActionEvent) {
                SigridProjectConfiguration.getInstance(project).lastIssueCreationAction = trackerType
                open()
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
            override fun update(e: AnActionEvent) {
                val config = SigridProjectConfiguration.getInstance(project)
                e.presentation.isEnabled = table.selectedRows.isNotEmpty() && when (trackerType) {
                    TRACKER_JIRA -> config.isJiraConfigured
                    else -> config.isAzureDevOpsConfigured
                }
            }
        }

    private fun resolveActiveTracker(config: SigridProjectConfiguration): String? {
        val last = config.lastIssueCreationAction
        return when {
            last == TRACKER_JIRA && config.isJiraConfigured -> TRACKER_JIRA
            last == TRACKER_AZURE && config.isAzureDevOpsConfigured -> TRACKER_AZURE
            config.isJiraConfigured -> TRACKER_JIRA
            config.isAzureDevOpsConfigured -> TRACKER_AZURE
            else -> null
        }
    }

    private fun buildTooltip(config: SigridProjectConfiguration, hasSelection: Boolean): String? {
        if (!hasSelection) return null
        return when {
            config.isAzureDevOpsUrlOverrideWithoutPatOverride ->
                SigridBundle["azuredevops.create.button.tooltip.url.override.no.pat"]
            resolveActiveTracker(config) == null ->
                SigridBundle["jira.create.button.tooltip.not.configured"]
            else -> SigridBundle["create.issue.button.tooltip"]
        }
    }
}
