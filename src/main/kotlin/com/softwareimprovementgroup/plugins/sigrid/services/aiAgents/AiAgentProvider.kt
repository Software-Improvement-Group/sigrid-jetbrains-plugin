package com.softwareimprovementgroup.plugins.sigrid.services.aiAgents

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.models.FixPrompt

/**
 * An AI coding agent the plugin can hand a prompt to.
 *
 * There is no vendor-neutral way to send a prompt to an agent, so every agent needs a small
 * adapter. Everything else - deciding what to say - is shared, see [com.softwareimprovementgroup.plugins.sigrid.promptBuilders.FixItPromptBuilder].
 */
interface AiAgentProvider {
    /** Stable identifier. */
    val id: String

    /** Human readable name, shown when the user has to pick between agents. */
    val label: String

    /** Whether the agent understands `/sigrid:...` slash commands from the Sigrid plugin. */
    val supportsSlashCommands: Boolean

    /** Whether the agent is installed and can accept a handoff. */
    fun isAvailable(): Boolean

    /** Best-effort check whether the Sigrid MCP server is wired up for this agent. */
    fun hasSigridMcp(): Boolean

    /** Opens the agent with [prompt] prefilled and starts it. */
    fun handoff(project: Project, prompt: FixPrompt)

    /** For agents that offer a way to wire up the Sigrid MCP server. */
    fun getMcpInstallHint(): McpInstallHint? = null
}

/** A one-click nudge towards an agent's Sigrid MCP install flow. */
data class McpInstallHint(
    val message: String,
    val action: String,
    val url: String,
)
