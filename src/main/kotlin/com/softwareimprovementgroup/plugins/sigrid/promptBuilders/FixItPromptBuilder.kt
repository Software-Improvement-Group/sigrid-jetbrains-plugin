package com.softwareimprovementgroup.plugins.sigrid.promptBuilders

import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.FixItContext
import com.softwareimprovementgroup.plugins.sigrid.models.FixPrompt
import com.softwareimprovementgroup.plugins.sigrid.models.FixPromptContext
import com.softwareimprovementgroup.plugins.sigrid.services.SigridMcpDetection
import com.softwareimprovementgroup.plugins.sigrid.services.SigridToolNames

/**
 * Finding categories, matching the panel tabs. These key the slash-command, plain-instruction and
 * MCP-tool maps, so the strings must stay exactly as the Sigrid tooling expects them.
 */
object FindingCategory {
    const val MAINTAINABILITY = "Maintainability"
    const val SECURITY = "Security"
    const val OPEN_SOURCE_HEALTH = "Open Source Health"
}

data class FixPromptOptions(
    val supportsSlashCommands: Boolean,
    val mcpDetected: Boolean,
    /** Renders a tool name the way the agent links tools, or null if it cannot. */
    val resolveToolReference: (String) -> String? = { null },
)

/**
 * Builds the prompt handed to an AI agent. The same body goes to every agent; only the lead
 * instruction differs, depending on whether the agent understands the Sigrid plugin's slash
 * commands and whether the Sigrid MCP server was detected. Ported from the VS Code extension's
 * `fix-prompt-builder.ts` so both integrations speak to the agent identically.
 */
object FixItPromptBuilder {
    // Slash commands are protocol strings the agent parses literally, not language for a human -
    // they must stay exactly as written and are never localized.
    private val SLASH_COMMANDS = mapOf(
        FindingCategory.MAINTAINABILITY to "/sigrid:sigrid-improve autonomous",
        FindingCategory.OPEN_SOURCE_HEALTH to "/sigrid:fix-osh-risk",
    )

    private val PLAIN_INSTRUCTIONS = mapOf(
        FindingCategory.MAINTAINABILITY to SigridBundle["finding.fixit.prompt.maintainability"],
        FindingCategory.SECURITY to SigridBundle["finding.fixit.prompt.security"],
        FindingCategory.OPEN_SOURCE_HEALTH to SigridBundle["finding.fixit.prompt.opensourcehealth"],
    )

    private val MIXED_INSTRUCTION = SigridBundle["finding.fixit.prompt.mixed"]

    private val MCP_HINT = SigridBundle["finding.fixit.prompt.mcphint", SigridMcpDetection.SIGRID_MCP_INSTALL_URL]

    /** The Sigrid MCP tools worth naming per category. Read-only on purpose. */
    private val MCP_TOOLS = mapOf(
        FindingCategory.MAINTAINABILITY to listOf(SigridToolNames.MAINTAINABILITY_GET_FINDINGS, SigridToolNames.GUARDRAILS_QUALITY_CHECK),
        FindingCategory.SECURITY to listOf(SigridToolNames.SECURITY_GET_FINDINGS, SigridToolNames.GUARDRAILS_QUALITY_CHECK),
        FindingCategory.OPEN_SOURCE_HEALTH to listOf(SigridToolNames.OPENSOURCEHEALTH_GET_RISKS, SigridToolNames.OPENSOURCEHEALTH_GET_VULNERABILITIES),
    )

    private const val GUARDRAILS_TOOL = SigridToolNames.GUARDRAILS_QUALITY_CHECK

    fun build(findings: List<FixItContext>, context: FixPromptContext, options: FixPromptOptions): FixPrompt {
        val canUseSkill = options.supportsSlashCommands && options.mcpDetected
        val lead = buildLeadInstruction(findings, canUseSkill)
        val sections = mutableListOf(lead, buildContextLine(context), buildFindingList(findings))

        if (!options.mcpDetected) {
            sections.add(0, MCP_HINT)
        } else if (!isSlashCommand(lead)) {
            // A Sigrid skill already orchestrates MCP, so instructions of our own would only fight it.
            sections.add(buildMcpInstruction(mcpToolsFor(findings), options.resolveToolReference))
        }

        return FixPrompt(lead, sections.filter { it.isNotEmpty() }.joinToString("\n\n"))
    }

    private fun isSlashCommand(lead: String): Boolean = lead.startsWith("/")

    /** The tools relevant to the selection, in category order and without repeats. */
    private fun mcpToolsFor(findings: List<FixItContext>): List<String> =
        findings.map { it.category }.toSet().flatMap { MCP_TOOLS[it] ?: emptyList() }.distinct()

    private fun buildMcpInstruction(tools: List<String>, resolveToolReference: (String) -> String?): String {
        if (tools.isEmpty()) return ""
        val reference = { tool: String -> resolveToolReference(tool) ?: tool }
        val queryTools = tools.filter { it != GUARDRAILS_TOOL }
        val lines = mutableListOf("The Sigrid MCP server is available - use it instead of guessing what Sigrid measured:")
        if (queryTools.isNotEmpty()) {
            lines.add("- confirm each finding above against Sigrid with ${queryTools.joinToString(" and ") { reference(it) }}")
        }
        if (tools.contains(GUARDRAILS_TOOL)) {
            lines.add("- after editing, run ${reference(GUARDRAILS_TOOL)} on the changed files and iterate until it passes")
        }
        lines.add("Do not change the status of any finding in Sigrid.")
        return lines.joinToString("\n")
    }

    /**
     * Prefers a Sigrid skill when the agent supports slash commands and every finding belongs to a
     * category that has one. Security has no dedicated skill, so it always gets a plain instruction.
     */
    private fun buildLeadInstruction(findings: List<FixItContext>, supportsSlashCommands: Boolean): String {
        val category = singleCategory(findings) ?: return MIXED_INSTRUCTION
        val slashCommand = SLASH_COMMANDS[category]
        if (supportsSlashCommands && slashCommand != null) return slashCommand
        return PLAIN_INSTRUCTIONS[category] ?: MIXED_INSTRUCTION
    }

    /** The category shared by all findings, or null for an empty or mixed selection. */
    private fun singleCategory(findings: List<FixItContext>): String? =
        findings.map { it.category }.toSet().singleOrNull()

    private fun buildContextLine(context: FixPromptContext): String {
        val parts = mutableListOf<String>()
        if (context.customer.isNotBlank()) parts.add("Customer: ${context.customer}")
        if (context.system.isNotBlank()) parts.add("System: ${context.system}")
        return parts.joinToString("   ")
    }

    private fun buildFindingList(findings: List<FixItContext>): String {
        val lines = findings.mapIndexed { index, finding -> formatFinding(finding, index + 1) }
        return (listOf("Findings (from Sigrid - fix these, do not go looking for others):") + lines).joinToString("\n")
    }

    private fun formatFinding(finding: FixItContext, position: Int): String {
        val builder = StringBuilder("$position. ${finding.category} / ${finding.severity} - ${finding.title}")
        val locations = finding.fileLocations.map { formatLocation(it) }.filter { it.isNotEmpty() }
        if (locations.isNotEmpty()) builder.append("\n   Locations: ${locations.joinToString(", ")}")
        finding.href?.takeIf { it.isNotBlank() }?.let { builder.append("\n   Reference: $it") }
        return builder.toString()
    }

    internal fun formatLocation(loc: FileLocation): String {
        if (loc.filePath.isBlank()) return ""
        val start = loc.startLine ?: return loc.filePath
        val lines = if (loc.endLine != null && loc.endLine != start) "$start-${loc.endLine}" else "$start"
        return "${loc.filePath}:$lines"
    }
}
