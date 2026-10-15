package com.softwareimprovementgroup.plugins.sigrid.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrioritizedFindingTest {

    private fun finding(displayLocation: String, vararg paths: String) = PrioritizedFinding(
        id = "id",
        capability = PriorityCapability.Security,
        priorityRank = PriorityRank.High,
        displayLocation = displayLocation,
        description = "",
        statusLabel = "",
        remark = "",
        fileLocations = paths.map { FileLocation(component = "", filePath = it) },
        href = null,
        editable = false,
        statusOptions = emptyList(),
        currentStatusValue = "",
    )

    @Test
    fun fileGroupKey_usesFirstFileLocation() {
        assertEquals("src/A.kt", finding("A.kt", "src/A.kt", "src/B.kt").fileGroupKey)
    }

    @Test
    fun fileGroupKey_skipsBlankFilePaths() {
        assertEquals("src/B.kt", finding("B.kt", " ", "src/B.kt").fileGroupKey)
    }

    @Test
    fun fileGroupKey_withoutFileLocations_fallsBackToDisplayLocation() {
        assertEquals("lodash", finding("lodash").fileGroupKey)
    }

    private fun groupedFinding(rank: PriorityRank, tier: PriorityTier = PriorityTier.Standard) =
        finding("x", "src/x.kt").copy(priorityRank = rank, tier = tier)

    private fun orderedGroups(vararg groups: Pair<String, List<PrioritizedFinding>>) =
        groups.toMap().entries.sortedWith(PrioritizedFinding.fileGroupOrder).map { it.key }

    @Test
    fun fileGroupOrder_tierBeatsSeverityAndCount() {
        val order = orderedGroups(
            "big" to List(5) { groupedFinding(PriorityRank.Critical) },
            "security" to listOf(groupedFinding(PriorityRank.Low, PriorityTier.VulnerabilityFirst)),
        )
        assertEquals(listOf("security", "big"), order)
    }

    @Test
    fun fileGroupOrder_sameTier_higherSeverityBeatsLargerCount() {
        val order = orderedGroups(
            "many" to List(5) { groupedFinding(PriorityRank.Medium) },
            "severe" to listOf(groupedFinding(PriorityRank.High)),
        )
        assertEquals(listOf("severe", "many"), order)
    }

    @Test
    fun fileGroupOrder_sameTierAndSeverity_moreFindingsFirst() {
        val order = orderedGroups(
            "a" to listOf(groupedFinding(PriorityRank.High)),
            "b" to List(3) { groupedFinding(PriorityRank.High) },
        )
        assertEquals(listOf("b", "a"), order)
    }

    @Test
    fun fileGroupOrder_fullTie_keepsIncomingOrder() {
        val order = orderedGroups(
            "z" to listOf(groupedFinding(PriorityRank.High)),
            "a" to listOf(groupedFinding(PriorityRank.High)),
        )
        assertEquals(listOf("z", "a"), order)
    }
}
