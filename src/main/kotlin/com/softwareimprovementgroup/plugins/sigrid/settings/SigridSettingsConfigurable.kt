package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridConfiguration
import javax.swing.JComponent
import javax.swing.JPasswordField

class SigridSettingsConfigurable : Configurable {
    private var panel: DialogPanel? = null
    private val apiKeyField = JPasswordField(1)
    private val jiraTokenField = JPasswordField(1)
    private var customer = ""
    private var sigridUrl = ""
    private var jiraUser = ""

    override fun getDisplayName() = SigridBundle["settings.display.name"]

    override fun createComponent(): JComponent {
        reset()
        panel = panel {
            group(SigridBundle["settings.group.general"]) {
                SigridBundle["settings.global.group"]
                    row(SigridBundle["settings.api.key.label"]) {
                        cell(apiKeyField).align(AlignX.FILL)
                            .comment(SigridBundle["settings.api.key.comment"])
                    }
                    row(SigridBundle["settings.customer.label"]) {
                        textField().bindText(::customer).align(AlignX.FILL)
                            .comment(SigridBundle["settings.global.customer.comment"])
                    }
                    row(SigridBundle["settings.sigrid.url.label"]) {
                        textField().bindText(::sigridUrl).align(AlignX.FILL)
                            .comment(SigridBundle["settings.global.sigrid.url.comment", SigridConfiguration.SIGRID_DEFAULT_URL])
                    }
            }
            // TODO: Uncomment when Jira integration is implemented
            /*group(SigridBundle["settings.group.jira"]) {
                row(SigridBundle["settings.project.jira.user.label"]) {
                    textField().bindText(::jiraUser).align(AlignX.FILL)
                        .comment(SigridBundle["settings.jira.user.comment"])
                }
                row(SigridBundle["settings.project.jira.token.label"]) {
                    cell(jiraTokenField).align(AlignX.FILL)
                        .comment(SigridBundle["settings.jira.token.comment"])
                }
            }*/
        }
        return panel!!
    }

    override fun isModified(): Boolean {
        panel?.apply()
        val config = SigridConfiguration.getInstance()
        return String(apiKeyField.password) != config.apiKey ||
                customer != config.customer ||
                sigridUrl != config.sigridUrl ||
                jiraUser != config.jiraUser ||
                String(jiraTokenField.password) != config.jiraToken
    }

    override fun apply() {
        panel?.apply()
        val config = SigridConfiguration.getInstance()
        config.apiKey = String(apiKeyField.password)
        config.customer = customer
        config.sigridUrl = sigridUrl
        config.jiraUser = jiraUser
        config.jiraToken = String(jiraTokenField.password)
    }

    override fun reset() {
        val config = SigridConfiguration.getInstance()
        apiKeyField.text = config.apiKey
        customer = config.customer
        sigridUrl = config.sigridUrl
        jiraUser = config.jiraUser
        jiraTokenField.text = config.jiraToken
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}