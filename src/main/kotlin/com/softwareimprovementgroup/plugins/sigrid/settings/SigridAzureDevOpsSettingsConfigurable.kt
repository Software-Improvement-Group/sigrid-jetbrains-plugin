package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridConfiguration
import javax.swing.JComponent
import javax.swing.JPasswordField

class SigridAzureDevOpsSettingsConfigurable : Configurable {
    private var panel: DialogPanel? = null
    private val patField = JPasswordField(1)
    private var organizationUrl = ""

    override fun getDisplayName() = SigridBundle["settings.azure.devops.display.name"]

    override fun createComponent(): JComponent {
        reset()
        panel = panel {
            group(SigridBundle["settings.group.global"]) {
                row(SigridBundle["settings.azure.devops.org.url.label"]) {
                    textField().bindText(::organizationUrl).align(AlignX.FILL)
                        .comment(SigridBundle["settings.azure.devops.org.url.comment"])
                }
                row(SigridBundle["settings.azure.devops.pat.label"]) {
                    cell(patField).align(AlignX.FILL)
                        .comment(SigridBundle["settings.azure.devops.pat.comment"])
                }
            }
        }
        return panel!!
    }

    override fun isModified(): Boolean {
        panel?.apply()
        val config = SigridConfiguration.getInstance()
        return organizationUrl != config.azureDevOpsOrganizationUrl ||
                String(patField.password) != config.azureDevOpsPat
    }

    override fun apply() {
        panel?.apply()
        val url = organizationUrl.trim()
        if (url.isNotBlank() && !url.startsWith("https://")) {
            throw ConfigurationException(SigridBundle["settings.azure.devops.url.https.only"])
        }
        val config = SigridConfiguration.getInstance()
        config.azureDevOpsOrganizationUrl = url
        config.azureDevOpsPat = String(patField.password)
        ApplicationManager.getApplication().invokeLater(
            { ApplicationManager.getApplication().messageBus.syncPublisher(SigridSettingsTopic.GLOBAL).settingsChanged() },
            ModalityState.nonModal()
        )
    }

    override fun reset() {
        val config = SigridConfiguration.getInstance()
        organizationUrl = config.azureDevOpsOrganizationUrl
        patField.text = config.azureDevOpsPat
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}
