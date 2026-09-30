package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.DependencyType
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.OshVulnerability

// DE-DUPLICATE stage of the Epic 432 prioritization pipeline (see design doc section 3). Two distinct
// duplication problems live in the same OSH data: the same CVE listed more than once within one
// dependency's own vulnerability list, and the same library listed multiple times under different
// version constraints.
object PriorityDeduplicator {

    // A single OSH finding's own vulnerability list can list the same CVE more than once - confirmed in
    // the design doc: one real dependency listed ~125 CVE entries but only 94 were unique. Collapse by
    // CVE id, keeping whichever duplicate reports the worst severity.
    fun deduplicateVulnerabilities(vulnerabilities: List<OshVulnerability>): List<OshVulnerability> =
        vulnerabilities
            .groupBy { it.id }
            .map { (_, duplicates) -> duplicates.maxBy { it.severity.ordinal } }

    // The same library can appear multiple times under different version constraints
    // (e.g. apache-airflow >=3.0.0, >=3.0.1, >=3.0.2) - collapse by library identity (group + name), not
    // by finding id, or Gate 1's urgency override would see the same real vulnerability multiple times.
    fun collapseByLibraryName(dependencies: List<OpenSourceHealthDependency>): List<OpenSourceHealthDependency> =
        dependencies
            .groupBy { it.group.orEmpty() to it.name }
            .map { (_, variants) -> collapse(variants) }

    private fun collapse(variants: List<OpenSourceHealthDependency>): OpenSourceHealthDependency {
        if (variants.size == 1) return variants[0]
        return variants[0].copy(
            version = variants.map { it.version }.distinct().sorted().joinToString(", "),
            dependencyType = collapsedDependencyType(variants),
            risk = variants.maxOf { it.risk },
            licenseRisk = variants.maxOf { it.licenseRisk },
            vulnerabilityRisk = variants.maxOf { it.vulnerabilityRisk },
            freshnessRisk = variants.maxOf { it.freshnessRisk },
            activityRisk = variants.maxOf { it.activityRisk },
            stabilityRisk = variants.maxOf { it.stabilityRisk },
            managementRisk = variants.maxOf { it.managementRisk },
            fileLocations = variants.flatMap { it.fileLocations }.distinct(),
            href = variants.firstNotNullOfOrNull { it.href },
            vulnerabilities = deduplicateVulnerabilities(variants.flatMap { it.vulnerabilities }),
        )
    }

    // Knowing a library is used directly anywhere is the more actionable signal, so DIRECT wins over
    // TRANSITIVE if any version-constraint variant reports it; only falls through to UNKNOWN when every
    // variant is UNKNOWN.
    private fun collapsedDependencyType(variants: List<OpenSourceHealthDependency>): DependencyType = when {
        variants.any { it.dependencyType == DependencyType.DIRECT } -> DependencyType.DIRECT
        variants.any { it.dependencyType == DependencyType.TRANSITIVE } -> DependencyType.TRANSITIVE
        else -> DependencyType.UNKNOWN
    }
}
