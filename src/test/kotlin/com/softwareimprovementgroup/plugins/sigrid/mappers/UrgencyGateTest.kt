package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.DependencyType
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.OshVulnerability
import com.softwareimprovementgroup.plugins.sigrid.models.RiskSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrgencyGateTest {

    private fun makeVulnerability(id: String = "CVE-2024-1234", severity: RiskSeverity = RiskSeverity.Critical) =
        OshVulnerability(id = id, severity = severity, score = null, method = null)

    private fun makeDependency(
        name: String = "left-pad",
        purl: String? = "pkg:npm/left-pad@1.0.0",
        vulnerabilities: List<OshVulnerability> = emptyList(),
    ) = OpenSourceHealthDependency(
        name = name,
        displayName = name,
        version = "1.0.0",
        group = null,
        dependencyType = DependencyType.DIRECT,
        purl = purl,
        risk = RiskSeverity.Critical,
        licenseRisk = RiskSeverity.None,
        vulnerabilityRisk = RiskSeverity.Critical,
        freshnessRisk = RiskSeverity.None,
        activityRisk = RiskSeverity.None,
        stabilityRisk = RiskSeverity.None,
        managementRisk = RiskSeverity.None,
        fileLocations = emptyList(),
        href = null,
        vulnerabilities = vulnerabilities,
    )

    // isUrgent

    @Test
    fun isUrgent_criticalWithCve_isTrue() {
        val dep = makeDependency(vulnerabilities = listOf(makeVulnerability(id = "CVE-2024-1234", severity = RiskSeverity.Critical)))
        assertTrue(UrgencyGate.isUrgent(dep))
    }

    @Test
    fun isUrgent_highWithGhsa_isTrue() {
        val dep = makeDependency(vulnerabilities = listOf(makeVulnerability(id = "GHSA-abcd-1234", severity = RiskSeverity.High)))
        assertTrue(UrgencyGate.isUrgent(dep))
    }

    @Test
    fun isUrgent_mediumSeverity_isFalse() {
        val dep = makeDependency(vulnerabilities = listOf(makeVulnerability(id = "CVE-2024-1234", severity = RiskSeverity.Medium)))
        assertFalse(UrgencyGate.isUrgent(dep))
    }

    @Test
    fun isUrgent_criticalWithoutRealIdentifier_isFalse() {
        val dep = makeDependency(vulnerabilities = listOf(makeVulnerability(id = "internal-finding-1", severity = RiskSeverity.Critical)))
        assertFalse(UrgencyGate.isUrgent(dep))
    }

    @Test
    fun isUrgent_noVulnerabilities_isFalse() {
        assertFalse(UrgencyGate.isUrgent(makeDependency(vulnerabilities = emptyList())))
    }

    // urgentFindingIds

    @Test
    fun urgentFindingIds_usesPurlWhenPresent() {
        val dep = makeDependency(purl = "pkg:npm/left-pad@1.0.0", vulnerabilities = listOf(makeVulnerability()))
        assertEquals(setOf("pkg:npm/left-pad@1.0.0"), UrgencyGate.urgentFindingIds(listOf(dep)))
    }

    @Test
    fun urgentFindingIds_fallsBackToNameWhenNoPurl() {
        val dep = makeDependency(name = "left-pad", purl = null, vulnerabilities = listOf(makeVulnerability()))
        assertEquals(setOf("left-pad"), UrgencyGate.urgentFindingIds(listOf(dep)))
    }

    @Test
    fun urgentFindingIds_nonUrgentDependency_excluded() {
        assertTrue(UrgencyGate.urgentFindingIds(listOf(makeDependency(vulnerabilities = emptyList()))).isEmpty())
    }
}
