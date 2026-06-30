package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import javax.swing.JComponent
import javax.swing.JPasswordField

class SigridJiraSettingsConfigurable(private val project: Project) : Configurable {
    private var panel: DialogPanel? = null
    private val jiraTokenField = JPasswordField(1)
    private var jiraBaseUrl = ""
    private var jiraUser = ""
    private var jiraProjectKey = ""

    override fun getDisplayName() = SigridBundle["settings.jira.display.name"]

    override fun createComponent(): JComponent {
        reset()
        panel = panel {
            row(SigridBundle["settings.project.jira.base.url.label"]) {
                textField().bindText(::jiraBaseUrl).align(AlignX.FILL)
                    .comment(SigridBundle["settings.jira.base.url.comment"])
            }
            row(SigridBundle["settings.project.jira.user.label"]) {
                textField().bindText(::jiraUser).align(AlignX.FILL)
                    .comment(SigridBundle["settings.jira.user.comment"])
            }
            row(SigridBundle["settings.project.jira.token.label"]) {
                cell(jiraTokenField).align(AlignX.FILL)
                    .comment(SigridBundle["settings.jira.token.comment"])
            }
            row(SigridBundle["settings.project.jira.project.key.label"]) {
                textField().bindText(::jiraProjectKey).align(AlignX.FILL)
                    .comment(SigridBundle["settings.jira.project.key.comment"])
            }
        }
        return panel!!
    }

    override fun isModified(): Boolean {
        panel?.apply()
        val config = SigridProjectConfiguration.getInstance(project)
        return jiraBaseUrl != config.jiraBaseUrl ||
                jiraUser != config.jiraUser ||
                String(jiraTokenField.password) != config.jiraToken ||
                jiraProjectKey != config.jiraProjectKey
    }

    override fun apply() {
        panel?.apply()
        val raw = jiraBaseUrl.trim()
        if (raw.contains("://") && !raw.startsWith("https://")) {
            throw ConfigurationException(SigridBundle["settings.jira.base.url.https.only"])
        }
        val config = SigridProjectConfiguration.getInstance(project)
        config.jiraBaseUrl = normalizeUrl(raw)
        config.jiraUser = jiraUser.trim()
        config.jiraToken = String(jiraTokenField.password)
        config.jiraProjectKey = jiraProjectKey.trim()
        ApplicationManager.getApplication().invokeLater(
            { project.messageBus.syncPublisher(SigridSettingsTopic.PROJECT).settingsChanged() },
            com.intellij.openapi.application.ModalityState.nonModal()
        )
    }

    override fun reset() {
        val config = SigridProjectConfiguration.getInstance(project)
        jiraBaseUrl = config.jiraBaseUrl
        jiraUser = config.jiraUser
        jiraTokenField.text = config.jiraToken
        jiraProjectKey = config.jiraProjectKey
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}

private fun normalizeUrl(url: String): String {
    if (url.isEmpty()) return url
    return if (url.contains("://")) url else "https://$url"
}
