package com.softwareimprovementgroup.plugins.sigrid.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridProjectSettingsConfigurable


class SigridWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = SigridToolWindowContent(project, toolWindow)
        toolWindow.setTitleActions(listOf(content.refreshAction(), settingsAction(project)))
        content.start()
    }

    private fun settingsAction(project: Project) =
        object : AnAction(SigridBundle["panel.settings.button"], null, AllIcons.General.Settings) {
            override fun actionPerformed(e: AnActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, SigridProjectSettingsConfigurable::class.java)
            }
        }
}
