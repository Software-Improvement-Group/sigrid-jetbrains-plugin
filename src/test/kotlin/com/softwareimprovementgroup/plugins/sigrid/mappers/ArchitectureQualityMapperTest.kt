package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArchitectureQualityMapperTest {

    private fun makeElement(
        name: String = "svc/Foo.kt",
        type: String = "FILE",
        churn: MeasurementTimeSeriesResponse? = MeasurementTimeSeriesResponse(dataPoints = mapOf("2026-01-05" to 10.0), averageValue = 10.0),
    ) = SystemElementResponse(
        id = "id-$name",
        name = name,
        type = type,
        measurementTimeSeries = churn?.let { mapOf("CHURN" to it) },
    )

    private fun makeResponse(
        elements: List<SystemElementResponse>,
        historyStartDate: String? = "2026-01-01",
        historyEndDate: String? = "2026-06-01",
    ) = ArchitectureQualityRawResponse(
        metadata = ArchitectureQualityMetadataResponse(
            historyInterval = "WEEK",
            historyStartDate = historyStartDate,
            historyEndDate = historyEndDate,
            historyCommitCount = 12,
        ),
        systemElements = elements,
    )

    @Test
    fun map_noElements_returnsEmptyFileActivities() {
        val result = ArchitectureQualityMapper.map(makeResponse(emptyList()), "")
        assertTrue(result.fileActivities.isEmpty())
    }

    @Test
    fun map_nonFileElement_excluded() {
        val component = makeElement(name = "svc", type = "CODE_COMPONENT")
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(component)), "")
        assertTrue(result.fileActivities.isEmpty())
    }

    @Test
    fun map_fileElementWithChurn_included() {
        val file = makeElement(name = "svc/Foo.kt")
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "")
        assertEquals(1, result.fileActivities.size)
    }

    @Test
    fun map_fileElementWithoutChurnMetric_excluded() {
        val file = makeElement(name = "svc/Foo.kt", churn = null)
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "")
        assertTrue(result.fileActivities.isEmpty())
    }

    @Test
    fun map_averageChurn_mapped() {
        val file = makeElement(name = "svc/Foo.kt", churn = MeasurementTimeSeriesResponse(dataPoints = emptyMap(), averageValue = 42.5))
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "")
        assertEquals(42.5, result.fileActivities[0].averageChurn)
    }

    @Test
    fun map_churnByPeriod_preservesDataPoints() {
        val dataPoints = mapOf("2026-01-05" to 10.0, "2026-01-12" to 20.0)
        val file = makeElement(name = "svc/Foo.kt", churn = MeasurementTimeSeriesResponse(dataPoints = dataPoints, averageValue = 15.0))
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "")
        assertEquals(dataPoints, result.fileActivities[0].churnByPeriod)
    }

    @Test
    fun map_nullDataPoints_churnByPeriodIsEmptyMap() {
        val file = makeElement(name = "svc/Foo.kt", churn = MeasurementTimeSeriesResponse(dataPoints = null, averageValue = 1.0))
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "")
        assertTrue(result.fileActivities[0].churnByPeriod.isEmpty())
    }

    @Test
    fun map_filePath_normalizedBySubsystem() {
        val file = makeElement(name = "svc/path/Foo.kt")
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "svc")
        assertEquals("path/Foo.kt", result.fileActivities[0].filePath)
    }

    // Real AQ raw export shape (confirmed against a live system): FILE element names are full repo-relative
    // paths with no subsystem prefix to strip - normalizePath is a no-op here, and that's correct.
    @Test
    fun map_filePath_realExportShape_fullPathPreservedWhenNoSubsystemPrefixMatches() {
        val file = makeElement(name = "src/main/java/com/exin/astride/controller/FooController.java")
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "sig-astride-backend")
        assertEquals("src/main/java/com/exin/astride/controller/FooController.java", result.fileActivities[0].filePath)
    }

    @Test
    fun map_multipleFiles_allIncludedRegardlessOfSubsystem() {
        val files = listOf(makeElement(name = "svcA/Foo.kt"), makeElement(name = "svcB/Bar.kt"))
        val result = ArchitectureQualityMapper.map(makeResponse(files), "svcA")
        assertEquals(2, result.fileActivities.size)
    }

    @Test
    fun map_historyStartDatePresent_hasHistoryIsTrue() {
        val result = ArchitectureQualityMapper.map(makeResponse(emptyList(), historyStartDate = "2026-01-01"), "")
        assertTrue(result.hasHistory)
    }

    @Test
    fun map_historyStartDateAbsent_hasHistoryIsFalse() {
        val result = ArchitectureQualityMapper.map(makeResponse(emptyList(), historyStartDate = null), "")
        assertFalse(result.hasHistory)
    }

    @Test
    fun map_historyDates_passedThrough() {
        val result = ArchitectureQualityMapper.map(makeResponse(emptyList(), historyStartDate = "2026-01-01", historyEndDate = "2026-06-01"), "")
        assertEquals("2026-01-01", result.historyStartDate)
        assertEquals("2026-06-01", result.historyEndDate)
    }
}
