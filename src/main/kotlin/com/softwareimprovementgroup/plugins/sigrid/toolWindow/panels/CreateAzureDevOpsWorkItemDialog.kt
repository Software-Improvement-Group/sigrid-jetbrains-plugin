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
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import com.softwareimprovementgroup.plugins.sigrid.services.AzureDevOpsApiService
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import com.intellij.openapi.ui.ComboBox
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private const val NOTIFICATION_GROUP_ID = "Sigrid"

class CreateAzureDevOpsWorkItemDialog(
    private val project: Project,
    private val findings: List<IssueFinding>,
) : DialogWrapper(project, true) {

    private val config = SigridProjectConfiguration.getInstance(project)
    private val sigridUrl = "${config.effectiveSigridUrl}/${config.effectiveCustomer}/${config.system}"

    private val titleField = JBTextField()
    private val typeStatusLabel = JBLabel(SigridBundle["azuredevops.create.dialog.type.loading"]).apply {
        foreground = JBColor.GRAY
    }
    private val typeCombo = ComboBox<String>().apply { isVisible = false }
    private val previewPane = JEditorPane().apply {
        editorKit = HTMLEditorKitBuilder().build()
        isEditable = false
        text = AzureDevOpsApiService.buildPreviewHtml(findings, sigridUrl)
        caretPosition = 0
        background = null
    }
    private val errorLabel = JBLabel("").apply {
        foreground = JBColor.RED
        isVisible = false
    }

    @Volatile private var typesLoaded = false

    init {
        title = SigridBundle["azuredevops.create.dialog.title"]
        setOKActionEnabled(false)
        init()
        loadWorkItemTypes()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(4)
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(JBLabel(SigridBundle["azuredevops.create.dialog.findings.count", findings.size]), gbc)

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE
        panel.add(JBLabel(SigridBundle["azuredevops.create.dialog.title.label"]), gbc)

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        titleField.preferredSize = java.awt.Dimension(400, titleField.preferredSize.height)
        panel.add(titleField, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE
        panel.add(JBLabel(SigridBundle["azuredevops.create.dialog.type.label"]), gbc)

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(typeStatusLabel, gbc)
        panel.add(typeCombo, gbc)

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.weighty = 0.0
        panel.add(JBLabel(SigridBundle["azuredevops.create.dialog.preview.label"]), gbc)

        gbc.gridy = 4; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0
        val scrollPane = JBScrollPane(previewPane).apply { preferredSize = java.awt.Dimension(400, 160) }
        panel.add(scrollPane, gbc)

        gbc.gridy = 5; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0.0
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
        isOKActionEnabled = titleField.text.isNotBlank() && typesLoaded
    }

    private fun loadWorkItemTypes() {
        val orgUrl = config.effectiveAzureDevOpsOrganizationUrl
        val pat = config.effectiveAzureDevOpsPat
        val projectName = config.azureDevOpsProjectName
        AppExecutorUtil.getAppExecutorService().submit {
            try {
                val types = AzureDevOpsApiService.getInstance().getWorkItemTypes(orgUrl, projectName, pat)
                ApplicationManager.getApplication().invokeLater({ populateWorkItemTypes(types) }, ModalityState.any())
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater({ showTypeLoadError(e.message ?: "") }, ModalityState.any())
            }
        }
    }

    private fun populateWorkItemTypes(types: List<String>) {
        typeStatusLabel.isVisible = false
        types.forEach { typeCombo.addItem(it) }
        val lastType = config.azureDevOpsLastWorkItemType
        val typeToSelect = types.firstOrNull { it == lastType } ?: types.firstOrNull()
        if (typeToSelect != null) typeCombo.selectedItem = typeToSelect
        typeCombo.isVisible = true
        typesLoaded = true
        updateOkButton()
    }

    private fun showTypeLoadError(message: String) {
        typeStatusLabel.text = SigridBundle["azuredevops.create.dialog.type.error.short"]
        typeStatusLabel.foreground = JBColor.RED
        errorLabel.text = SigridBundle["azuredevops.create.dialog.type.error", message]
        errorLabel.isVisible = true
    }

    override fun doOKAction() {
        val summary = titleField.text.trim()
        val workItemType = typeCombo.selectedItem as? String ?: return
        errorLabel.isVisible = false
        isOKActionEnabled = false
        val modality = ModalityState.current()
        AppExecutorUtil.getAppExecutorService().submit {
            try {
                val result = AzureDevOpsApiService.getInstance().createWorkItem(
                    organizationUrl = config.effectiveAzureDevOpsOrganizationUrl,
                    projectName = config.azureDevOpsProjectName,
                    pat = config.effectiveAzureDevOpsPat,
                    workItemType = workItemType,
                    title = summary,
                    findings = findings,
                    sigridUrl = sigridUrl,
                )
                ApplicationManager.getApplication().invokeLater({
                    config.azureDevOpsLastWorkItemType = workItemType
                    close(OK_EXIT_CODE)
                    showSuccessNotification(result)
                }, modality)
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater({
                    isOKActionEnabled = true
                    errorLabel.text = SigridBundle["azuredevops.create.error", e.message ?: ""]
                    errorLabel.isVisible = true
                    pack()
                }, modality)
            }
        }
    }

    private fun showSuccessNotification(result: com.softwareimprovementgroup.plugins.sigrid.services.AzureDevOpsWorkItemResult) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(SigridBundle["azuredevops.create.success", result.id], NotificationType.INFORMATION)
        val browserUrl = result.browserUrl
        if (browserUrl != null) {
            notification.addAction(NotificationAction.createSimple(SigridBundle["azuredevops.create.open"]) {
                BrowserUtil.browse(browserUrl)
            })
        }
        notification.notify(project)
    }
}
