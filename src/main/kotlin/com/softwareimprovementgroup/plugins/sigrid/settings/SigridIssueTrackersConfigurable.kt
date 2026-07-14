package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.openapi.options.Configurable
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import javax.swing.JComponent

class SigridIssueTrackersConfigurable : Configurable {
    override fun getDisplayName() = SigridBundle["settings.integrations.display.name"]
    override fun createComponent(): JComponent? = null
    override fun isModified() = false
    override fun apply() = Unit
}
