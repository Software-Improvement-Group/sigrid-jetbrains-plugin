package com.softwareimprovementgroup.plugins.sigrid.mappers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapperUtilsTest {

    // normalizePath

    @Test
    fun normalizePath_nullPath_returnsEmpty() {
        assertEquals("", normalizePath(null, "svc"))
    }

    @Test
    fun normalizePath_blankPath_returnsEmpty() {
        assertEquals("", normalizePath("   ", "svc"))
    }

    @Test
    fun normalizePath_blankSubsystem_returnsPathUnchanged() {
        assertEquals("svc/src/Foo.kt", normalizePath("svc/src/Foo.kt", ""))
    }

    @Test
    fun normalizePath_whitespaceSubsystem_returnsPathUnchanged() {
        assertEquals("svc/src/Foo.kt", normalizePath("svc/src/Foo.kt", "   "))
    }

    @Test
    fun normalizePath_pathStartsWithSubsystemPrefix_stripsPrefix() {
        assertEquals("src/main/Foo.kt", normalizePath("svc/src/main/Foo.kt", "svc"))
    }

    @Test
    fun normalizePath_pathDoesNotStartWithSubsystemPrefix_returnsUnchanged() {
        assertEquals("other/src/Bar.kt", normalizePath("other/src/Bar.kt", "svc"))
    }

    @Test
    fun normalizePath_partialNameMatch_doesNotStrip() {
        assertEquals("svcX/src/Foo.kt", normalizePath("svcX/src/Foo.kt", "svc"))
    }

    @Test
    fun normalizePath_pathEqualsSubsystemPlusSlash_returnsEmpty() {
        assertEquals("", normalizePath("svc/", "svc"))
    }

    // toDisplayFilePath

    @Test
    fun toDisplayFilePath_nullPath_returnsEmpty() {
        assertEquals("", toDisplayFilePath(null))
    }

    @Test
    fun toDisplayFilePath_blankPath_returnsEmpty() {
        assertEquals("", toDisplayFilePath("   "))
    }

    @Test
    fun toDisplayFilePath_noDirectory_returnsFileNameOnly() {
        assertEquals("Foo.kt", toDisplayFilePath("Foo.kt"))
    }

    @Test
    fun toDisplayFilePath_withDirectory_returnsPrefixPlusFileName() {
        assertEquals(".../Foo.kt", toDisplayFilePath("src/main/Foo.kt"))
    }

    @Test
    fun toDisplayFilePath_customPrefix_usesCustomPrefix() {
        assertEquals(">>>/Bar.kt", toDisplayFilePath("src/Bar.kt", ">>>/"))
    }

    @Test
    fun toDisplayFilePath_emptyPrefix_returnsFileNameOnly() {
        assertEquals("Baz.kt", toDisplayFilePath("src/Baz.kt", ""))
    }

    @Test
    fun toDisplayFilePath_deeplyNestedPath_returnsDefaultPrefixPlusFileName() {
        assertEquals(".../File.kt", toDisplayFilePath("a/b/c/d/File.kt"))
    }
}
