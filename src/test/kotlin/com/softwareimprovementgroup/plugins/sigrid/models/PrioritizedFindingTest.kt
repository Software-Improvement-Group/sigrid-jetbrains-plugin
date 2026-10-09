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
}
