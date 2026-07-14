package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import javax.swing.JComponent
import javax.swing.JPasswordField

class SigridAzureDevOpsProjectSettingsConfigurable(private val project: Project) : Configurable {
    private var panel: DialogPanel? = null
    private val patOverrideField = JPasswordField(1)
    private var projectName = ""
    private var organizationUrlOverride = ""

    override fun getDisplayName() = SigridBundle["settings.azure.devops.project.display.name"]

    override fun createComponent(): JComponent {
        reset()
        panel = panel {
            row(SigridBundle["settings.azure.devops.project.name.label"]) {
                textField().bindText(::projectName).align(AlignX.FILL)
                    .comment(SigridBundle["settings.azure.devops.project.name.comment"])
            }
            row(SigridBundle["settings.azure.devops.org.url.override.label"]) {
                textField().bindText(::organizationUrlOverride).align(AlignX.FILL)
                    .comment(SigridBundle["settings.azure.devops.org.url.override.comment"])
            }
            row(SigridBundle["settings.azure.devops.pat.override.label"]) {
                cell(patOverrideField).align(AlignX.FILL)
                    .comment(SigridBundle["settings.azure.devops.pat.override.comment"])
            }
        }
        return panel!!
    }

    override fun isModified(): Boolean {
        panel?.apply()
        val config = SigridProjectConfiguration.getInstance(project)
        return projectName != config.azureDevOpsProjectName ||
                organizationUrlOverride != config.azureDevOpsOrganizationUrlOverride ||
                String(patOverrideField.password) != config.azureDevOpsPatOverride
    }

    override fun apply() {
        panel?.apply()
        val urlOverride = organizationUrlOverride.trim()
        if (urlOverride.isNotBlank() && !urlOverride.startsWith("https://")) {
            throw ConfigurationException(SigridBundle["settings.azure.devops.url.https.only"])
        }
        val patOverride = String(patOverrideField.password)
        if (urlOverride.isNotBlank() && patOverride.isBlank()) {
            throw ConfigurationException(SigridBundle["settings.azure.devops.url.override.requires.pat"])
        }
        val config = SigridProjectConfiguration.getInstance(project)
        config.azureDevOpsProjectName = projectName.trim()
        config.azureDevOpsOrganizationUrlOverride = urlOverride
        config.azureDevOpsPatOverride = patOverride
        ApplicationManager.getApplication().invokeLater(
            { project.messageBus.syncPublisher(SigridSettingsTopic.PROJECT).settingsChanged() },
            ModalityState.nonModal()
        )
    }

    override fun reset() {
        val config = SigridProjectConfiguration.getInstance(project)
        projectName = config.azureDevOpsProjectName
        organizationUrlOverride = config.azureDevOpsOrganizationUrlOverride
        patOverrideField.text = config.azureDevOpsPatOverride
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}
