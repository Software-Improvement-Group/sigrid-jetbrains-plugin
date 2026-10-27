package com.softwareimprovementgroup.plugins.sigrid.toolWindow

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.icons.AllIcons
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import com.softwareimprovementgroup.plugins.sigrid.services.onboarding.OnboardingStatus
import com.softwareimprovementgroup.plugins.sigrid.services.onboarding.SigridOnboardingListener
import com.softwareimprovementgroup.plugins.sigrid.services.onboarding.SigridOnboardingTopic
import com.softwareimprovementgroup.plugins.sigrid.services.onboarding.SystemOnboardingService
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.ArchitecturePanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.MaintainabilityPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.OnboardingPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.OpenSourceHealthPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.SecurityPanel
import com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels.SigridPanel
import javax.swing.JComponent

/**
 * Fills the Sigrid tool window with either the findings tabs or, while the project's system is not
 * onboarded, the onboarding view. The findings panels are created once and re-attached on later swaps.
 */
internal class SigridToolWindowContent(private val project: Project, private val toolWindow: ToolWindow) {

    private enum class View { TABS, ONBOARDING }

    private val onboarding = SystemOnboardingService.getInstance(project)
    private val onboardingPanel = OnboardingPanel(project)
    private val architecturePanel by lazy { ArchitecturePanel(project) }
    private val tabs by lazy { createTabs() }
    private var current: View? = null
    private var tabsAttachedBefore = false

    fun start() {
        project.messageBus.connect(toolWindow.disposable).subscribe(
            SigridOnboardingTopic.TOPIC,
            SigridOnboardingListener { ApplicationManager.getApplication().invokeLater { render() } },
        )
        render()
        onboarding.check()
    }

    fun refreshAction() =
        object : AnAction(SigridBundle["panel.refresh.button"], null, AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                if (current == View.TABS) tabs.forEach { it.second.loadData() } else onboarding.check()
            }
        }

    private fun createTabs(): List<Pair<String, SigridPanel<*>>> {
        val panels = listOf(
            SigridBundle["maintainability.tab"] to MaintainabilityPanel(project),
            SigridBundle["security.tab"] to SecurityPanel(project),
            SigridBundle["osh.tab"] to OpenSourceHealthPanel(project),
            SigridBundle["architecture.tab"] to architecturePanel,
        )
        syncSearchAndFileFilter(panels.map { it.second })
        activateOnFirstSelection(architecturePanel)
        return panels
    }

    private fun render() {
        val view = if (wantsTabs()) View.TABS else View.ONBOARDING
        if (view != current) {
            current = view
            replaceContent(view)
        }
        onboardingPanel.render(onboarding.state)
    }

    private fun wantsTabs(): Boolean =
        !SigridProjectConfiguration.getInstance(project).isConfigurationValid ||
            onboarding.state.status == OnboardingStatus.ONBOARDED

    private fun replaceContent(view: View) {
        toolWindow.contentManager.removeAllContents(true)
        if (view == View.TABS) attachTabs() else addContent(SigridBundle["onboarding.tab"], onboardingPanel)
    }

    private fun attachTabs() {
        tabs.forEach { (title, panel) -> addContent(title, panel) }
        if (tabsAttachedBefore) tabs.forEach { it.second.loadData() }
        tabsAttachedBefore = true
        toolWindow.contentManager.getContent(0)?.let { toolWindow.contentManager.setSelectedContent(it) }
    }

    private fun addContent(title: String, component: JComponent) {
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(component, title, false))
    }

    private fun syncSearchAndFileFilter(allPanels: List<SigridPanel<*>>) {
        allPanels.forEach { panel ->
            panel.onSearchChange = { query -> allPanels.filter { it !== panel }.forEach { it.setSearchText(query) } }
            panel.onFileFilterChange = { value -> allPanels.filter { it !== panel }.forEach { it.setActiveFileOnly(value) } }
        }
    }

    /** The architecture graph is large, so its panel only loads once its tab is first selected. */
    private fun activateOnFirstSelection(panel: SigridPanel<*>) {
        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                if (event.operation == ContentManagerEvent.ContentOperation.add && event.content.component === panel) panel.activate()
            }
        })
    }
}
