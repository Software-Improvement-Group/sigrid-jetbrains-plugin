package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RefactoringCandidateMapperTest {

    private fun makeResponse(
        id: String = "r1",
        severity: String = "high",
        weight: Int = 50,
        status: String = "RAW",
        component: String? = "svc",
        file: String? = null,
        name: String? = "MyUnit",
        mcCabe: Int? = null,
        fanIn: Int? = null,
        loc: Int? = null,
        parameters: Int? = null,
        locations: List<CandidateLocation>? = null,
        lineRanges: List<LineRange>? = null,
    ) = RefactoringCandidateResponse(
        id = id,
        severity = severity,
        weight = weight,
        status = status,
        technology = "kotlin",
        snapshotDate = "2024-01-01",
        sameComponent = null,
        sameFile = null,
        mcCabe = mcCabe,
        fanIn = fanIn,
        loc = loc,
        parameters = parameters,
        locations = locations,
        component = component,
        file = file,
        name = name,
        moduleId = null,
        startLine = null,
        endLine = null,
        lineRanges = lineRanges,
    )

    private fun makeMap(
        category: RefactoringCategory,
        candidates: List<RefactoringCandidateResponse>,
    ): Map<RefactoringCategory, RefactoringCandidatesResponse> =
        mapOf(category to RefactoringCandidatesResponse(candidates))

    private fun loc(component: String, file: String, startLine: Int = 1, endLine: Int = 10) =
        CandidateLocation(component = component, file = file, moduleId = 1, startLine = startLine, endLine = endLine)

    // Filtering

    @Test
    fun map_emptyMap_returnsEmptyList() {
        assertTrue(RefactoringCandidateMapper.map(emptyMap(), "").isEmpty())
    }

    @Test
    fun map_nullResponseForCategory_skipsWithoutCrash() {
        val result = RefactoringCandidateMapper.map(emptyMap(), "")
        assertTrue(result.isEmpty())
    }

    @Test
    fun map_blankSubsystem_returnsAllCandidates() {
        val map = makeMap(RefactoringCategory.UnitSize, listOf(
            makeResponse(id = "a", component = "svcA"),
            makeResponse(id = "b", component = "svcB"),
        ))
        assertEquals(2, RefactoringCandidateMapper.map(map, "").size)
    }

    @Test
    fun map_subsystemFilter_excludesOtherComponents() {
        val map = makeMap(RefactoringCategory.UnitSize, listOf(
            makeResponse(id = "a", component = "svcA"),
            makeResponse(id = "b", component = "svcB"),
        ))
        val result = RefactoringCandidateMapper.map(map, "svcA")
        assertEquals(1, result.size)
        assertEquals("a", result[0].id)
    }

    // Field mapping

    @Test
    fun map_severityMapped() {
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(makeResponse(severity = "very_high"))), "")
        assertEquals(MaintainabilitySeverity.VeryHigh, result[0].severity)
    }

    @Test
    fun map_statusMapped() {
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(makeResponse(status = "WILL_FIX"))), "")
        assertEquals(MaintainabilityFindingStatus.WillFix, result[0].status)
    }

    @Test
    fun map_statusLabel_snakeCaseToTitleCase() {
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(makeResponse(status = "WILL_FIX"))), "")
        assertEquals("Will Fix", result[0].statusLabel)
    }

    @Test
    fun map_nullName_producesEmptyName() {
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(makeResponse(name = null))), "")
        assertEquals("", result[0].name)
    }

    // displayLocation

    @Test
    fun map_displayLocation_noLocationsNoFile_returnsEmpty() {
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(makeResponse(locations = null, file = null))), "")
        assertEquals("", result[0].displayLocation)
    }

    @Test
    fun map_displayLocation_noLocations_fileWithNoDir_returnsFileName() {
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(makeResponse(file = "Foo.kt"))), "")
        assertEquals("Foo.kt", result[0].displayLocation)
    }

    @Test
    fun map_displayLocation_noLocations_fileWithDir_returnsPrefixPlusFile() {
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(makeResponse(file = "src/main/Foo.kt"))), "")
        assertEquals(".../Foo.kt", result[0].displayLocation)
    }

    @Test
    fun map_displayLocation_oneLocation_returnsSingleFile() {
        val response = makeResponse(locations = listOf(loc("svc", "src/A.kt")))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.Duplication, listOf(response)), "")
        assertEquals(".../A.kt", result[0].displayLocation)
    }

    @Test
    fun map_displayLocation_twoLocations_returnsAndFormat() {
        val response = makeResponse(locations = listOf(loc("svc", "src/A.kt"), loc("svc", "src/B.kt")))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.Duplication, listOf(response)), "")
        assertEquals(".../A.kt and .../B.kt", result[0].displayLocation)
    }

    @Test
    fun map_displayLocation_threeLocations_returns1OtherFiles() {
        val response = makeResponse(locations = listOf(loc("svc", "src/A.kt"), loc("svc", "src/B.kt"), loc("svc", "src/C.kt")))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.Duplication, listOf(response)), "")
        assertEquals(".../A.kt, .../B.kt and 1 other files", result[0].displayLocation)
    }

    @Test
    fun map_displayLocation_fourLocations_returns2OtherFiles() {
        val response = makeResponse(locations = listOf(
            loc("svc", "src/A.kt"), loc("svc", "src/B.kt"), loc("svc", "src/C.kt"), loc("svc", "src/D.kt"),
        ))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.Duplication, listOf(response)), "")
        assertEquals(".../A.kt, .../B.kt and 2 other files", result[0].displayLocation)
    }

    // description

    @Test
    fun map_description_duplication_twoLocations() {
        // noPathPrefix=true uses prefix="" so toDisplayFilePath("src/A.kt", "") returns "A.kt" (no ".../" prefix)
        val response = makeResponse(weight = 30, locations = listOf(loc("svc", "src/A.kt"), loc("svc", "src/B.kt")))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.Duplication, listOf(response)), "")
        assertEquals("30 lines of code are duplicated between A.kt and B.kt.", result[0].description)
    }

    @Test
    fun map_description_duplication_fileWithNoDir_noPathPrefix() {
        // noPathPrefix=true means files without a directory get no prefix added
        val response = makeResponse(weight = 10, locations = listOf(loc("svc", "Foo.kt")))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.Duplication, listOf(response)), "")
        assertEquals("10 lines of code are duplicated between Foo.kt.", result[0].description)
    }

    @Test
    fun map_description_unitSize() {
        val response = makeResponse(name = "MyClass", weight = 120, lineRanges = listOf(LineRange(1, 120)))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(response)), "")
        assertEquals("MyClass contains 120 lines of code.", result[0].description)
    }

    @Test
    fun map_description_unitSize_nameWithComma_getsSpaceAfterComma() {
        val response = makeResponse(name = "foo,bar", weight = 50, lineRanges = listOf(LineRange(1, 50)))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(response)), "")
        assertEquals("foo, bar contains 50 lines of code.", result[0].description)
    }

    @Test
    fun map_description_unitComplexity() {
        val response = makeResponse(name = "calculate", mcCabe = 8, lineRanges = listOf(LineRange(1, 30)))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitComplexity, listOf(response)), "")
        assertEquals("calculate defines 8 decision points.", result[0].description)
    }

    @Test
    fun map_description_unitInterfacing() {
        val response = makeResponse(name = "doWork", parameters = 5, lineRanges = listOf(LineRange(1, 10)))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitInterfacing, listOf(response)), "")
        assertEquals("doWork has 5 parameters.", result[0].description)
    }

    @Test
    fun map_description_moduleCoupling_fileWithDir_usesFilenameOnly() {
        // ModuleCoupling description uses toDisplayFilePath(r.file, "") — empty prefix → filename only
        val response = makeResponse(file = "src/main/Router.kt", fanIn = 12, loc = 100)
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.ModuleCoupling, listOf(response)), "")
        assertEquals("Router.kt has 12 incoming dependencies from other units.", result[0].description)
    }

    @Test
    fun map_description_moduleCoupling_fileWithNoDir() {
        val response = makeResponse(file = "Router.kt", fanIn = 3, loc = 50)
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.ModuleCoupling, listOf(response)), "")
        assertEquals("Router.kt has 3 incoming dependencies from other units.", result[0].description)
    }

    // fileLocations

    @Test
    fun map_fileLocations_duplication_fromLocations() {
        val response = makeResponse(locations = listOf(CandidateLocation("svc", "svc/src/A.kt", 1, 5, 15)))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.Duplication, listOf(response)), "svc")
        assertEquals(1, result[0].fileLocations.size)
        assertEquals("src/A.kt", result[0].fileLocations[0].filePath)
        assertEquals(5, result[0].fileLocations[0].startLine)
        assertEquals(15, result[0].fileLocations[0].endLine)
    }

    @Test
    fun map_fileLocations_duplication_nullLocations_returnsEmpty() {
        val response = makeResponse(locations = null)
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.Duplication, listOf(response)), "")
        assertTrue(result[0].fileLocations.isEmpty())
    }

    @Test
    fun map_fileLocations_moduleCoupling_singleLocationWithLocAsEndLine() {
        val response = makeResponse(component = "svc", file = "svc/Router.kt", loc = 200)
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.ModuleCoupling, listOf(response)), "svc")
        assertEquals(1, result[0].fileLocations.size)
        assertEquals("svc", result[0].fileLocations[0].component)
        assertEquals("Router.kt", result[0].fileLocations[0].filePath)
        assertEquals(0, result[0].fileLocations[0].startLine)
        assertEquals(200, result[0].fileLocations[0].endLine)
    }

    @Test
    fun map_fileLocations_moduleCoupling_nullLoc_endLineIsZero() {
        val response = makeResponse(component = "svc", file = "svc/Router.kt", loc = null)
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.ModuleCoupling, listOf(response)), "svc")
        assertEquals(0, result[0].fileLocations[0].endLine)
    }

    @Test
    fun map_fileLocations_moduleCoupling_nullComponent_usesEmptyString() {
        val response = makeResponse(component = null, file = "Router.kt", loc = 10)
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.ModuleCoupling, listOf(response)), "")
        assertEquals("", result[0].fileLocations[0].component)
    }

    @Test
    fun map_fileLocations_unitSize_fromLineRanges() {
        val response = makeResponse(
            component = "svc", file = "svc/Foo.kt",
            lineRanges = listOf(LineRange(10, 30), LineRange(50, 70)),
        )
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(response)), "svc")
        assertEquals(2, result[0].fileLocations.size)
        assertEquals(10, result[0].fileLocations[0].startLine)
        assertEquals(30, result[0].fileLocations[0].endLine)
        assertEquals(50, result[0].fileLocations[1].startLine)
        assertEquals(70, result[0].fileLocations[1].endLine)
        assertEquals("Foo.kt", result[0].fileLocations[0].filePath)
    }

    @Test
    fun map_fileLocations_unitSize_nullLineRanges_returnsEmpty() {
        val response = makeResponse(lineRanges = null)
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitSize, listOf(response)), "")
        assertTrue(result[0].fileLocations.isEmpty())
    }

    @Test
    fun map_fileLocations_unitComplexity_fromLineRanges() {
        val response = makeResponse(component = "svc", file = "svc/Bar.kt", lineRanges = listOf(LineRange(1, 20)))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitComplexity, listOf(response)), "svc")
        assertEquals(1, result[0].fileLocations.size)
        assertEquals("Bar.kt", result[0].fileLocations[0].filePath)
    }

    @Test
    fun map_fileLocations_unitInterfacing_fromLineRanges() {
        val response = makeResponse(component = "svc", file = "svc/Baz.kt", lineRanges = listOf(LineRange(5, 8)))
        val result = RefactoringCandidateMapper.map(makeMap(RefactoringCategory.UnitInterfacing, listOf(response)), "svc")
        assertEquals("Baz.kt", result[0].fileLocations[0].filePath)
    }
}
