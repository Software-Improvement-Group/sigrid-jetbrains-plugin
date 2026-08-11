package com.softwareimprovementgroup.plugins.sigrid.services

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ClaudeCodeTerminalLauncherTest {

    private val promptFile = Path.of("/tmp/sigrid-fix-prompts/sigrid-fix-1.md")

    @Test
    fun buildCommand_startsWithClaudeAndQuotesTheMessage() {
        val command = ClaudeCodeTerminalLauncher.buildCommand("Fix the following Sigrid findings.", promptFile)
        assertTrue(command.startsWith("claude \""))
    }

    @Test
    fun buildCommand_pointsTheAgentAtThePromptFile() {
        val command = ClaudeCodeTerminalLauncher.buildCommand("Fix the following Sigrid findings.", promptFile)
        assertTrue(command.contains("$promptFile. Read that file first."))
    }

    @Test
    fun buildCommand_addsPromptDirectoryAfterThePrompt() {
        val command = ClaudeCodeTerminalLauncher.buildCommand("/sigrid:sigrid-improve autonomous", promptFile)
        assertTrue(command.contains("--add-dir"))
        // --add-dir is variadic and would swallow the prompt if it came first.
        assertTrue(command.indexOf("--add-dir") > command.indexOf("/sigrid:"))
    }

    @Test
    fun buildCommand_isSingleLine() {
        val command = ClaudeCodeTerminalLauncher.buildCommand("Fix the following Sigrid findings.", promptFile)
        assertFalse(command.contains("\n"), "a newline would submit the command early")
    }

    @Test
    fun buildCommand_neutralisesShellMetacharactersInTheLead() {
        // The lead is controlled text; sanitising defends against it ever becoming dynamic.
        val command = ClaudeCodeTerminalLauncher.buildCommand("Fix `whoami` and \$HOME now.", promptFile)
        assertFalse(command.contains("`"))
        assertFalse(command.contains("\$HOME"))
    }

    @Test
    fun quote_escapesDoubleQuotes() {
        assertTrue(ClaudeCodeTerminalLauncher.quote("say \"hi\"") == "\"say \\\"hi\\\"\"")
    }
}
