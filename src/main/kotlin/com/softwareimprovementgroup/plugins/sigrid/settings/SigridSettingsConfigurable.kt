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
    private val apiKeyField = JPasswordField()
    private var customer = ""
    private var sigridUrl = ""

    override fun getDisplayName() = SigridBundle["settings.display.name"]

    override fun createComponent(): JComponent {
        reset()
        panel = panel {
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