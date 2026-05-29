package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*

object SecurityFindingMapper {
    fun map(response: List<SecurityFindingResponse>, subsystem: String): List<SecurityFinding> =
        response
            .filter { subsystem.isBlank() || it.component == subsystem }
            .map { create(it, subsystem) }
            .sortedWith(compareByDescending<SecurityFinding> { it.severity }.thenBy { it.displayFilePath })

    private fun create(r: SecurityFindingResponse, subsystem: String): SecurityFinding {
        val filePath = r.filePath ?: ""
        return SecurityFinding(
            id = r.id,
            href = r.href,
            severity = RiskSeverity.from(r.severity),
            filePath = filePath,
            displayFilePath = toDisplayFilePath(filePath),
            type = r.type,
            status = FindingStatus.from(r.status),
            statusLabel = snakeCaseToTitleCase(r.status),
            fileLocations = listOf(
                FileLocation(
                    component = r.component,
                    filePath = normalizePath(filePath, subsystem),
                    startLine = r.startLine,
                    endLine = r.endLine,
                )
            ),
        )
    }

}