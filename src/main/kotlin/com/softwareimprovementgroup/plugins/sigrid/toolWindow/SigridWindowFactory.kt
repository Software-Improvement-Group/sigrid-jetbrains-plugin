package com.softwareimprovementgroup.plugins.sigrid.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridProjectSettingsConfigurable
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.ArchitecturePanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.MaintainabilityPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.OpenSourceHealthPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.SecurityPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.SigridPanel


class SigridWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val architecturePanel = ArchitecturePanel(project)
        val tabs = listOf(
            SigridBundle["maintainability.tab"] to MaintainabilityPanel(project),
            SigridBundle["security.tab"] to SecurityPanel(project),
            SigridBundle["osh.tab"] to OpenSourceHealthPanel(project),
            SigridBundle["architecture.tab"] to architecturePanel,
        )
        val allPanels = tabs.map { it.second }

        syncSearchAndFileFilter(allPanels)
        toolWindow.setTitleActions(listOf(refreshAction(allPanels), settingsAction(project)))
        addTabs(toolWindow, tabs)
        activateOnFirstSelection(toolWindow, architecturePanel)
        toolWindow.contentManager.setSelectedContent(toolWindow.contentManager.getContent(0)!!)
    }

    private fun syncSearchAndFileFilter(allPanels: List<SigridPanel<*>>) {
        allPanels.forEach { panel ->
            panel.onSearchChange = { query -> allPanels.filter { it !== panel }.forEach { it.setSearchText(query) } }
            panel.onFileFilterChange = { value -> allPanels.filter { it !== panel }.forEach { it.setActiveFileOnly(value) } }
        }
    }

    private fun refreshAction(allPanels: List<SigridPanel<*>>) =
        object : AnAction(SigridBundle["panel.refresh.button"], null, AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                allPanels.forEach { it.loadData() }
            }
        }

    private fun settingsAction(project: Project) =
        object : AnAction(SigridBundle["panel.settings.button"], null, AllIcons.General.Settings) {
            override fun actionPerformed(e: AnActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, SigridProjectSettingsConfigurable::class.java)
            }
        }

    private fun addTabs(toolWindow: ToolWindow, tabs: List<Pair<String, SigridPanel<*>>>) {
        val contentFactory = ContentFactory.getInstance()
        tabs.forEach { (title, panel) ->
            toolWindow.contentManager.addContent(contentFactory.createContent(panel, title, false))
        }
    }

    /** The architecture graph is large, so its panel only loads once its tab is first selected. */
    private fun activateOnFirstSelection(toolWindow: ToolWindow, panel: SigridPanel<*>) {
        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                if (event.operation == ContentManagerEvent.ContentOperation.add && event.content.component === panel) panel.activate()
            }
        })
    }
}
