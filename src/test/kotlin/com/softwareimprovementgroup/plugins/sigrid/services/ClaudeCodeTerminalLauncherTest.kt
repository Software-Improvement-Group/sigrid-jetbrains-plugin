package com.softwareimprovementgroup.plugins.sigrid.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ClaudeCodeTerminalLauncherTest {

    private val promptFile = Path.of("/tmp/sigrid-fix-prompts/sigrid-fix-1.md")

    @Test
    fun buildArguments_startsWithClaude() {
        val arguments = ClaudeCodeTerminalLauncher.buildArguments("Fix the following Sigrid findings.", promptFile)
        assertEquals("claude", arguments.first())
    }

    @Test
    fun buildArguments_passesTheMessageAsOneArgument() {
        val arguments = ClaudeCodeTerminalLauncher.buildArguments("Fix the following Sigrid findings.", promptFile)
        assertEquals(
            "Fix the following Sigrid findings. The findings to fix are described in $promptFile. Read that file first.",
            arguments[1],
        )
    }

    @Test
    fun buildArguments_addsPromptDirectoryAfterThePrompt() {
        val arguments = ClaudeCodeTerminalLauncher.buildArguments("/sigrid:sigrid-improve autonomous", promptFile)
        // --add-dir is variadic and would swallow the prompt if it came first.
        assertEquals(listOf("--add-dir", promptFile.parent.toString()), arguments.drop(2))
    }

    @Test
    fun buildArguments_usesResolvedClaudeCliWhenProvided() {
        val arguments = ClaudeCodeTerminalLauncher.buildArguments(
            "Fix the following Sigrid findings.",
            promptFile,
            claudeCli = "/opt/homebrew/bin/claude",
        )
        assertTrue(arguments.first() == "/opt/homebrew/bin/claude")
    }
}
