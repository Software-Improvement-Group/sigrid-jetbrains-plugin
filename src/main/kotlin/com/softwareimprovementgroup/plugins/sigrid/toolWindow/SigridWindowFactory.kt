package com.softwareimprovementgroup.plugins.sigrid.toolWindow

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout


class SigridWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val maintainabilityContent = contentFactory.createContent(
            createTabPanel("Maintainability findings will appear here."),
            "Maintainability",
            false
        )

        val securityContent = contentFactory.createContent(
            createTabPanel("Security findings will appear here."),
            "Security",
            false
        )

        val openSourceHealthContent = contentFactory.createContent(
            createTabPanel("Open source health findings will appear here."),
            "Open Source Health",
            false
        )

        toolWindow.contentManager.addContent(maintainabilityContent)
        toolWindow.contentManager.addContent(securityContent)
        toolWindow.contentManager.addContent(openSourceHealthContent)

        toolWindow.contentManager.setSelectedContent(maintainabilityContent)
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun createTabPanel(text: String) = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        val label = JBLabel(text)
        label.horizontalAlignment = JBLabel.CENTER
        add(label, BorderLayout.CENTER)
    }
}
