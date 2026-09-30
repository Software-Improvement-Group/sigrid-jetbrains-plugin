package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*

object OpenSourceHealthMapper {
    private const val DEPENDENCY_TYPE_KEY = "sigrid:transitive"
    private const val VULNERABILITY_RISK_KEY = "sigrid:risk:vulnerability"
    private const val LICENSE_RISK_KEY = "sigrid:risk:legal"
    private const val FRESHNESS_RISK_KEY = "sigrid:risk:freshness"
    private const val ACTIVITY_RISK_KEY = "sigrid:risk:activity"
    private const val STABILITY_RISK_KEY = "sigrid:risk:stability"
    private const val MANAGEMENT_RISK_KEY = "sigrid:risk:management"

    fun map(response: OpenSourceHealthResponse, subsystem: String): List<OpenSourceHealthDependency> {
        val components = response.components.orEmpty()
        if (components.isEmpty()) return emptyList()
        val vulnerabilitiesByRef = groupVulnerabilitiesByRef(response.vulnerabilities.orEmpty())
        return components
            .map { create(it, subsystem, vulnerabilitiesByRef) }
            .filter { subsystem.isBlank() || it.fileLocations.any { loc -> loc.component == subsystem } }
            .sortedWith(compareByDescending<OpenSourceHealthDependency> { it.risk }.thenBy { it.displayName })
    }

    // A vulnerability's `affects` list names the components it applies to by their bom-ref; index by ref
    // once per response so each component can look its vulnerabilities up in O(1) instead of re-scanning.
    private fun groupVulnerabilitiesByRef(vulnerabilities: List<OshVulnerabilityResponse>): Map<String, List<OshVulnerabilityResponse>> =
        vulnerabilities
            .flatMap { vuln -> vuln.affects.orEmpty().map { it.ref to vuln } }
            .groupBy({ it.first }, { it.second })

    private fun mapVulnerabilities(
        component: OshDependencyResponse,
        vulnerabilitiesByRef: Map<String, List<OshVulnerabilityResponse>>,
    ): List<OshVulnerability> {
        val ref = component.bomRef ?: component.purl ?: return emptyList()
        val mapped = vulnerabilitiesByRef[ref].orEmpty().map(::mapVulnerability)
        return PriorityDeduplicator.deduplicateVulnerabilities(mapped)
    }

    // A single CVE can carry ratings from multiple scoring methods (CVSSv2, CVSSv3, ...); take the worst
    // one, same "max wins" approach as the six OSH risk dimensions above.
    private fun mapVulnerability(vulnerability: OshVulnerabilityResponse): OshVulnerability {
        val worstRating = vulnerability.ratings.orEmpty().maxByOrNull { RiskSeverity.from(it.severity).ordinal }
        return OshVulnerability(
            id = vulnerability.id,
            severity = RiskSeverity.from(worstRating?.severity),
            score = worstRating?.score,
            method = worstRating?.method,
        )
    }

    private fun create(
        component: OshDependencyResponse,
        subsystem: String,
        vulnerabilitiesByRef: Map<String, List<OshVulnerabilityResponse>>,
    ): OpenSourceHealthDependency {
        val props = component.properties.associate { it.name to it.value }
        val licenseRisk       = RiskSeverity.from(props[LICENSE_RISK_KEY])
        val vulnerabilityRisk = RiskSeverity.from(props[VULNERABILITY_RISK_KEY])
        val freshnessRisk     = RiskSeverity.from(props[FRESHNESS_RISK_KEY])
        val activityRisk      = RiskSeverity.from(props[ACTIVITY_RISK_KEY])
        val stabilityRisk     = RiskSeverity.from(props[STABILITY_RISK_KEY])
        val managementRisk    = RiskSeverity.from(props[MANAGEMENT_RISK_KEY])
        val overallRisk = maxOf(licenseRisk, vulnerabilityRisk, freshnessRisk, activityRisk, stabilityRisk, managementRisk)

        val fileLocations = component.evidence?.occurrences
            ?.mapNotNull { it.location }
            ?.map { location ->
                FileLocation(
                    component = location.substringBefore("/"),
                    filePath = normalizePath(location, subsystem),
                )
            }
            ?.filter { subsystem.isBlank() || it.component == subsystem }
            ?: emptyList()

        return OpenSourceHealthDependency(
            name = component.name,
            displayName = if (!component.group.isNullOrBlank()) "${component.group}/${component.name}" else component.name,
            version = component.version,
            group = component.group,
            dependencyType = DependencyType.from(props[DEPENDENCY_TYPE_KEY]),
            purl = component.purl,
            risk = overallRisk,
            licenseRisk = licenseRisk,
            vulnerabilityRisk = vulnerabilityRisk,
            freshnessRisk = freshnessRisk,
            activityRisk = activityRisk,
            stabilityRisk = stabilityRisk,
            managementRisk = managementRisk,
            fileLocations = fileLocations,
            href = component.externalReferences
                ?.firstOrNull { it.type == "website" && !it.url.isNullOrEmpty() }
                ?.url,
            vulnerabilities = mapVulnerabilities(component, vulnerabilitiesByRef),
        )
    }

}