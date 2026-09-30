package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.RiskSeverity

// GATE 1 of the Epic 432 prioritization pipeline (design doc section 3): promotes Open Source Health
// findings backed by a real vulnerability identifier and high/critical severity ahead of everything else
// in the non-Security/Reliability bucket (see PrioritizedFindingMapper.rankRemaining). Security and
// Reliability are unconditionally first regardless of severity per the epic author's confirmed product
// decision - that rule supersedes this gate for those two capabilities, so this gate only ever reorders
// within what's left (OSH and Maintainability).
//
// Known gap: the design doc's original criterion is "CVE/GHSA present + high severity + high
// exploitability (EPSS/KEV where available)". This plugin's OSH data has no exploitability signal at all -
// OshVulnerability only carries a CVSS-derived severity/score, not EPSS or KEV - so urgency here is judged
// on identifier + severity alone. Tightening this later needs Sigrid to expose exploitability data for OSH
// vulnerabilities, which it currently doesn't via the public API.
object UrgencyGate {
    private val URGENT_SEVERITIES = setOf(RiskSeverity.Critical, RiskSeverity.High)

    fun isUrgent(dependency: OpenSourceHealthDependency): Boolean =
        dependency.vulnerabilities.any { it.severity in URGENT_SEVERITIES && hasRealIdentifier(it.id) }

    private fun hasRealIdentifier(id: String): Boolean =
        id.startsWith("CVE-", ignoreCase = true) || id.startsWith("GHSA-", ignoreCase = true)

    // Findings' ids for OSH are purl-or-name (see OpenSourceHealthDependency.toPrioritizedFinding) - this
    // pre-computes the set of urgent ids once so PrioritizedFindingMapper can partition by simple lookup.
    fun urgentFindingIds(dependencies: List<OpenSourceHealthDependency>): Set<String> =
        dependencies.filter { isUrgent(it) }.map { it.purl ?: it.name }.toSet()
}
