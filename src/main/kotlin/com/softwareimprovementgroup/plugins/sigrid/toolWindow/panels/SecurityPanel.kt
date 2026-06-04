package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.mappers.SecurityFindingMapper
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.FindingStatus
import com.softwareimprovementgroup.plugins.sigrid.models.SecurityFinding
import com.softwareimprovementgroup.plugins.sigrid.models.snakeCaseToTitleCase
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
        displayFilePath,
        type,
        statusLabel,
    )

    override fun SecurityFinding.getFileLocations(): List<FileLocation> = fileLocations

    override fun SecurityFinding.isEditable() = true
    override fun SecurityFinding.getId() = id
    override fun SecurityFinding.getEditDescription() = type
    override fun SecurityFinding.getStatusOptions() = FindingStatus.entries.map { "${it.icon} ${snakeCaseToTitleCase(it.apiValue)}" to it.apiValue }
    override fun SecurityFinding.getCurrentStatus() = status.apiValue
    override fun SecurityFinding.getCurrentRemark() = remark
}