package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.mappers.OpenSourceHealthMapper
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService

class OpenSourceHealthPanel(project: Project) : SigridPanel<OpenSourceHealthDependency>(
    project,
    arrayOf(SigridBundle["column.risk"], SigridBundle["column.library"], SigridBundle["column.version"], SigridBundle["column.transitive"], SigridBundle["column.license"], SigridBundle["column.vulnerabilities"], SigridBundle["column.freshness"], SigridBundle["column.activity"]),
    setOf(SigridBundle["column.risk"], SigridBundle["column.transitive"], SigridBundle["column.license"], SigridBundle["column.vulnerabilities"], SigridBundle["column.freshness"], SigridBundle["column.activity"]),
) {
    override val emptyMessage = SigridBundle["osh.empty"]

    override fun fetch(subsystem: String): List<OpenSourceHealthDependency> =
        OpenSourceHealthMapper.map(SigridApiService.getInstance().getOpenSourceHealthFindings(project), subsystem)

    override fun OpenSourceHealthDependency.matchesSearch(query: String) =
        displayName.contains(query, ignoreCase = true) ||
        version.contains(query, ignoreCase = true) ||
        dependencyType.contains(query, ignoreCase = true)

    override fun OpenSourceHealthDependency.toRow(): Array<Any> = arrayOf(
        risk.toRiskIcon(),
        displayName,
        version,
        dependencyType,
        licenseRisk.toRiskIcon(),
        vulnerabilityRisk.toRiskIcon(),
        freshnessRisk.toRiskIcon(),
        activityRisk.toRiskIcon(),
    )

    override fun OpenSourceHealthDependency.getFileLocations(): List<FileLocation> = fileLocations
}