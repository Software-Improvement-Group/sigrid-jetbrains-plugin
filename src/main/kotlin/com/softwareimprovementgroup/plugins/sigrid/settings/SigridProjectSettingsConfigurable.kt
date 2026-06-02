package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridConfiguration
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import javax.swing.JComponent
import javax.swing.JPasswordField

class SigridProjectSettingsConfigurable(private val project: Project) : Configurable {
    private var panel: DialogPanel? = null
    private val apiKeyOverrideField = JPasswordField(1)
    private val jiraTokenOverrideField = JPasswordField(1)
    private var customerOverride = ""
    private var sigridUrlOverride = ""
    private var system = ""
    private var subsystem = ""
    private var jiraBaseUrl = ""
    private var jiraUserOverride = ""
    private var jiraProjectKey = ""

    override fun getDisplayName() = SigridBundle["settings.project.display.name"]

    override fun createComponent(): JComponent {
        reset()
        val global = SigridConfiguration.getInstance()
        panel = panel {
            group(SigridBundle["settings.group.general"]) {
                row(SigridBundle["settings.api.key.label"]) {
                    cell(apiKeyOverrideField).align(AlignX.FILL)
                        .comment(SigridBundle["settings.project.api.key.comment"])
                }
                row(SigridBundle["settings.customer.label"]) {
                    textField().bindText(::customerOverride).align(AlignX.FILL)
                        .comment(SigridBundle["settings.project.override.comment", global.customer.ifBlank { SigridBundle["settings.project.not.set"] }])
                }
                row(SigridBundle["settings.sigrid.url.label"]) {
                    textField().bindText(::sigridUrlOverride).align(AlignX.FILL)
                        .comment(SigridBundle["settings.project.override.comment", global.sigridUrl.ifBlank { SigridConfiguration.SIGRID_DEFAULT_URL }])
                }
            }
            group(SigridBundle["settings.project.system.group"]) {
                row(SigridBundle["settings.project.system.label"]) {
                    textField().bindText(::system).align(AlignX.FILL)
                }
                row(SigridBundle["settings.project.subsystem.label"]) {
                    textField().bindText(::subsystem).align(AlignX.FILL)
                        .comment(SigridBundle["settings.project.subsystem.comment"])
                }
            }
            // TODO: Uncomment when Jira integration is implemented
            /*group(SigridBundle["settings.group.jira"]) {
                row(SigridBundle["settings.project.jira.base.url.label"]) {
                    textField().bindText(::jiraBaseUrl).align(AlignX.FILL)
                }
                row(SigridBundle["settings.project.jira.user.label"]) {
                    textField().bindText(::jiraUserOverride).align(AlignX.FILL)
                        .comment(SigridBundle["settings.project.override.comment", global.jiraUser.ifBlank { SigridBundle["settings.project.not.set"] }])
                }
                row(SigridBundle["settings.project.jira.token.label"]) {
                    cell(jiraTokenOverrideField).align(AlignX.FILL)
                        .comment(SigridBundle["settings.project.jira.token.override.comment"])
                }
                row(SigridBundle["settings.project.jira.project.key.label"]) {
                    textField().bindText(::jiraProjectKey).align(AlignX.FILL).enabled(false)
                }
            }*/
        }
        return panel!!
    }

    override fun isModified(): Boolean {
        panel?.apply()
        val config = SigridProjectConfiguration.getInstance(project)
        return String(apiKeyOverrideField.password) != config.apiKeyOverride ||
                customerOverride != config.customerOverride ||
                sigridUrlOverride != config.sigridUrlOverride ||
                system != config.system ||
                subsystem != config.subsystem ||
                jiraBaseUrl != config.jiraBaseUrl ||
                jiraUserOverride != config.jiraUserOverride ||
                String(jiraTokenOverrideField.password) != config.jiraTokenOverride ||
                jiraProjectKey != config.jiraProjectKey
    }

    override fun apply() {
        panel?.apply()
        val config = SigridProjectConfiguration.getInstance(project)
        config.apiKeyOverride = String(apiKeyOverrideField.password)
        config.customerOverride = customerOverride
        config.sigridUrlOverride = sigridUrlOverride
        config.system = system
        config.subsystem = subsystem
        config.jiraBaseUrl = jiraBaseUrl
        config.jiraUserOverride = jiraUserOverride
        config.jiraTokenOverride = String(jiraTokenOverrideField.password)
        config.jiraProjectKey = jiraProjectKey
    }

    override fun reset() {
        val config = SigridProjectConfiguration.getInstance(project)
        apiKeyOverrideField.text = config.apiKeyOverride
        customerOverride = config.customerOverride
        sigridUrlOverride = config.sigridUrlOverride
        system = config.system
        subsystem = config.subsystem
        jiraBaseUrl = config.jiraBaseUrl
        jiraUserOverride = config.jiraUserOverride
        jiraTokenOverrideField.text = config.jiraTokenOverride
        jiraProjectKey = config.jiraProjectKey
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}