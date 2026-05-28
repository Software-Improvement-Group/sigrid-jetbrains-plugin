package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout

class OpenSourceHealthPanel : JBPanel<OpenSourceHealthPanel>(BorderLayout()) {

    init {
        val label = JBLabel("Open source health findings will appear here.")
        label.horizontalAlignment = JBLabel.CENTER
        add(label, BorderLayout.CENTER)
    }
}