package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.softwareimprovementgroup.plugins.sigrid.services.SigridConfiguration
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import javax.swing.JComponent
import javax.swing.JPasswordField

class SigridProjectSettingsConfigurable(private val project: Project) : Configurable {
    private var panel: DialogPanel? = null
    private val apiKeyOverrideField = JPasswordField()
    private var customerOverride = ""
    private var sigridUrlOverride = ""
    private var system = ""
    private var subsystem = ""
    private var jiraBaseUrl = ""
    private var jiraUser = ""
    private var jiraToken = ""
    private var jiraProjectKey = ""

    override fun getDisplayName() = "Project"

    override fun createComponent(): JComponent {
        reset()
        val global = SigridConfiguration.getInstance()
        panel = panel {
            group("Overrides") {
                row("API Key:") {
                    cell(apiKeyOverrideField).align(AlignX.FILL)
                        .comment("Leave blank to use the global API key")
                }
                row("Customer:") {
                    textField().bindText(::customerOverride).align(AlignX.FILL)
                        .comment("Leave blank to use global: <i>${global.customer.ifBlank { "not set" }}</i>")
                }
                row("Sigrid URL:") {
                    textField().bindText(::sigridUrlOverride).align(AlignX.FILL)
                        .comment("Leave blank to use global: <i>${global.sigridUrl.ifBlank { SigridConfiguration.SIGRID_DEFAULT_URL }}</i>")
                }
            }
            group("System") {
                row("System:") {
                    textField().bindText(::system).align(AlignX.FILL)
                }
                row("Subsystem:") {
                    textField().bindText(::subsystem).align(AlignX.FILL)
                        .comment("Optional — leave blank to include all subsystems")
                }
            }
            group("Jira Integration (Optional)") {
                row("Base URL:") {
                    textField().bindText(::jiraBaseUrl).align(AlignX.FILL)
                }
                row("User:") {
                    textField().bindText(::jiraUser).align(AlignX.FILL)
                }
                row("Token:") {
                    textField().bindText(::jiraToken).align(AlignX.FILL)
                }
                row("Project Key:") {
                    textField().bindText(::jiraProjectKey).align(AlignX.FILL)
                }
            }
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
                jiraUser != config.jiraUser ||
                jiraToken != config.jiraToken ||
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
        config.jiraUser = jiraUser
        config.jiraToken = jiraToken
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
        jiraUser = config.jiraUser
        jiraToken = config.jiraToken
        jiraProjectKey = config.jiraProjectKey
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}