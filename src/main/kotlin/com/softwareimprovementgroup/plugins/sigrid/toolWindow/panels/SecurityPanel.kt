package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.mappers.SecurityFindingMapper
import com.softwareimprovementgroup.plugins.sigrid.models.SecurityFinding
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService

class SecurityPanel(project: Project) : SigridPanel<SecurityFinding>(project, arrayOf("Risk", "Location", "Description", "Status")) {
    override val emptyMessage = "No security findings found."

    override fun fetch(subsystem: String): List<SecurityFinding> =
        SecurityFindingMapper.map(SigridApiService.getInstance().getSecurityFindings(project), subsystem)

    override fun SecurityFinding.toRow(): Array<String> = arrayOf(
        severity.name,
        fileLocations.firstOrNull()?.let { loc ->
            if (loc.startLine != null && loc.startLine > 0) "${loc.filePath}:${loc.startLine}" else loc.filePath
        } ?: displayFilePath,
        type,
        statusLabel,
    )
}