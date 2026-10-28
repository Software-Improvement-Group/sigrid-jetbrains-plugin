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
        churn: Double? = 10.0,
        commits: Double? = 3.0,
    ) = SystemElementResponse(
        id = "id-$name",
        name = name,
        type = type,
        measurementValues = listOfNotNull(churn?.let { "CHURN" to it }, commits?.let { "COMMITS" to it }).toMap(),
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
        val file = makeElement(name = "svc/Foo.kt", churn = null, commits = null)
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "")
        assertTrue(result.fileActivities.isEmpty())
    }

    @Test
    fun map_churn_mapped() {
        val file = makeElement(name = "svc/Foo.kt", churn = 42.5)
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "")
        assertEquals(42.5, result.fileActivities[0].churn)
    }

    @Test
    fun map_zeroChurn_keptAsZeroNotDropped() {
        val file = makeElement(name = "svc/Foo.kt", churn = 0.0)
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "")
        assertEquals(0.0, result.fileActivities.single().churn)
    }

    @Test
    fun map_commitsOnly_includedWithZeroChurn() {
        val file = makeElement(name = "svc/Foo.kt", churn = null, commits = 4.0)
        val activity = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "").fileActivities.single()
        assertEquals(0.0, activity.churn)
        assertEquals(4.0, activity.commits)
    }

    @Test
    fun map_fileElementWithNullMeasurementValues_excluded() {
        val file = SystemElementResponse(id = "id", name = "svc/Foo.kt", type = "FILE", measurementValues = null)
        val result = ArchitectureQualityMapper.map(makeResponse(listOf(file)), "")
        assertTrue(result.fileActivities.isEmpty())
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
