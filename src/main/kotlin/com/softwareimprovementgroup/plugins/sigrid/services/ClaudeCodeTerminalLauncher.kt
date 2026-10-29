package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FixPrompt
import com.softwareimprovementgroup.plugins.sigrid.notifySigrid
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

private const val CLAUDE_CLI = "claude"
private const val PROMPT_DIR_NAME = "sigrid-fix-prompts"

object ClaudeCodeTerminalLauncher {
    private val promptCounter = AtomicInteger(0)

    fun launch(project: Project, prompt: FixPrompt, claudeCli: String = CLAUDE_CLI) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // The prompt goes into a file rather than onto the command line: it is multi-line and
                // contains finding text from the Sigrid API. The arguments only ever hold text we control
                // plus paths, and are passed as argv, so no shell ever interprets them.
                val promptFile = writePromptFile(prompt.text)
                ApplicationManager.getApplication().invokeLater {
                    try {
                        openTerminalAndRun(project, prompt, promptFile, claudeCli)
                    } catch (e: Throwable) {
                        thisLogger().warn("Failed to open terminal for Claude Code", e)
                        notifyLaunchFailed(project)
                    }
                }
            } catch (e: Throwable) {
                thisLogger().warn("Failed to write Claude Code prompt file", e)
                ApplicationManager.getApplication().invokeLater { notifyLaunchFailed(project) }
            }
        }
    }

    private fun openTerminalAndRun(project: Project, prompt: FixPrompt, promptFile: Path, claudeCli: String) {
        // The experimental tab builder replaces the soft-deprecated TerminalToolWindowManager.createShellWidget;
        // every other non-deprecated creator is @ApiStatus.Internal, which the Marketplace verifier flags.
        TerminalToolWindowTabsManager.getInstance(project)
            .createTabBuilder()
            .workingDirectory(project.basePath)
            .tabName(SigridBundle["finding.fixit.terminal.tab.title"])
            .shellCommand(buildArguments(prompt.lead, promptFile, claudeCli))
            .requestFocus(true)
            .createTab()
    }

    private fun writePromptFile(text: String): Path {
        val directory = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), PROMPT_DIR_NAME))
        // Unique per handoff: two terminals must not end up pointing at the same file.
        val file = directory.resolve("sigrid-fix-${System.currentTimeMillis()}-${promptCounter.incrementAndGet()}.md")
        Files.writeString(file, text)
        return file
    }

    private fun notifyLaunchFailed(project: Project) {
        notifySigrid(project, SigridBundle["finding.fixit.terminal.error"], NotificationType.ERROR)
    }

    /**
     * The program followed by its arguments, executed directly with no shell in between.
     * `claude [options] [prompt]` accepts the prompt positionally, but `--add-dir` is variadic and
     * would swallow it, so every option has to come after the prompt.
     */
    internal fun buildArguments(lead: String, promptFile: Path, claudeCli: String = CLAUDE_CLI): List<String> {
        val message = "$lead The findings to fix are described in $promptFile. Read that file first."
        return listOf(claudeCli, message, "--add-dir", promptFile.parent.toString())
    }
}
