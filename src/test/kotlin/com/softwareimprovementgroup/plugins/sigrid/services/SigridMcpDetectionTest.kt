package com.softwareimprovementgroup.plugins.sigrid.services

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SigridMcpDetectionTest {

    @TempDir
    lateinit var claudeHome: File

    private fun writeSettings(contents: String) {
        File(claudeHome, "settings.json").writeText(contents)
    }

    @Test
    fun enabledPlugin_isDetected() {
        writeSettings("""{ "enabledPlugins": { "sigrid@sigrid-ai-toolkit": true } }""")
        assertTrue(SigridMcpDetection.hasSigridClaudePlugin(claudeHome))
    }

    @Test
    fun disabledPlugin_isNotDetected() {
        writeSettings("""{ "enabledPlugins": { "sigrid@sigrid-ai-toolkit": false } }""")
        assertFalse(SigridMcpDetection.hasSigridClaudePlugin(claudeHome))
    }

    @Test
    fun pluginAbsentFromEnabledPlugins_isNotDetected() {
        writeSettings("""{ "enabledPlugins": { "other@marketplace": true } }""")
        assertFalse(SigridMcpDetection.hasSigridClaudePlugin(claudeHome))
    }

    @Test
    fun noEnabledPluginsKey_isNotDetected() {
        writeSettings("""{ "theme": "dark" }""")
        assertFalse(SigridMcpDetection.hasSigridClaudePlugin(claudeHome))
    }

    @Test
    fun missingSettingsFile_isNotDetected() {
        assertFalse(SigridMcpDetection.hasSigridClaudePlugin(claudeHome))
    }

    @Test
    fun malformedSettingsFile_isNotDetected() {
        writeSettings("{ this is not json")
        assertFalse(SigridMcpDetection.hasSigridClaudePlugin(claudeHome))
    }

    @Test
    fun enabledPluginsNotAnObject_isNotDetected() {
        writeSettings("""{ "enabledPlugins": "nonsense" }""")
        assertFalse(SigridMcpDetection.hasSigridClaudePlugin(claudeHome))
    }

    @Test
    fun wordBoundaryPreventsPartialMatch() {
        writeSettings("""{ "enabledPlugins": { "mysigridwrapper@x": true } }""")
        assertFalse(SigridMcpDetection.hasSigridClaudePlugin(claudeHome))
    }

    @Test
    fun nonBooleanEnabledValue_isNotDetected() {
        writeSettings("""{ "enabledPlugins": { "sigrid@sigrid-ai-toolkit": "true" } }""")
        assertFalse(SigridMcpDetection.hasSigridClaudePlugin(claudeHome))
    }
}
