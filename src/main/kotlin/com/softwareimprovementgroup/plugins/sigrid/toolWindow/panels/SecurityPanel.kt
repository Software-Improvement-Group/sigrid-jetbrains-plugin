package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.mappers.SecurityFindingMapper
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.SecurityFinding
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService

class SecurityPanel(project: Project) : SigridPanel<SecurityFinding>(
    project,
    arrayOf(SigridBundle["column.risk"], SigridBundle["column.location"], SigridBundle["column.description"], SigridBundle["column.status"]),
    setOf(SigridBundle["column.risk"], SigridBundle["column.status"])
) {
    override val emptyMessage = SigridBundle["security.empty"]

    override fun fetch(subsystem: String): List<SecurityFinding> =
        SecurityFindingMapper.map(SigridApiService.getInstance().getSecurityFindings(project), subsystem)

    override fun SecurityFinding.matchesSearch(query: String) =
        displayFilePath.contains(query, ignoreCase = true) ||
        type.contains(query, ignoreCase = true) ||
        statusLabel.contains(query, ignoreCase = true)

    override fun SecurityFinding.toRow(): Array<Any> = arrayOf(
        severity.toRiskIcon(),
        fileLocations.firstOrNull()?.let { loc ->
            if (loc.startLine != null && loc.startLine > 0) "${loc.filePath}:${loc.startLine}" else loc.filePath
        } ?: displayFilePath,
        type,
        statusLabel,
    )

    override fun SecurityFinding.getFileLocations(): List<FileLocation> = fileLocations
}