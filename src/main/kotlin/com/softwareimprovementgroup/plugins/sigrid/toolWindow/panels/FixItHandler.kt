package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.models.FixItContext
import com.softwareimprovementgroup.plugins.sigrid.notifySigrid
import com.softwareimprovementgroup.plugins.sigrid.models.FixPromptContext
import com.softwareimprovementgroup.plugins.sigrid.promptBuilders.FixItPromptBuilder
import com.softwareimprovementgroup.plugins.sigrid.promptBuilders.FixPromptOptions
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import com.softwareimprovementgroup.plugins.sigrid.services.aiAgents.AiAgentProvider
import com.softwareimprovementgroup.plugins.sigrid.services.aiAgents.AiAgentRegistry
import java.util.concurrent.ConcurrentHashMap

/**
 * Hands the selected findings to an AI coding agent as a prefilled prompt, mirroring the VS Code
 * extension's `FixFindingsWithAiCommand`: pick an agent, build the shared prompt, hand off, and -
 * when Sigrid MCP was not detected - nudge the user towards installing it.
 */
class FixItHandler<T>(
    private val project: Project,
    private val toFixItContext: (T) -> FixItContext,
) {
    fun openFixIt(findings: List<T>) {
        if (findings.isEmpty()) return
        val agent = AiAgentRegistry.agents.firstOrNull { it.isAvailable() } ?: return
        val config = SigridProjectConfiguration.getInstance(project)

        ApplicationManager.getApplication().executeOnPooledThread {
            val mcpDetected = agent.hasSigridMcp()
            val prompt = FixItPromptBuilder.build(
                findings = findings.map(toFixItContext),
                context = FixPromptContext(config.effectiveCustomer, config.system),
                options = FixPromptOptions(agent.supportsSlashCommands, mcpDetected),
            )

            agent.handoff(project, prompt)
            if (!mcpDetected) notifyMcpNotDetected(agent)
        }
    }

    /** Nudges the user towards the agent's MCP install flow once per project session, never blocking. */
    private fun notifyMcpNotDetected(agent: AiAgentProvider) {
        val hint = agent.getMcpInstallHint() ?: return
        if (!shownMcpHints.add("${project.basePath}:${agent.id}:mcpInstall")) return

        notifySigrid(project, hint.message, NotificationType.WARNING, hint.action) {
            BrowserUtil.browse(hint.url)
        }
    }

    companion object {
        private val shownMcpHints = ConcurrentHashMap.newKeySet<String>()
    }
}
