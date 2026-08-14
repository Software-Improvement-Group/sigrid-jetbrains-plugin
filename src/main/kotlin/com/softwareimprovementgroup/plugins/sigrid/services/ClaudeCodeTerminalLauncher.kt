package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FixPrompt
import com.softwareimprovementgroup.plugins.sigrid.notifySigrid
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
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
                // contains finding text from the Sigrid API, which must never be interpreted by a shell.
                // The command line only ever holds text we control plus quoted paths.
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
        // TODO: Replace createShellWidget with a stable non-deprecated alternative.
        // createShellWidget returns the engine-agnostic com.intellij.terminal.ui.TerminalWidget and is only
        // soft-deprecated (plain @Deprecated, not scheduled for removal); every non-deprecated creator that
        // returns a TerminalWidget is @ApiStatus.Internal, which the Marketplace verifier flags, so this stays
        // the safest public option.
        val widget = TerminalToolWindowManager.getInstance(project).createShellWidget(
            project.basePath,
            SigridBundle["finding.fixit.terminal.tab.title"],
            true,
            true,
        )
        widget.sendCommandToExecute(buildCommand(prompt.lead, promptFile, claudeCli))
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
     * `claude [options] [prompt]` accepts the prompt positionally, but `--add-dir` is variadic and
     * would swallow it, so every option has to come after the prompt.
     */
    internal fun buildCommand(lead: String, promptFile: Path, claudeCli: String = CLAUDE_CLI): String {
        val message = "${sanitizeForCommandLine(lead)} The findings to fix are described in $promptFile. Read that file first."
        return "$claudeCli ${quote(message)} --add-dir ${quote(promptFile.parent.toString())}"
    }

    internal fun quote(value: String): String = "\"${value.replace("\"", "\\\"")}\""

    /** Strips anything a shell would act on, defending the controlled lead against future changes. */
    private fun sanitizeForCommandLine(value: String): String =
        value.replace(Regex("""["`$\\!;|&()<>\r\n]"""), " ").trim()
}
