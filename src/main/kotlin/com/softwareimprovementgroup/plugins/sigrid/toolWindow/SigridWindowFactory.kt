package com.softwareimprovementgroup.plugins.sigrid.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.MaintainabilityPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.OpenSourceHealthPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.SecurityPanel


class SigridWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val maintainabilityPanel = MaintainabilityPanel(project)
        val securityPanel = SecurityPanel(project)
        val oshPanel = OpenSourceHealthPanel(project)

        val allPanels = listOf(maintainabilityPanel, securityPanel, oshPanel)
        allPanels.forEach { panel -> panel.onRefresh = { allPanels.forEach { it.loadData() } } }
        allPanels.forEach { panel ->
            panel.onSearchChange = { query -> allPanels.filter { it !== panel }.forEach { it.setSearchText(query) } }
        }

        toolWindow.contentManager.addContent(contentFactory.createContent(maintainabilityPanel, SigridBundle["maintainability.tab"], false))
        toolWindow.contentManager.addContent(contentFactory.createContent(securityPanel, SigridBundle["security.tab"], false))
        toolWindow.contentManager.addContent(contentFactory.createContent(oshPanel, SigridBundle["osh.tab"], false))

        toolWindow.contentManager.setSelectedContent(toolWindow.contentManager.getContent(0)!!)
    }

    override fun shouldBeAvailable(project: Project) = true
}
