package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FindingNavigatorTest {

    private fun loc(filePath: String, startLine: Int? = null) =
        FileLocation(component = "svc", filePath = filePath, startLine = startLine)

    // filterValidLocations

    @Test
    fun filterValidLocations_emptyList_returnsEmpty() {
        assertTrue(FindingNavigator.filterValidLocations(emptyList()).isEmpty())
    }

    @Test
    fun filterValidLocations_allBlankPaths_returnsEmpty() {
        val locations = listOf(loc(""), loc("   "), loc("\t"))
        assertTrue(FindingNavigator.filterValidLocations(locations).isEmpty())
    }

    @Test
    fun filterValidLocations_mixedPaths_keepsNonBlank() {
        val locations = listOf(loc("src/Foo.kt"), loc(""), loc("src/Bar.kt"))
        val result = FindingNavigator.filterValidLocations(locations)
        assertEquals(listOf("src/Foo.kt", "src/Bar.kt"), result.map { it.filePath })
    }

    @Test
    fun filterValidLocations_allValid_returnsAll() {
        val locations = listOf(loc("a/A.kt"), loc("b/B.kt"))
        assertEquals(2, FindingNavigator.filterValidLocations(locations).size)
    }

    // editorLine

    @Test
    fun editorLine_nullStartLine_returnsZero() {
        assertEquals(0, FindingNavigator.editorLine(loc("src/Foo.kt", null)))
    }

    @Test
    fun editorLine_zeroStartLine_returnsZero() {
        assertEquals(0, FindingNavigator.editorLine(loc("src/Foo.kt", 0)))
    }

    @Test
    fun editorLine_negativeStartLine_returnsZero() {
        assertEquals(0, FindingNavigator.editorLine(loc("src/Foo.kt", -1)))
    }

    @Test
    fun editorLine_oneBasedLine_convertsToZeroBased() {
        assertEquals(0, FindingNavigator.editorLine(loc("src/Foo.kt", 1)))
    }

    @Test
    fun editorLine_positiveStartLine_subtractsOne() {
        assertEquals(9, FindingNavigator.editorLine(loc("src/Foo.kt", 10)))
    }

    @Test
    fun editorLine_largeLineNumber_subtractsOne() {
        assertEquals(999, FindingNavigator.editorLine(loc("src/Foo.kt", 1000)))
    }

    // popupItemText

    @Test
    fun popupItemText_fileNameOnly_noDirectory() {
        assertEquals("Foo.kt", FindingNavigator.popupItemText(loc("Foo.kt")))
    }

    @Test
    fun popupItemText_nestedPath_showsFileNameOnly() {
        assertEquals("Foo.kt", FindingNavigator.popupItemText(loc("src/main/Foo.kt")))
    }

    @Test
    fun popupItemText_withStartLine_appendsLineNumber() {
        assertEquals("Foo.kt:42", FindingNavigator.popupItemText(loc("src/Foo.kt", 42)))
    }

    @Test
    fun popupItemText_nullStartLine_noLineNumber() {
        assertEquals("Foo.kt", FindingNavigator.popupItemText(loc("src/Foo.kt", null)))
    }

    @Test
    fun popupItemText_zeroStartLine_noLineNumber() {
        assertEquals("Foo.kt", FindingNavigator.popupItemText(loc("src/Foo.kt", 0)))
    }

    @Test
    fun popupItemText_negativeStartLine_noLineNumber() {
        assertEquals("Foo.kt", FindingNavigator.popupItemText(loc("src/Foo.kt", -5)))
    }

    @Test
    fun popupItemText_lineOne_appendsLineNumber() {
        assertEquals("Foo.kt:1", FindingNavigator.popupItemText(loc("src/Foo.kt", 1)))
    }
}