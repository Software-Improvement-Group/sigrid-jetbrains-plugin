package com.softwareimprovementgroup.plugins.sigrid.toolWindow

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.MaintainabilityPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.OpenSourceHealthPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.SecurityPanel


class SigridWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val maintainabilityContent = contentFactory.createContent(MaintainabilityPanel(project), "Maintainability", false)
        val securityContent = contentFactory.createContent(SecurityPanel(project), "Security", false)
        val openSourceHealthContent = contentFactory.createContent(OpenSourceHealthPanel(project), "Open Source Health", false)

        toolWindow.contentManager.addContent(maintainabilityContent)
        toolWindow.contentManager.addContent(securityContent)
        toolWindow.contentManager.addContent(openSourceHealthContent)

        toolWindow.contentManager.setSelectedContent(maintainabilityContent)
    }

    override fun shouldBeAvailable(project: Project) = true
}
