package com.softwareimprovementgroup.plugins.sigrid.services.aiAgents

/** Every agent the plugin can hand off to. Add a provider here to support another agent. */
object AiAgentRegistry {
    val agents: List<AiAgentProvider> = listOf(ClaudeCodeProvider())
}
