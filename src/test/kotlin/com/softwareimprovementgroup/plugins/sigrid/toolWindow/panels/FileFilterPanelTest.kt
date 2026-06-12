package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FileFilterPanelTest {

    // relativePath

    @Test
    fun relativePath_pathUnderBase_stripsBaseAndSeparator() {
        assertEquals(
            "src/main/Foo.kt",
            FileFilterPanel.relativePath("/home/user/project/src/main/Foo.kt", "/home/user/project")
        )
    }

    @Test
    fun relativePath_pathEqualsBase_returnsOriginal() {
        // removePrefix("$base/") requires a trailing slash, so an exact match returns unchanged
        assertEquals("/project", FileFilterPanel.relativePath("/project", "/project"))
    }

    @Test
    fun relativePath_pathOutsideBase_returnsOriginal() {
        assertEquals(
            "/other/src/Foo.kt",
            FileFilterPanel.relativePath("/other/src/Foo.kt", "/home/user/project")
        )
    }

    @Test
    fun relativePath_baseWithTrailingSlash_doesNotDoubleStrip() {
        // basePath never has trailing slash in practice; ensure removePrefix works correctly
        assertEquals(
            "src/Foo.kt",
            FileFilterPanel.relativePath("/project/src/Foo.kt", "/project")
        )
    }

    @Test
    fun relativePath_topLevelFile_returnsFileName() {
        assertEquals("Foo.kt", FileFilterPanel.relativePath("/project/Foo.kt", "/project"))
    }

    @Test
    fun relativePath_baseIsPrefix_butNotDirectParent_returnsOriginal() {
        // "/projects" is a prefix of "/projects-other/..." but not a parent dir
        assertEquals(
            "/projects-other/src/Foo.kt",
            FileFilterPanel.relativePath("/projects-other/src/Foo.kt", "/projects")
        )
    }

    // matchesActivePath

    @Test
    fun matchesActivePath_exactMatch_returnsTrue() {
        assertTrue(FileFilterPanel.matchesActivePath("src/main/Foo.kt", "src/main/Foo.kt"))
    }

    @Test
    fun matchesActivePath_activePathEndsWithSlashAndLocPath_returnsTrue() {
        assertTrue(FileFilterPanel.matchesActivePath("main/Foo.kt", "src/main/Foo.kt"))
    }

    @Test
    fun matchesActivePath_completelyDifferentPaths_returnsFalse() {
        assertFalse(FileFilterPanel.matchesActivePath("src/Bar.kt", "src/Foo.kt"))
    }

    @Test
    fun matchesActivePath_locPathIsSuffix_butMissingSlash_returnsFalse() {
        // "oo.kt" is a suffix of "Foo.kt" but without the "/" separator it should not match
        assertFalse(FileFilterPanel.matchesActivePath("oo.kt", "Foo.kt"))
    }

    @Test
    fun matchesActivePath_locPathLongerThanActivePath_returnsFalse() {
        assertFalse(FileFilterPanel.matchesActivePath("src/main/Foo.kt", "main/Foo.kt"))
    }

    @Test
    fun matchesActivePath_emptyLocPath_returnsFalse() {
        assertFalse(FileFilterPanel.matchesActivePath("", "src/Foo.kt"))
    }

    @Test
    fun matchesActivePath_emptyActivePath_returnsFalse() {
        assertFalse(FileFilterPanel.matchesActivePath("src/Foo.kt", ""))
    }

    @Test
    fun matchesActivePath_bothEmpty_returnsTrue() {
        assertTrue(FileFilterPanel.matchesActivePath("", ""))
    }

    @Test
    fun matchesActivePath_singleFileName_exactMatch() {
        assertTrue(FileFilterPanel.matchesActivePath("Foo.kt", "Foo.kt"))
    }

    @Test
    fun matchesActivePath_deeplyNestedActivePath_matchesShortLocPath() {
        assertTrue(FileFilterPanel.matchesActivePath("Bar.kt", "a/b/c/d/Bar.kt"))
    }

    @Test
    fun matchesActivePath_partialDirectoryName_doesNotMatch() {
        // "src/Foo.kt" should not match "xsrc/Foo.kt" (no "/" before "src")
        assertFalse(FileFilterPanel.matchesActivePath("src/Foo.kt", "xsrc/Foo.kt"))
    }
}