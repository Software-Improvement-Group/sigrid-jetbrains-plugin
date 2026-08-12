package com.softwareimprovementgroup.plugins.sigrid.services.aiAgents

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FixPrompt
import com.softwareimprovementgroup.plugins.sigrid.services.ClaudeCodeDetector
import com.softwareimprovementgroup.plugins.sigrid.services.ClaudeCodeTerminalLauncher
import com.softwareimprovementgroup.plugins.sigrid.services.SigridMcpDetection

/**
 * Hands off to Claude Code by launching the `claude` CLI in a terminal. This is the portable
 * handoff route; the VS Code "extension route" relies on VS Code URIs that do not exist here.
 */
class ClaudeCodeProvider : AiAgentProvider {
    override val id = CLAUDE_CODE_AGENT_ID
    override val label = "Claude Code (CLI)"
    override val supportsSlashCommands = true

    override fun isAvailable(): Boolean =
        ClaudeCodeDetector.getInstance().isAvailableCached() == true && ClaudeCodeDetector.isTerminalPluginEnabled()

    override fun hasSigridMcp(): Boolean = SigridMcpDetection.hasSigridClaudePlugin()

    override fun handoff(project: Project, prompt: FixPrompt) {
        val resolvedPath = ClaudeCodeDetector.getInstance().resolvedPathCached()
        if (resolvedPath != null) {
            ClaudeCodeTerminalLauncher.launch(project, prompt, resolvedPath)
        } else {
            ClaudeCodeTerminalLauncher.launch(project, prompt)
        }
    }

    override fun getMcpInstallHint(): McpInstallHint = McpInstallHint(
        message = SigridBundle["finding.fixit.mcp.notdetected.message"],
        action = SigridBundle["finding.fixit.mcp.notdetected.action"],
        url = SigridMcpDetection.SIGRID_MCP_INSTALL_URL,
    )

    companion object {
        const val CLAUDE_CODE_AGENT_ID = "claude-code"
    }
}
