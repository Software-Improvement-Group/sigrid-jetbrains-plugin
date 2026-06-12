package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.mappers.OpenSourceHealthMapper
import com.softwareimprovementgroup.plugins.sigrid.models.DependencyType
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthDependency
import com.softwareimprovementgroup.plugins.sigrid.models.RiskSeverity
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService

class OpenSourceHealthPanel(project: Project) : SigridPanel<OpenSourceHealthDependency>(
    project,
    arrayOf(SigridBundle["column.risk"], SigridBundle["column.library"], SigridBundle["column.version"], SigridBundle["column.transitive"], SigridBundle["column.license"], SigridBundle["column.vulnerabilities"], SigridBundle["column.freshness"], SigridBundle["column.activity"]),
    centeredColumns = setOf(SigridBundle["column.risk"], SigridBundle["column.transitive"], SigridBundle["column.license"], SigridBundle["column.vulnerabilities"], SigridBundle["column.freshness"], SigridBundle["column.activity"]),
    columnFilters = listOf(
        ColumnFilterDef(
            columnName = SigridBundle["column.risk"],
            options = RiskSeverity.entries.map { FilterOption(it.toRiskIcon().label, it.name) },
            getOptionId = { risk.name },
        ),
        ColumnFilterDef(
            columnName = SigridBundle["column.transitive"],
            options = DependencyType.entries.map { FilterOption(it.label, it.name) },
            getOptionId = { dependencyType.name },
        ),
        ColumnFilterDef(
            columnName = SigridBundle["column.license"],
            options = RiskSeverity.entries.map { FilterOption(it.toRiskIcon().label, it.name) },
            getOptionId = { licenseRisk.name },
        ),
        ColumnFilterDef(
            columnName = SigridBundle["column.vulnerabilities"],
            options = RiskSeverity.entries.map { FilterOption(it.toRiskIcon().label, it.name) },
            getOptionId = { vulnerabilityRisk.name },
        ),
        ColumnFilterDef(
            columnName = SigridBundle["column.freshness"],
            options = RiskSeverity.entries.map { FilterOption(it.toRiskIcon().label, it.name) },
            getOptionId = { freshnessRisk.name },
        ),
        ColumnFilterDef(
            columnName = SigridBundle["column.activity"],
            options = RiskSeverity.entries.map { FilterOption(it.toRiskIcon().label, it.name) },
            getOptionId = { activityRisk.name },
        ),
    ),
) {
    override val emptyMessage = SigridBundle["osh.empty"]

    override fun fetch(subsystem: String): List<OpenSourceHealthDependency> =
        OpenSourceHealthMapper.map(SigridApiService.getInstance().getOpenSourceHealthFindings(project), subsystem)

    override fun OpenSourceHealthDependency.matchesSearch(query: String) =
        displayName.contains(query, ignoreCase = true) ||
        version.contains(query, ignoreCase = true) ||
        dependencyType.label.contains(query, ignoreCase = true)

    override fun OpenSourceHealthDependency.toRow(): Array<Any> = arrayOf(
        risk.toRiskIcon(),
        displayName,
        version,
        dependencyType.label,
        licenseRisk.toRiskIcon(),
        vulnerabilityRisk.toRiskIcon(),
        freshnessRisk.toRiskIcon(),
        activityRisk.toRiskIcon(),
    )

    override fun OpenSourceHealthDependency.getFileLocations(): List<FileLocation> = fileLocations
}