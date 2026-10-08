package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.intellij.openapi.diagnostic.thisLogger
import com.softwareimprovementgroup.plugins.sigrid.models.*

object ArchitectureQualityMapper {
    private const val FILE_ELEMENT_TYPE = "FILE"
    private const val CHURN_METRIC = "CHURN"
    private const val COMMITS_METRIC = "COMMITS"

    // No subsystem filter here: unlike Maintainability/Security, Architecture Quality's FILE elements carry
    // no component field to filter on (see FileActivity's doc comment) - subsystem scoping happens naturally
    // when the caller joins this against already subsystem-filtered findings by file path.
    fun map(response: ArchitectureQualityRawResponse, subsystem: String): FileActivityData {
        val fileActivities = response.systemElements
            .filter { it.type == FILE_ELEMENT_TYPE }
            .mapNotNull { create(it, subsystem) }

        logShape(response, fileActivities)
        return FileActivityData(
            hasHistory = response.metadata.historyStartDate != null,
            historyStartDate = response.metadata.historyStartDate,
            historyEndDate = response.metadata.historyEndDate,
            fileActivities = fileActivities,
        )
    }

    // Elements with neither a CHURN nor a COMMITS value have no activity data (as opposed to zero activity) -
    // excluded here rather than defaulted to zero, so Gate 2 can tell the two cases apart later.
    private fun create(element: SystemElementResponse, subsystem: String): FileActivity? {
        val churn = element.measurementValues?.get(CHURN_METRIC)
        val commits = element.measurementValues?.get(COMMITS_METRIC)
        if (churn == null && commits == null) return null
        return FileActivity(normalizePath(element.name, subsystem), churn ?: 0.0, commits ?: 0.0)
    }

    // Diagnostic for "why does the activity gate see no churn data": shows what the export actually contained
    // versus what the mapper managed to extract.
    private fun logShape(response: ArchitectureQualityRawResponse, fileActivities: List<FileActivity>) {
        val fileCount = response.systemElements.count { it.type == FILE_ELEMENT_TYPE }
        thisLogger().info(
            "Architecture Quality export: $fileCount FILE elements, ${fileActivities.size} with activity data; " +
                "${fileActivities.count { it.hasActivity }} active ($CHURN_METRIC or $COMMITS_METRIC > 0); " +
                "history=${response.metadata.historyStartDate}..${response.metadata.historyEndDate}, " +
                "commits=${response.metadata.historyCommitCount}",
        )
    }
}
