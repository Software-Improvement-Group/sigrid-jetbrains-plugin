package com.softwareimprovementgroup.plugins.sigrid.models

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChangedFilesTest {
    private val changed = ChangedFiles(setOf("repo/svc/Foo.kt", "/abs/project/repo/svc/Bar.kt"))

    @Test
    fun matches_exactPath_true() {
        assertTrue(changed.matches("repo/svc/Foo.kt"))
    }

    @Test
    fun matches_findingPathIsSuffixOfChangedPath_true() {
        assertTrue(changed.matches("svc/Foo.kt"))
        assertTrue(changed.matches("svc/Bar.kt"))
    }

    @Test
    fun matches_differentFile_false() {
        assertFalse(changed.matches("svc/Baz.kt"))
    }

    @Test
    fun matches_partialFileNameWithoutSeparator_false() {
        assertFalse(changed.matches("oo.kt"))
    }

    @Test
    fun matches_blankPath_false() {
        assertFalse(changed.matches(""))
    }

    @Test
    fun matches_emptyChangedFiles_false() {
        assertFalse(ChangedFiles.EMPTY.matches("svc/Foo.kt"))
    }
}
