package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FindingEditPopupHandlerTest {

    // detectCommonValue

    @Test
    fun detectCommonValue_emptyList_returnsNull() =
        assertNull(FindingEditPopupHandler.detectCommonValue(emptyList()))

    @Test
    fun detectCommonValue_singleElement_returnsThatElement() =
        assertEquals("RAW", FindingEditPopupHandler.detectCommonValue(listOf("RAW")))

    @Test
    fun detectCommonValue_allSame_returnsThatValue() =
        assertEquals("RAW", FindingEditPopupHandler.detectCommonValue(listOf("RAW", "RAW", "RAW")))

    @Test
    fun detectCommonValue_allDifferent_returnsNull() =
        assertNull(FindingEditPopupHandler.detectCommonValue(listOf("RAW", "WILL_FIX")))

    @Test
    fun detectCommonValue_threeDifferent_returnsNull() =
        assertNull(FindingEditPopupHandler.detectCommonValue(listOf("RAW", "WILL_FIX", "ACCEPTED")))

    @Test
    fun detectCommonValue_twoSameOneDifferent_returnsNull() =
        assertNull(FindingEditPopupHandler.detectCommonValue(listOf("RAW", "RAW", "WILL_FIX")))

    @Test
    fun detectCommonValue_emptyStrings_allSame_returnsEmptyString() =
        assertEquals("", FindingEditPopupHandler.detectCommonValue(listOf("", "")))

    @Test
    fun detectCommonValue_emptyStringAndNonEmpty_returnsNull() =
        assertNull(FindingEditPopupHandler.detectCommonValue(listOf("", "RAW")))

    // MAX_EDIT_ITEMS_SIZE

    @Test
    fun maxEditItemsSize_isExactly25() =
        assertEquals(25, FindingEditPopupHandler.MAX_EDIT_ITEMS_SIZE)
}
