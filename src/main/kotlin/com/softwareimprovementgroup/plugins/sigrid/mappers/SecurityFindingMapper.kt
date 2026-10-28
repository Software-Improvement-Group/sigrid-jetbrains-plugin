package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*

object SecurityFindingMapper {
    fun map(response: List<SecurityFindingResponse>, subsystem: String): List<SecurityFinding> =
        response
            .filter { subsystem.isBlank() || it.component == subsystem }
            .let(::deduplicate)
            .map { create(it, subsystem) }
            .sortedWith(compareByDescending<SecurityFinding> { it.severity }.thenBy { it.displayFilePath })

    // The API sometimes returns the same finding once per analysis date: identical location and rule, but a
    // different id, href and first-seen date. Status and remark stay out of the key so a triaged copy still
    // matches its untriaged twins - and wins, so the user's triage isn't hidden behind a fresh RAW entry.
    private data class DuplicateKey(
        val component: String,
        val filePath: String?,
        val startLine: Int,
        val endLine: Int,
        val ruleId: String?,
        val type: String,
        val cweId: String,
        val severity: String,
    )

    // Deterministic, so a refresh keeps showing the same id (selection restore and edit write-back rely on it).
    private val representativeOrder = compareBy<SecurityFindingResponse>({ !it.isTriaged() }, { it.firstSeenAnalysisDate }, { it.id })

    private fun SecurityFindingResponse.isTriaged() = FindingStatus.from(status) != FindingStatus.Raw || !remark.isNullOrBlank()

    private fun SecurityFindingResponse.duplicateKey() = DuplicateKey(component, filePath, startLine, endLine, ruleId, type, cweId, severity)

    private fun deduplicate(responses: List<SecurityFindingResponse>): List<SecurityFindingResponse> =
        responses.groupBy { it.duplicateKey() }.values.map { it.minWith(representativeOrder) }

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
            statusLabel = "${FindingStatus.from(r.status).icon} ${snakeCaseToTitleCase(r.status)}",
            remark = r.remark ?: "",
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