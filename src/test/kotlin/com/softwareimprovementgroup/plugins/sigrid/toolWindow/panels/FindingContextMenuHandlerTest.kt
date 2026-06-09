package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FindingContextMenuHandlerTest {

    // detectCommonValue

    @Test
    fun detectCommonValue_emptyList_returnsNull() =
        assertNull(FindingContextMenuHandler.detectCommonValue(emptyList()))

    @Test
    fun detectCommonValue_singleElement_returnsThatElement() =
        assertEquals("RAW", FindingContextMenuHandler.detectCommonValue(listOf("RAW")))

    @Test
    fun detectCommonValue_allSame_returnsThatValue() =
        assertEquals("RAW", FindingContextMenuHandler.detectCommonValue(listOf("RAW", "RAW", "RAW")))

    @Test
    fun detectCommonValue_allDifferent_returnsNull() =
        assertNull(FindingContextMenuHandler.detectCommonValue(listOf("RAW", "WILL_FIX")))

    @Test
    fun detectCommonValue_threeDifferent_returnsNull() =
        assertNull(FindingContextMenuHandler.detectCommonValue(listOf("RAW", "WILL_FIX", "ACCEPTED")))

    @Test
    fun detectCommonValue_twoSameOneDifferent_returnsNull() =
        assertNull(FindingContextMenuHandler.detectCommonValue(listOf("RAW", "RAW", "WILL_FIX")))

    @Test
    fun detectCommonValue_emptyStrings_allSame_returnsEmptyString() =
        assertEquals("", FindingContextMenuHandler.detectCommonValue(listOf("", "")))

    @Test
    fun detectCommonValue_emptyStringAndNonEmpty_returnsNull() =
        assertNull(FindingContextMenuHandler.detectCommonValue(listOf("", "RAW")))

    // MAX_EDIT_ITEMS_SIZE

    @Test
    fun maxEditItemsSize_isExactly25() =
        assertEquals(25, FindingContextMenuHandler.MAX_EDIT_ITEMS_SIZE)
}
