package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.intellij.openapi.diagnostic.thisLogger
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding

// TEMPORARY diagnostics: logs which files each Prioritized-tab filter removed and why. Delete this file and its
// call sites (grep "TempFilterLog") once the filtering has been investigated.
object TempFilterLog {
    fun log(stage: String, reason: String, findings: List<PrioritizedFinding>) {
        if (findings.isEmpty()) return
        val lines = findings.joinToString("\n") { "  - ${describe(it)}" }
        thisLogger().info("[TEMP] $stage filtered ${findings.size} finding(s) - $reason:\n$lines")
    }

    private fun describe(finding: PrioritizedFinding): String {
        val files = finding.fileLocations.map { it.filePath }.filter { it.isNotBlank() }.distinct()
        return "${finding.capability.label} ${finding.id}: ${files.ifEmpty { listOf("<no file>") }.joinToString(", ")}"
    }
}