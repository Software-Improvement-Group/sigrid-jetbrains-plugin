package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.DependencyType
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.OshVulnerability
import com.softwareimprovementgroup.plugins.sigrid.models.RiskSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PriorityDeduplicatorTest {

    private fun makeVulnerability(id: String, severity: RiskSeverity, score: Double? = null) =
        OshVulnerability(id = id, severity = severity, score = score, method = "CVSSv3")

    private fun makeDependency(
        name: String = "apache-airflow",
        group: String? = null,
        version: String = "1.0.0",
        risk: RiskSeverity = RiskSeverity.Low,
        dependencyType: DependencyType = DependencyType.TRANSITIVE,
        fileLocations: List<FileLocation> = emptyList(),
        href: String? = null,
        vulnerabilities: List<OshVulnerability> = emptyList(),
    ) = OpenSourceHealthDependency(
        name = name,
        displayName = name,
        version = version,
        group = group,
        dependencyType = dependencyType,
        purl = "pkg:pypi/$name@$version",
        risk = risk,
        licenseRisk = RiskSeverity.None,
        vulnerabilityRisk = risk,
        freshnessRisk = RiskSeverity.None,
        activityRisk = RiskSeverity.None,
        stabilityRisk = RiskSeverity.None,
        managementRisk = RiskSeverity.None,
        fileLocations = fileLocations,
        href = href,
        vulnerabilities = vulnerabilities,
    )

    // deduplicateVulnerabilities

    @Test
    fun deduplicateVulnerabilities_distinctIds_allKept() {
        val result = PriorityDeduplicator.deduplicateVulnerabilities(
            listOf(makeVulnerability("CVE-1", RiskSeverity.Low), makeVulnerability("CVE-2", RiskSeverity.High)),
        )
        assertEquals(2, result.size)
    }

    @Test
    fun deduplicateVulnerabilities_duplicateId_collapsedToOne() {
        val result = PriorityDeduplicator.deduplicateVulnerabilities(
            listOf(makeVulnerability("CVE-1", RiskSeverity.Low), makeVulnerability("CVE-1", RiskSeverity.Low)),
        )
        assertEquals(1, result.size)
    }

    @Test
    fun deduplicateVulnerabilities_duplicateId_keepsWorstSeverity() {
        val result = PriorityDeduplicator.deduplicateVulnerabilities(
            listOf(makeVulnerability("CVE-1", RiskSeverity.Low), makeVulnerability("CVE-1", RiskSeverity.Critical)),
        )
        assertEquals(RiskSeverity.Critical, result[0].severity)
    }

    @Test
    fun deduplicateVulnerabilities_empty_returnsEmpty() {
        assertTrue(PriorityDeduplicator.deduplicateVulnerabilities(emptyList()).isEmpty())
    }

    // collapseByLibraryName

    @Test
    fun collapseByLibraryName_distinctLibraries_bothKept() {
        val result = PriorityDeduplicator.collapseByLibraryName(listOf(makeDependency(name = "a"), makeDependency(name = "b")))
        assertEquals(2, result.size)
    }

    @Test
    fun collapseByLibraryName_sameNameDifferentVersionConstraints_collapsedToOne() {
        val variants = listOf(
            makeDependency(name = "apache-airflow", version = ">=3.0.0"),
            makeDependency(name = "apache-airflow", version = ">=3.0.1"),
            makeDependency(name = "apache-airflow", version = ">=3.0.2"),
        )
        val result = PriorityDeduplicator.collapseByLibraryName(variants)
        assertEquals(1, result.size)
    }

    @Test
    fun collapseByLibraryName_differentGroupsSameName_notCollapsed() {
        val variants = listOf(
            makeDependency(name = "core", group = "com.a"),
            makeDependency(name = "core", group = "com.b"),
        )
        val result = PriorityDeduplicator.collapseByLibraryName(variants)
        assertEquals(2, result.size)
    }

    @Test
    fun collapseByLibraryName_versionsJoinedInResult() {
        val variants = listOf(
            makeDependency(name = "lib", version = ">=1.0"),
            makeDependency(name = "lib", version = ">=2.0"),
        )
        val result = PriorityDeduplicator.collapseByLibraryName(variants)
        assertEquals(">=1.0, >=2.0", result[0].version)
    }

    @Test
    fun collapseByLibraryName_risksCombinedByMax() {
        val variants = listOf(
            makeDependency(name = "lib", risk = RiskSeverity.Low),
            makeDependency(name = "lib", risk = RiskSeverity.Critical),
        )
        val result = PriorityDeduplicator.collapseByLibraryName(variants)
        assertEquals(RiskSeverity.Critical, result[0].risk)
    }

    @Test
    fun collapseByLibraryName_dependencyType_directWinsOverTransitive() {
        val variants = listOf(
            makeDependency(name = "lib", dependencyType = DependencyType.TRANSITIVE),
            makeDependency(name = "lib", dependencyType = DependencyType.DIRECT),
        )
        val result = PriorityDeduplicator.collapseByLibraryName(variants)
        assertEquals(DependencyType.DIRECT, result[0].dependencyType)
    }

    @Test
    fun collapseByLibraryName_dependencyType_allUnknown_staysUnknown() {
        val variants = listOf(
            makeDependency(name = "lib", dependencyType = DependencyType.UNKNOWN),
            makeDependency(name = "lib", dependencyType = DependencyType.UNKNOWN),
        )
        val result = PriorityDeduplicator.collapseByLibraryName(variants)
        assertEquals(DependencyType.UNKNOWN, result[0].dependencyType)
    }

    @Test
    fun collapseByLibraryName_fileLocationsUnioned() {
        val variants = listOf(
            makeDependency(name = "lib", fileLocations = listOf(FileLocation("svc", "svc/a/pom.xml"))),
            makeDependency(name = "lib", fileLocations = listOf(FileLocation("svc", "svc/b/pom.xml"))),
        )
        val result = PriorityDeduplicator.collapseByLibraryName(variants)
        assertEquals(2, result[0].fileLocations.size)
    }

    @Test
    fun collapseByLibraryName_vulnerabilitiesMergedAndDeduped() {
        val variants = listOf(
            makeDependency(name = "lib", vulnerabilities = listOf(makeVulnerability("CVE-1", RiskSeverity.Low))),
            makeDependency(name = "lib", vulnerabilities = listOf(makeVulnerability("CVE-1", RiskSeverity.Critical), makeVulnerability("CVE-2", RiskSeverity.Medium))),
        )
        val result = PriorityDeduplicator.collapseByLibraryName(variants)
        val byId = result[0].vulnerabilities.associateBy { it.id }
        assertEquals(2, result[0].vulnerabilities.size)
        assertEquals(RiskSeverity.Critical, byId["CVE-1"]?.severity)
    }

    @Test
    fun collapseByLibraryName_hrefFallsBackToFirstNonNull() {
        val variants = listOf(
            makeDependency(name = "lib", href = null),
            makeDependency(name = "lib", href = "https://example.com/lib"),
        )
        val result = PriorityDeduplicator.collapseByLibraryName(variants)
        assertEquals("https://example.com/lib", result[0].href)
    }

    @Test
    fun collapseByLibraryName_empty_returnsEmpty() {
        assertTrue(PriorityDeduplicator.collapseByLibraryName(emptyList()).isEmpty())
    }
}
