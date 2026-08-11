package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.PluginId
import java.io.File

private const val CLAUDE_CODE_VERSION_TIMEOUT_MS = 3000
private const val TERMINAL_PLUGIN_ID = "org.jetbrains.plugins.terminal"
private const val CLAUDE_CLI_NAME = "claude"

@Service(Service.Level.APP)
class ClaudeCodeDetector {
    @Volatile private var cached: Boolean? = null

    /** Returns the cached detection result, or null if detection hasn't completed yet. */
    fun isAvailableCached(): Boolean? = cached

    fun warmUpAsync() {
        if (cached != null) return
        ApplicationManager.getApplication().executeOnPooledThread {
            cached = detect()
        }
    }

    internal fun detect(
        resolvePath: () -> String? = ::resolveClaudePath,
        runVersionCheck: (String) -> Boolean = ::runClaudeVersionCheck,
    ): Boolean {
        val path = resolvePath() ?: return false
        return try {
            runVersionCheck(path)
        } catch (e: Exception) {
            thisLogger().debug("Claude Code CLI detection failed", e)
            false
        }
    }

    companion object {
        fun getInstance(): ClaudeCodeDetector =
            ApplicationManager.getApplication().getService(ClaudeCodeDetector::class.java)

        @Suppress("DEPRECATION")
        fun isTerminalPluginEnabled(): Boolean =
            PluginManagerCore.getPlugin(PluginId.getId(TERMINAL_PLUGIN_ID))?.isEnabled == true
    }
}

private fun runClaudeVersionCheck(path: String): Boolean {
    val handler = CapturingProcessHandler(GeneralCommandLine(path, "--version"))
    val output = handler.runProcess(CLAUDE_CODE_VERSION_TIMEOUT_MS)
    return !output.isTimeout && output.exitCode == 0
}

/**
 * PATH is searched first: a hit there means an interactive terminal resolves the bare name too.
 * Well-known install directories are probed afterwards, because an IDE launched from Finder or the
 * Dock inherits a truncated PATH that often misses Homebrew and user-local bin directories.
 */
private fun resolveClaudePath(): String? =
    PathEnvironmentVariableUtil.findInPath(CLAUDE_CLI_NAME)?.absolutePath
        ?: findInWellKnownDirectories(System.getProperty("user.home"))

internal fun findInWellKnownDirectories(home: String, exists: (File) -> Boolean = File::isFile): String? =
    wellKnownClaudePaths(home).firstOrNull(exists)?.absolutePath

private fun wellKnownClaudePaths(home: String): List<File> = listOf(
    File(home, ".claude/local/$CLAUDE_CLI_NAME"),
    File(home, ".local/bin/$CLAUDE_CLI_NAME"),
    File("/opt/homebrew/bin/$CLAUDE_CLI_NAME"),
    File("/usr/local/bin/$CLAUDE_CLI_NAME"),
    File(home, ".bun/bin/$CLAUDE_CLI_NAME"),
    File(home, ".volta/bin/$CLAUDE_CLI_NAME"),
)
