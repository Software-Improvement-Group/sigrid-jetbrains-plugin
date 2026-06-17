package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridSettingsConfigurable
import java.awt.FlowLayout
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

internal fun buildNotConfiguredCard(project: Project): JPanel {
    val prefix = JBLabel("${SigridBundle["panel.not.configured.prefix"]} ").apply {
        foreground = JBColor.RED
    }
    val link = HyperlinkLabel(SigridBundle["panel.not.configured.link"]).apply {
        foreground = JBColor.BLUE
        addHyperlinkListener { e ->
            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, SigridSettingsConfigurable::class.java)
            }
        }
    }
    val suffix = JBLabel(".").apply {
        foreground = JBColor.RED
    }
    val row = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
        add(prefix)
        add(link)
        add(suffix)
    }
    return JPanel(GridBagLayout()).apply { add(row) }
}