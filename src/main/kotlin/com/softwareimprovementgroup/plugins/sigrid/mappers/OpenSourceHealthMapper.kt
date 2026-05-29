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
        if (response.components.isEmpty()) return emptyList()
        return response.components
            .map { create(it, subsystem) }
            .filter { subsystem.isBlank() || it.fileLocations.any { loc -> loc.component == subsystem } }
            .sortedWith(compareByDescending<OpenSourceHealthDependency> { it.risk }.thenBy { it.displayName })
    }

    private fun create(component: OshDependencyResponse, subsystem: String): OpenSourceHealthDependency {
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
            displayName = if (component.group.isNotBlank()) "${component.group}/${component.name}" else component.name,
            version = component.version,
            group = component.group,
            dependencyType = props[DEPENDENCY_TYPE_KEY]?.let { snakeCaseToTitleCase(it) } ?: "Unknown",
            purl = component.purl,
            risk = overallRisk,
            licenseRisk = licenseRisk,
            vulnerabilityRisk = vulnerabilityRisk,
            freshnessRisk = freshnessRisk,
            activityRisk = activityRisk,
            stabilityRisk = stabilityRisk,
            managementRisk = managementRisk,
            fileLocations = fileLocations,
        )
    }

}