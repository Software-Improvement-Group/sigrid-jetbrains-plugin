package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EditFindingDialogTest {

    private val defaultOptions = listOf("Raw" to "RAW", "Will Fix" to "WILL_FIX", "Accepted" to "ACCEPTED")

    // buildEffectiveStatusOptions

    @Test
    fun buildEffectiveStatusOptions_notMixed_returnsOptionsUnchanged() =
        assertEquals(defaultOptions, EditFindingDialog.buildEffectiveStatusOptions(false, defaultOptions))

    @Test
    fun buildEffectiveStatusOptions_notMixed_emptyOptions_returnsEmpty() =
        assertTrue(EditFindingDialog.buildEffectiveStatusOptions(false, emptyList()).isEmpty())

    @Test
    fun buildEffectiveStatusOptions_mixed_prependsSentinelAtIndexZero() {
        val result = EditFindingDialog.buildEffectiveStatusOptions(true, defaultOptions)
        assertEquals(4, result.size)
        assertEquals("(mixed)", result[0].first)
    }

    @Test
    fun buildEffectiveStatusOptions_mixed_emptyOptions_returnsOnlySentinel() {
        val result = EditFindingDialog.buildEffectiveStatusOptions(true, emptyList())
        assertEquals(1, result.size)
        assertEquals("(mixed)", result[0].first)
    }

    @Test
    fun buildEffectiveStatusOptions_mixed_originalOptionsShiftedByOne() {
        val result = EditFindingDialog.buildEffectiveStatusOptions(true, defaultOptions)
        assertEquals(defaultOptions, result.drop(1))
    }

    @Test
    fun buildEffectiveStatusOptions_mixed_sentinelSecondIsEmptyString() {
        val result = EditFindingDialog.buildEffectiveStatusOptions(true, defaultOptions)
        assertEquals("", result[0].second)
    }

    // resolveStatus

    @Test
    fun resolveStatus_negativeIndex_returnsNull() {
        val opts = EditFindingDialog.buildEffectiveStatusOptions(false, defaultOptions)
        assertNull(EditFindingDialog.resolveStatus(false, -1, opts))
    }

    @Test
    fun resolveStatus_negativeIndex_mixed_returnsNull() {
        val opts = EditFindingDialog.buildEffectiveStatusOptions(true, defaultOptions)
        assertNull(EditFindingDialog.resolveStatus(true, -1, opts))
    }

    @Test
    fun resolveStatus_notMixed_indexZero_returnsFirstOption() {
        val opts = EditFindingDialog.buildEffectiveStatusOptions(false, defaultOptions)
        assertEquals("RAW", EditFindingDialog.resolveStatus(false, 0, opts))
    }

    @Test
    fun resolveStatus_notMixed_indexOne_returnsSecondOption() {
        val opts = EditFindingDialog.buildEffectiveStatusOptions(false, defaultOptions)
        assertEquals("WILL_FIX", EditFindingDialog.resolveStatus(false, 1, opts))
    }

    @Test
    fun resolveStatus_notMixed_lastIndex_returnsLastOption() {
        val opts = EditFindingDialog.buildEffectiveStatusOptions(false, defaultOptions)
        assertEquals("ACCEPTED", EditFindingDialog.resolveStatus(false, 2, opts))
    }

    @Test
    fun resolveStatus_mixed_indexZero_returnsNull() {
        // Index 0 is the sentinel — means "leave each unchanged"
        val opts = EditFindingDialog.buildEffectiveStatusOptions(true, defaultOptions)
        assertNull(EditFindingDialog.resolveStatus(true, 0, opts))
    }

    @Test
    fun resolveStatus_mixed_indexOne_returnsFirstRealOption() {
        val opts = EditFindingDialog.buildEffectiveStatusOptions(true, defaultOptions)
        assertEquals("RAW", EditFindingDialog.resolveStatus(true, 1, opts))
    }

    @Test
    fun resolveStatus_mixed_indexTwo_returnsSecondRealOption() {
        val opts = EditFindingDialog.buildEffectiveStatusOptions(true, defaultOptions)
        assertEquals("WILL_FIX", EditFindingDialog.resolveStatus(true, 2, opts))
    }

    @Test
    fun resolveStatus_mixed_lastRealIndex_returnsLastRealOption() {
        val opts = EditFindingDialog.buildEffectiveStatusOptions(true, defaultOptions)
        assertEquals("ACCEPTED", EditFindingDialog.resolveStatus(true, 3, opts))
    }

    @Test
    fun resolveStatus_notMixed_emptyStringApiValue_returned() {
        // Sentinel is identified by isMixedStatus + index==0, not by api value being "".
        // A non-mixed option whose api value happens to be "" should still be returned.
        val opts = listOf("(no status)" to "")
        assertEquals("", EditFindingDialog.resolveStatus(false, 0, opts))
    }

    // resolveRemark

    @Test
    fun resolveRemark_notMixed_nonEmptyText_returnsText() =
        assertEquals("needs review", EditFindingDialog.resolveRemark(false, "needs review"))

    @Test
    fun resolveRemark_notMixed_emptyText_returnsEmptyString() =
        assertEquals("", EditFindingDialog.resolveRemark(false, ""))

    @Test
    fun resolveRemark_mixed_nonEmptyText_returnsText() =
        assertEquals("needs review", EditFindingDialog.resolveRemark(true, "needs review"))

    @Test
    fun resolveRemark_mixed_emptyText_returnsNull() =
        assertNull(EditFindingDialog.resolveRemark(true, ""))

    @Test
    fun resolveRemark_notMixed_blankText_returnsBlank() =
        assertEquals("   ", EditFindingDialog.resolveRemark(false, "   "))

    @Test
    fun resolveRemark_mixed_blankText_returnsBlank() =
        // Blank (whitespace-only) is not empty; treated as an explicit remark value
        assertEquals("   ", EditFindingDialog.resolveRemark(true, "   "))
}
