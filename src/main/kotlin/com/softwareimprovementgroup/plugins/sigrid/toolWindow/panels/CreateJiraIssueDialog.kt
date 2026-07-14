package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.HTMLEditorKitBuilder
import javax.swing.JEditorPane
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import com.softwareimprovementgroup.plugins.sigrid.services.JiraApiService
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private const val NOTIFICATION_GROUP_ID = "Sigrid"

class CreateJiraIssueDialog(
    private val project: Project,
    private val findings: List<IssueFinding>,
) : DialogWrapper(project, true) {

    private val titleField = JBTextField()
    private val previewPane = JEditorPane().apply {
        editorKit = HTMLEditorKitBuilder().build()
        isEditable = false
        text = JiraApiService.buildPreviewHtml(findings)
        caretPosition = 0
        background = null
    }
    private val errorLabel = JBLabel("").apply {
        foreground = JBColor.RED
        isVisible = false
    }

    init {
        title = SigridBundle["jira.create.dialog.title"]
        isOKActionEnabled = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(4)
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(JBLabel(SigridBundle["jira.create.dialog.findings.count", findings.size]), gbc)

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE
        panel.add(JBLabel(SigridBundle["jira.create.dialog.title.label"]), gbc)

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        titleField.preferredSize = java.awt.Dimension(400, titleField.preferredSize.height)
        panel.add(titleField, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.weighty = 0.0
        panel.add(JBLabel(SigridBundle["jira.create.dialog.preview.label"]), gbc)

        gbc.gridy = 3; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0
        val scrollPane = JBScrollPane(previewPane).apply { preferredSize = java.awt.Dimension(400, 260) }
        panel.add(scrollPane, gbc)

        gbc.gridy = 4; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0.0
        panel.add(errorLabel, gbc)

        titleField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateOkButton()
            override fun removeUpdate(e: DocumentEvent) = updateOkButton()
            override fun changedUpdate(e: DocumentEvent) = updateOkButton()
        })

        return panel
    }

    override fun getPreferredFocusedComponent() = titleField

    private fun updateOkButton() {
        setOKActionEnabled(titleField.text.isNotBlank())
    }

    override fun doOKAction() {
        val summary = titleField.text.trim()
        errorLabel.isVisible = false
        setOKActionEnabled(false)
        val modality = ModalityState.current()
        val config = SigridProjectConfiguration.getInstance(project)
        AppExecutorUtil.getAppExecutorService().submit {
            try {
                val issueKey = JiraApiService.getInstance().createIssue(
                    jiraBaseUrl = config.jiraBaseUrl,
                    jiraUser = config.jiraUser,
                    jiraToken = config.jiraToken,
                    jiraProjectKey = config.jiraProjectKey,
                    summary = summary,
                    findings = findings,
                )
                ApplicationManager.getApplication().invokeLater({
                    close(OK_EXIT_CODE)
                    val issueUrl = "${config.jiraBaseUrl}/browse/$issueKey"
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup(NOTIFICATION_GROUP_ID)
                        .createNotification(SigridBundle["jira.create.success", issueKey], NotificationType.INFORMATION)
                        .addAction(NotificationAction.createSimple(SigridBundle["jira.create.open"]) {
                            BrowserUtil.browse(issueUrl)
                        })
                        .notify(project)
                }, modality)
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater({
                    setOKActionEnabled(true)
                    errorLabel.text = SigridBundle["jira.create.error", e.message ?: ""]
                    errorLabel.isVisible = true
                    pack()
                }, modality)
            }
        }
    }
}
