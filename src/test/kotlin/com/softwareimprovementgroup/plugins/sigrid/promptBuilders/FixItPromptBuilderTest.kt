package com.softwareimprovementgroup.plugins.sigrid.promptBuilders

import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.FixItContext
import com.softwareimprovementgroup.plugins.sigrid.models.FixPromptContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FixItPromptBuilderTest {

    private val context = FixPromptContext(customer = "my-customer", system = "my-system")
    private val slashAgent = FixPromptOptions(supportsSlashCommands = true, mcpDetected = true)
    private val plainAgent = FixPromptOptions(supportsSlashCommands = false, mcpDetected = true)

    private val maintainability = FixItContext(
        category = FindingCategory.MAINTAINABILITY,
        severity = "Very High",
        title = "src/panels/SigridPanel.kt: Unit size",
        fileLocations = listOf(FileLocation("svc", "src/panels/SigridPanel.kt", 120, 198)),
        href = null,
    )
    private val security = FixItContext(
        category = FindingCategory.SECURITY,
        severity = "High",
        title = "src/commands/CreateIssue.kt: Hardcoded secret",
        fileLocations = listOf(FileLocation("svc", "src/commands/CreateIssue.kt", 44)),
        href = null,
    )
    private val osh = FixItContext(
        category = FindingCategory.OPEN_SOURCE_HEALTH,
        severity = "High",
        title = "lodash 4.17.0",
        fileLocations = listOf(FileLocation("svc", "package.json")),
        href = null,
    )

    private fun text(findings: List<FixItContext>, options: FixPromptOptions) =
        FixItPromptBuilder.build(findings, context, options).text

    @Test
    fun maintainabilityOnly_withMcp_usesSkillAndContext() {
        val prompt = text(listOf(maintainability), slashAgent)
        assertTrue(prompt.startsWith("/sigrid:sigrid-improve autonomous"))
        assertTrue(prompt.contains("Customer: my-customer"))
        assertTrue(prompt.contains("System: my-system"))
    }

    @Test
    fun oshOnly_withMcp_usesOshSkillAndLocation() {
        val prompt = text(listOf(osh), slashAgent)
        assertTrue(prompt.startsWith("/sigrid:fix-osh-risk"))
        assertTrue(prompt.contains("Locations: package.json"))
    }

    @Test
    fun securityOnly_fallsBackToPlainInstruction() {
        val prompt = text(listOf(security), slashAgent)
        assertFalse(prompt.contains("/sigrid:"))
        assertTrue(prompt.startsWith("Fix the following Sigrid security findings."))
    }

    @Test
    fun mixedSelection_usesMixedInstructionAndNumberedList() {
        val prompt = text(listOf(maintainability, osh), slashAgent)
        assertFalse(prompt.contains("/sigrid:"))
        assertTrue(prompt.contains("1. Maintainability / Very High"))
        assertTrue(prompt.contains("2. Open Source Health / High"))
    }

    @Test
    fun agentWithoutSlashCommands_neverUsesSkill() {
        val prompt = text(listOf(maintainability), FixPromptOptions(supportsSlashCommands = false, mcpDetected = true))
        assertFalse(prompt.contains("/sigrid:"))
        assertTrue(prompt.startsWith("Fix the following Sigrid maintainability findings."))
    }

    @Test
    fun singleLineLocation_hasNoRange() {
        val prompt = text(listOf(security), slashAgent)
        assertTrue(prompt.contains("Locations: src/commands/CreateIssue.kt:44"))
    }

    @Test
    fun lineRangeLocation_isFormattedAsRange() {
        val prompt = text(listOf(maintainability), slashAgent)
        assertTrue(prompt.contains("src/panels/SigridPanel.kt:120-198"))
    }

    @Test
    fun mcpNotDetected_prependsInstallHint() {
        val prompt = text(listOf(maintainability), FixPromptOptions(supportsSlashCommands = true, mcpDetected = false))
        assertTrue(prompt.startsWith("Note: the Sigrid MCP server and Sigrid skills were not detected"))
        assertTrue(prompt.contains("sigrid-ai-toolkit") || prompt.contains("integration-sigrid-mcp"))
    }

    @Test
    fun mcpNotDetected_fallsBackToPlainLead() {
        val result = FixItPromptBuilder.build(
            listOf(maintainability),
            context,
            FixPromptOptions(supportsSlashCommands = true, mcpDetected = false),
        )
        assertFalse(result.lead.startsWith("/sigrid:"))
        assertEquals("Fix the following Sigrid maintainability findings.", result.lead)
    }

    @Test
    fun mcpDetected_plainAgent_namesCategoryTools() {
        val prompt = text(listOf(maintainability), plainAgent)
        assertTrue(prompt.contains("The Sigrid MCP server is available"))
        assertTrue(prompt.contains("maintainability_get_findings"))
        assertTrue(prompt.contains("guardrails_quality_check"))
    }

    @Test
    fun mcpDetected_skillLead_omitsMcpInstruction() {
        val prompt = text(listOf(maintainability), slashAgent)
        assertTrue(prompt.startsWith("/sigrid:"))
        assertFalse(prompt.contains("The Sigrid MCP server is available"))
    }

    @Test
    fun namesOnlySelectedCategoryTools() {
        val prompt = text(listOf(security), plainAgent)
        assertTrue(prompt.contains("security_get_findings"))
        assertFalse(prompt.contains("maintainability_get_findings"))
    }

    @Test
    fun oshWork_skipsQualityGate() {
        val prompt = text(listOf(osh), plainAgent)
        assertTrue(prompt.contains("opensourcehealth_get_risks"))
        assertTrue(prompt.contains("opensourcehealth_get_vulnerabilities"))
        assertFalse(prompt.contains("guardrails_quality_check"))
    }

    @Test
    fun mixedSelection_namesEachToolOnce() {
        val prompt = text(listOf(maintainability, security), plainAgent)
        assertTrue(prompt.contains("maintainability_get_findings"))
        assertTrue(prompt.contains("security_get_findings"))
        assertEquals(1, prompt.split("guardrails_quality_check").size - 1)
    }

    @Test
    fun neverAsksAgentToWriteStatusBack() {
        listOf(maintainability, security, osh).forEach { finding ->
            listOf(plainAgent, slashAgent).forEach { options ->
                assertFalse(text(listOf(finding), options).contains("update_finding_status"))
            }
        }
    }

    @Test
    fun resolveToolReference_rendersLinkedTools() {
        val options = FixPromptOptions(supportsSlashCommands = false, mcpDetected = true, resolveToolReference = { "#$it" })
        val prompt = text(listOf(maintainability), options)
        assertTrue(prompt.contains("#maintainability_get_findings"))
        assertTrue(prompt.contains("#guardrails_quality_check"))
    }

    @Test
    fun href_isRenderedAsReferenceLine() {
        val withHref = maintainability.copy(href = "https://sigrid-says.com/finding/1")
        val prompt = text(listOf(withHref), slashAgent)
        assertTrue(prompt.contains("Reference: https://sigrid-says.com/finding/1"))
    }

    @Test
    fun nullOrBlankHref_omitsReferenceLine() {
        assertFalse(text(listOf(maintainability), slashAgent).contains("Reference:"))
        assertFalse(text(listOf(maintainability.copy(href = "  ")), slashAgent).contains("Reference:"))
    }

    @Test
    fun noFileLocations_omitsLocationsLine() {
        val prompt = text(listOf(maintainability.copy(fileLocations = emptyList())), slashAgent)
        assertFalse(prompt.contains("Locations:"))
    }

    @Test
    fun findingListHeader_isPresent() {
        val prompt = text(listOf(maintainability), slashAgent)
        assertTrue(prompt.contains("Findings (from Sigrid - fix these, do not go looking for others):"))
    }
}
