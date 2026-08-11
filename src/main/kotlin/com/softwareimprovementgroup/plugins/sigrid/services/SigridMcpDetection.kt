package com.softwareimprovementgroup.plugins.sigrid.services

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.thisLogger
import java.io.File

/**
 * Tool names exposed by the Sigrid MCP server, named individually so the prompt builder references
 * the same strings instead of re-typing them. Deliberately read-only: the agent must never record
 * anything back to Sigrid, so `update_finding_status` is intentionally absent here.
 */
object SigridToolNames {
    const val MAINTAINABILITY_GET_FINDINGS = "maintainability_get_findings"
    const val SECURITY_GET_FINDINGS = "security_get_findings"
    const val OPENSOURCEHEALTH_GET_RISKS = "opensourcehealth_get_risks"
    const val OPENSOURCEHEALTH_GET_VULNERABILITIES = "opensourcehealth_get_vulnerabilities"
    const val GUARDRAILS_QUALITY_CHECK = "guardrails_quality_check"
}

/**
 * Best-effort detection of the Sigrid MCP server for Claude Code.
 *
 * There is no API to enumerate configured MCP servers, so this reads the documented Claude Code
 * plugin layout under `~/.claude`. Detection only ever decorates the prompt and the UI hint - a
 * failed detection never blocks a handoff, because the prompt embeds the findings themselves and
 * works without MCP.
 */
object SigridMcpDetection {
    const val SIGRID_MCP_INSTALL_URL =
        "https://docs.sigrid-says.com/integrations/integration-sigrid-mcp.html#installation"

    /** Matches "sigrid" as a whole word, so keys like `mysigrid@x` don't match. */
    private val SIGRID_WORD = Regex("""\bsigrid\b""", RegexOption.IGNORE_CASE)

    /**
     * Whether an *enabled* Sigrid Claude Code plugin (which provides the MCP server and the
     * `/sigrid:...` skills) is present in `~/.claude/settings.json`. Requires the plugin to be
     * enabled, not merely installed: a disabled plugin exposes no MCP server, so it must count as
     * "not detected". A missing or malformed file also means "not detected".
     */
    fun hasSigridClaudePlugin(claudeHome: File = defaultClaudeHome()): Boolean = try {
        val enabledPlugins = parseJsonObject(File(claudeHome, "settings.json"))
            ?.getAsJsonObject("enabledPlugins")
        enabledPlugins?.entrySet()?.any { (name, value) -> isEnabledSigridPlugin(name, value) } == true
    } catch (e: Exception) {
        thisLogger().debug("Sigrid Claude Code plugin detection failed", e)
        false
    }

    private fun defaultClaudeHome(): File = File(System.getProperty("user.home"), ".claude")

    private fun isEnabledSigridPlugin(name: String, value: JsonElement): Boolean =
        SIGRID_WORD.containsMatchIn(name) &&
            value.isJsonPrimitive &&
            value.asJsonPrimitive.isBoolean &&
            value.asBoolean

    private fun parseJsonObject(file: File): JsonObject? = try {
        readText(file)?.let { JsonParser.parseString(it).asJsonObject }
    } catch (e: Exception) {
        thisLogger().debug("Failed to parse ${file.path} as JSON", e)
        null
    }

    private fun readText(file: File): String? = try {
        if (file.isFile) file.readText() else null
    } catch (e: Exception) {
        thisLogger().debug("Failed to read ${file.path}", e)
        null
    }
}
