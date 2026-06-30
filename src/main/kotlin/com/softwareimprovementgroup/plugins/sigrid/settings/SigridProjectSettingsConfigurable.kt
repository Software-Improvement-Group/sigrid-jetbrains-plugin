package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridConfiguration
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import javax.swing.JComponent
import javax.swing.JPasswordField

class SigridProjectSettingsConfigurable(private val project: Project) : Configurable {
    private var panel: DialogPanel? = null
    private val apiKeyOverrideField = JPasswordField(1)
    private val effectiveHostLabel = JBLabel()
    private var customerOverride = ""
    private var sigridUrlOverride = ""
    private var system = ""
    private var subsystem = ""

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
                row(SigridBundle["settings.project.effective.host.label"]) {
                    cell(effectiveHostLabel)
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
                subsystem != config.subsystem
    }

    override fun apply() {
        panel?.apply()
        val urlOverride = sigridUrlOverride.trim()
        if (urlOverride.isNotBlank() && !urlOverride.startsWith("https://")) {
            throw ConfigurationException(SigridBundle["settings.error.url.must.be.https"])
        }
        val customerOverrideValue = customerOverride.trim()
        if (customerOverrideValue.isNotBlank() && !customerOverrideValue.matches(CUSTOMER_NAME_REGEX)) {
            throw ConfigurationException(SigridBundle["settings.error.customer.invalid.format"])
        }
        val systemValue = system.trim()
        if (systemValue.isBlank()) {
            throw ConfigurationException(SigridBundle["settings.error.system.required"])
        }
        if (!systemValue.matches(SYSTEM_NAME_REGEX)) {
            throw ConfigurationException(SigridBundle["settings.error.system.invalid.format"])
        }
        val subsystemValue = subsystem.trim()
        if (subsystemValue.isNotBlank() && !subsystemValue.matches(SUBSYSTEM_NAME_REGEX)) {
            throw ConfigurationException(SigridBundle["settings.error.subsystem.invalid.format"])
        }
        val config = SigridProjectConfiguration.getInstance(project)
        config.apiKeyOverride = String(apiKeyOverrideField.password)
        config.customerOverride = customerOverrideValue
        config.sigridUrlOverride = urlOverride
        config.system = systemValue
        config.subsystem = subsystemValue
        ApplicationManager.getApplication().invokeLater(
            { project.messageBus.syncPublisher(SigridSettingsTopic.PROJECT).settingsChanged() },
            com.intellij.openapi.application.ModalityState.nonModal()
        )
    }

    override fun reset() {
        val config = SigridProjectConfiguration.getInstance(project)
        apiKeyOverrideField.text = config.apiKeyOverride
        customerOverride = config.customerOverride
        sigridUrlOverride = config.sigridUrlOverride
        system = config.system
        subsystem = config.subsystem
        val global = SigridConfiguration.getInstance()
        effectiveHostLabel.text = config.sigridUrlOverride.trimEnd('/').ifBlank {
            global.sigridUrl.ifBlank { SigridConfiguration.SIGRID_DEFAULT_URL }
        }
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}