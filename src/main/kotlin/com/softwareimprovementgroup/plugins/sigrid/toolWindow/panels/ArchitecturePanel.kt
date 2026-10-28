package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.mappers.ArchitectureFindingMapper
import com.softwareimprovementgroup.plugins.sigrid.models.ArchitectureFinding
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.FixItContext
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import com.softwareimprovementgroup.plugins.sigrid.models.RiskSeverity
import com.softwareimprovementgroup.plugins.sigrid.models.toSeverityEmoji
import com.softwareimprovementgroup.plugins.sigrid.promptBuilders.FindingCategory
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration

class ArchitecturePanel(project: Project) : SigridPanel<ArchitectureFinding>(
    project,
    arrayOf(SigridBundle["column.risk"], SigridBundle["column.description"], SigridBundle["column.location"]),
    centeredColumns = setOf(SigridBundle["column.risk"]),
    columnFilters = listOf(
        ColumnFilterDef(
            columnName = SigridBundle["column.risk"],
            options = RiskSeverity.entries.map { FilterOption(it.toRiskIcon().label, it.name) },
            getOptionId = { severity.name },
        ),
    ),
    columnMaxWidths = mapOf(SigridBundle["column.risk"] to 80),
) {
    override val emptyMessage = SigridBundle["architecture.empty"]

    override fun fetch(subsystem: String): List<ArchitectureFinding> =
        ArchitectureFindingMapper.map(SigridApiService.getInstance().getArchitectureQualityRaw(project), subsystem)

    override fun ArchitectureFinding.matchesSearch(query: String) =
        description.contains(query, ignoreCase = true) ||
        displayFilePath.contains(query, ignoreCase = true) ||
        sourceComponent.contains(query, ignoreCase = true) ||
        targetComponent.contains(query, ignoreCase = true)

    override fun ArchitectureFinding.toRow(): Array<Any> = arrayOf(
        severity.toRiskIcon(),
        description,
        displayLocation(),
    )

    override fun ArchitectureFinding.getFileLocations(): List<FileLocation> = fileLocations

    override fun ArchitectureFinding.toIssueFinding() = IssueFinding(
        title = description,
        severityEmoji = severity.toSeverityEmoji(),
        fileLocations = fileLocations,
    )

    override fun ArchitectureFinding.toFixItContext() = FixItContext(
        category = FindingCategory.ARCHITECTURE,
        severity = severity.toRiskIcon().label,
        title = "${displayLocation()}: $description",
        fileLocations = fileLocations,
        href = explorerUrl(),
    )

    override fun ArchitectureFinding.getDisplayLocation() = displayLocation()
    override fun ArchitectureFinding.getHref() = explorerUrl()

    // Placeholder until Sigrid offers a URL per architecture finding: link to the architecture explorer.
    private fun explorerUrl(): String {
        val config = SigridProjectConfiguration.getInstance(project)
        return "${config.effectiveSigridUrl}/${config.effectiveCustomer}/${config.system}/-/architecture-quality/explorer"
    }

    private fun ArchitectureFinding.displayLocation() = if (line > 0) "$displayFilePath:$line" else displayFilePath
}
