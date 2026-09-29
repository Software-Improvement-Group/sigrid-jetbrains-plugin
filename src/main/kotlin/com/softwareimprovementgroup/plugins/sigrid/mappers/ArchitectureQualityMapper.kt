package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*

object ArchitectureQualityMapper {
    private const val FILE_ELEMENT_TYPE = "FILE"
    private const val CHURN_METRIC = "CHURN"

    // No subsystem filter here: unlike Maintainability/Security, Architecture Quality's FILE elements carry
    // no component field to filter on (see FileActivity's doc comment) - subsystem scoping happens naturally
    // when the caller joins this against already subsystem-filtered findings by file path.
    fun map(response: ArchitectureQualityRawResponse, subsystem: String): FileActivityData {
        val fileActivities = response.systemElements
            .filter { it.type == FILE_ELEMENT_TYPE }
            .mapNotNull { create(it, subsystem) }

        return FileActivityData(
            hasHistory = response.metadata.historyStartDate != null,
            historyStartDate = response.metadata.historyStartDate,
            historyEndDate = response.metadata.historyEndDate,
            fileActivities = fileActivities,
        )
    }

    // Elements with no CHURN time series at all have no activity data (as opposed to zero activity) -
    // excluded here rather than defaulted to zero, so Gate 2 can tell the two cases apart later.
    private fun create(element: SystemElementResponse, subsystem: String): FileActivity? {
        val churn = element.measurementTimeSeries?.get(CHURN_METRIC) ?: return null
        return FileActivity(
            filePath = normalizePath(element.name, subsystem),
            averageChurn = churn.averageValue ?: 0.0,
            churnByPeriod = churn.dataPoints.orEmpty(),
        )
    }
}
