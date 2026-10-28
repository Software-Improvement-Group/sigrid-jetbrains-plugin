package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PriorityGrouperTest {

    private fun makeCandidate(
        id: String = "m1",
        category: RefactoringCategory = RefactoringCategory.Duplication,
        weight: Int = 100,
        component: String? = "svc",
        fileLocations: List<FileLocation> = listOf(FileLocation(component ?: "", "svc/Foo.kt")),
    ) = RefactoringCandidate(
        id = id,
        category = category,
        severity = MaintainabilitySeverity.VeryHigh,
        status = MaintainabilityFindingStatus.Raw,
        statusLabel = "",
        weight = weight,
        technology = "Kotlin",
        snapshotDate = "",
        name = "foo",
        mcCabe = null,
        fanIn = null,
        component = component,
        parameters = null,
        displayLocation = "Foo.kt",
        description = "",
        remark = "",
        fileLocations = fileLocations,
        href = null,
    )

    private fun makeFinding(id: String, paths: List<String>) = PrioritizedFinding(
        id = id,
        capability = PriorityCapability.Maintainability,
        priorityRank = PriorityRank.High,
        displayLocation = "",
        description = "",
        statusLabel = "",
        remark = "",
        fileLocations = paths.map { FileLocation("svc", it) },
        href = null,
        editable = false,
        statusOptions = emptyList(),
        currentStatusValue = "",
    )

    // groupByFile

    @Test
    fun groupByFile_findingsAtSameFile_groupedTogether() {
        val findings = listOf(makeFinding("a", listOf("svc/Foo.kt")), makeFinding("b", listOf("svc/Foo.kt")))
        val result = PriorityGrouper.groupByFile(findings)
        assertEquals(listOf("a", "b"), result["svc/Foo.kt"]?.map { it.id })
    }

    @Test
    fun groupByFile_findingWithMultipleLocations_appearsUnderEachPath() {
        val findings = listOf(makeFinding("a", listOf("svc/Foo.kt", "svc/Bar.kt")))
        val result = PriorityGrouper.groupByFile(findings)
        assertTrue(result.containsKey("svc/Foo.kt"))
        assertTrue(result.containsKey("svc/Bar.kt"))
    }

    @Test
    fun groupByFile_blankPath_skipped() {
        val findings = listOf(makeFinding("a", listOf("")))
        val result = PriorityGrouper.groupByFile(findings)
        assertTrue(result.isEmpty())
    }

    // duplicationFirstEligible

    @Test
    fun duplicationFirstEligible_noDuplicationFindings_isFalse() {
        val candidates = listOf(makeCandidate(category = RefactoringCategory.UnitSize))
        assertFalse(PriorityGrouper.duplicationFirstEligible(candidates))
    }

    @Test
    fun duplicationFirstEligible_allDuplicationFilesOverlapWithOtherFindings_isTrue() {
        val candidates = listOf(
            makeCandidate(id = "dup1", category = RefactoringCategory.Duplication, fileLocations = listOf(FileLocation("svc", "svc/A.kt"))),
            makeCandidate(id = "other1", category = RefactoringCategory.UnitSize, fileLocations = listOf(FileLocation("svc", "svc/A.kt"))),
        )
        assertTrue(PriorityGrouper.duplicationFirstEligible(candidates))
    }

    @Test
    fun duplicationFirstEligible_noOverlapAtAll_isFalse() {
        val candidates = listOf(
            makeCandidate(id = "dup1", category = RefactoringCategory.Duplication, fileLocations = listOf(FileLocation("svc", "svc/A.kt"))),
            makeCandidate(id = "other1", category = RefactoringCategory.UnitSize, fileLocations = listOf(FileLocation("svc", "svc/B.kt"))),
        )
        assertFalse(PriorityGrouper.duplicationFirstEligible(candidates))
    }

    @Test
    fun duplicationFirstEligible_exactlyAtThreshold_isTrue() {
        val candidates = listOf(
            makeCandidate(id = "dup1", category = RefactoringCategory.Duplication, fileLocations = listOf(FileLocation("svc", "svc/A.kt"))),
            makeCandidate(id = "dup2", category = RefactoringCategory.Duplication, fileLocations = listOf(FileLocation("svc", "svc/B.kt"))),
            makeCandidate(id = "other1", category = RefactoringCategory.UnitSize, fileLocations = listOf(FileLocation("svc", "svc/A.kt"))),
        )
        // 1 of 2 duplication files (A.kt) overlaps with another finding -> exactly 50%.
        assertTrue(PriorityGrouper.duplicationFirstEligible(candidates))
    }

    @Test
    fun duplicationFirstEligible_belowThreshold_isFalse() {
        val candidates = listOf(
            makeCandidate(id = "dup1", category = RefactoringCategory.Duplication, fileLocations = listOf(FileLocation("svc", "svc/A.kt"))),
            makeCandidate(id = "dup2", category = RefactoringCategory.Duplication, fileLocations = listOf(FileLocation("svc", "svc/B.kt"))),
            makeCandidate(id = "dup3", category = RefactoringCategory.Duplication, fileLocations = listOf(FileLocation("svc", "svc/C.kt"))),
            makeCandidate(id = "other1", category = RefactoringCategory.UnitSize, fileLocations = listOf(FileLocation("svc", "svc/A.kt"))),
        )
        // 1 of 3 duplication files overlaps -> 33%, below the 50% threshold.
        assertFalse(PriorityGrouper.duplicationFirstEligible(candidates))
    }

    // sequenceDuplicationFirst

    @Test
    fun sequenceDuplicationFirst_nonDuplicationCandidates_excluded() {
        val candidates = listOf(makeCandidate(id = "unit", category = RefactoringCategory.UnitSize))
        assertTrue(PriorityGrouper.sequenceDuplicationFirst(candidates).isEmpty())
    }

    @Test
    fun sequenceDuplicationFirst_singleComponent_sortedByWeightDescending() {
        val candidates = listOf(
            makeCandidate(id = "small", weight = 10, component = "svc"),
            makeCandidate(id = "big", weight = 500, component = "svc"),
        )
        val result = PriorityGrouper.sequenceDuplicationFirst(candidates)
        assertEquals(listOf("big", "small"), result.map { it.id })
    }

    @Test
    fun sequenceDuplicationFirst_multipleComponents_roundRobinsAcrossThem() {
        val candidates = listOf(
            makeCandidate(id = "a1", weight = 900, component = "a"),
            makeCandidate(id = "a2", weight = 800, component = "a"),
            makeCandidate(id = "a3", weight = 700, component = "a"),
            makeCandidate(id = "b1", weight = 100, component = "b"),
        )
        val result = PriorityGrouper.sequenceDuplicationFirst(candidates)
        // Without diversity, a1/a2/a3 (all from "a") would dominate the top 3 slots outright.
        // Round-robin interleaves "b1" into position 2 despite its much lower weight.
        assertEquals(listOf("a1", "b1", "a2", "a3"), result.map { it.id })
    }

    @Test
    fun sequenceDuplicationFirst_empty_returnsEmpty() {
        assertTrue(PriorityGrouper.sequenceDuplicationFirst(emptyList()).isEmpty())
    }
}
