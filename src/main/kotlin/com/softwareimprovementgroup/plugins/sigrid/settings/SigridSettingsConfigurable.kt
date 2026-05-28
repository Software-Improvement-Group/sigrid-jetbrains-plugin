package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.softwareimprovementgroup.plugins.sigrid.services.SigridConfiguration
import javax.swing.JComponent
import javax.swing.JPasswordField

class SigridSettingsConfigurable : Configurable {
    private var panel: DialogPanel? = null
    private val apiKeyField = JPasswordField()
    private var customer = ""
    private var sigridUrl = ""

    override fun getDisplayName() = "Sigrid"

    override fun createComponent(): JComponent {
        reset()
        panel = panel {
            row("API Key:") {
                cell(apiKeyField).align(AlignX.FILL)
                    .comment("Your Sigrid personal access token")
            }
            row("Customer:") {
                textField().bindText(::customer).align(AlignX.FILL)
                    .comment("Default customer used across all projects; can be overridden per workspace")
            }
            row("Sigrid URL:") {
                textField().bindText(::sigridUrl).align(AlignX.FILL)
                    .comment("Default: ${SigridConfiguration.SIGRID_DEFAULT_URL}; can be overridden per workspace")
            }
        }
        return panel!!
    }

    override fun isModified(): Boolean {
        panel?.apply()
        val config = SigridConfiguration.getInstance()
        return String(apiKeyField.password) != config.apiKey ||
                customer != config.customer ||
                sigridUrl != config.sigridUrl
    }

    override fun apply() {
        panel?.apply()
        val config = SigridConfiguration.getInstance()
        config.apiKey = String(apiKeyField.password)
        config.customer = customer
        config.sigridUrl = sigridUrl
    }

    override fun reset() {
        val config = SigridConfiguration.getInstance()
        apiKeyField.text = config.apiKey
        customer = config.customer
        sigridUrl = config.sigridUrl
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}