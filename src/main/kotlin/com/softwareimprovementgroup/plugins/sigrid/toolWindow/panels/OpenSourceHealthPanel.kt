package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.mappers.OpenSourceHealthMapper
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService

class OpenSourceHealthPanel(project: Project) : SigridPanel<OpenSourceHealthDependency>(
    project,
    arrayOf("Risk", "Library", "Version", "Transitive", "License", "Vulnerabilities", "Freshness", "Activity"),
) {
    override val emptyMessage = "No open source health findings found."

    override fun fetch(subsystem: String): List<OpenSourceHealthDependency> =
        OpenSourceHealthMapper.map(SigridApiService.getInstance().getOpenSourceHealthFindings(project), subsystem)

    override fun OpenSourceHealthDependency.matchesSearch(query: String) =
        displayName.contains(query, ignoreCase = true) ||
        version.contains(query, ignoreCase = true) ||
        dependencyType.contains(query, ignoreCase = true)

    override fun OpenSourceHealthDependency.toRow(): Array<String> = arrayOf(
        risk.name,
        displayName,
        version,
        dependencyType,
        licenseRisk.name,
        vulnerabilityRisk.name,
        freshnessRisk.name,
        activityRisk.name,
    )
}