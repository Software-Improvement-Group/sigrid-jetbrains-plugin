package com.softwareimprovementgroup.plugins.sigrid.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.softwareimprovementgroup.plugins.sigrid.MyBundle
import com.softwareimprovementgroup.plugins.sigrid.services.MyProjectService
import javax.swing.JButton

class SigridWindow(toolWindow: ToolWindow) {

    private val service = toolWindow.project.service<MyProjectService>()

    fun getContent() = JBPanel<JBPanel<*>>().apply {
        val label = JBLabel(MyBundle["randomLabel", "?"])

        add(label)
        add(JButton(MyBundle["shuffle"]).apply {
            addActionListener {
                label.text = MyBundle["randomLabel", service.getRandomNumber()]
            }
        })
    }
}
