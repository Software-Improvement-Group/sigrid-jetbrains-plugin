package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProgressiveDisclosureTest {
    private val ranked = (1..10).toList()

    @Test
    fun visible_overLimit_takesTopOnly() {
        assertEquals(listOf(1, 2, 3), ProgressiveDisclosure(3).visible(ranked, narrowed = false))
    }

    @Test
    fun visible_underLimit_returnsAll() {
        assertEquals(ranked, ProgressiveDisclosure(20).visible(ranked, narrowed = false))
    }

    @Test
    fun visible_expanded_returnsAll() {
        val disclosure = ProgressiveDisclosure(3).apply { expanded = true }
        assertEquals(ranked, disclosure.visible(ranked, narrowed = false))
    }

    @Test
    fun visible_narrowed_returnsAll() {
        assertEquals(ranked, ProgressiveDisclosure(3).visible(ranked, narrowed = true))
    }

    @Test
    fun visible_noLimit_returnsAll() {
        assertEquals(ranked, ProgressiveDisclosure(null).visible(ranked, narrowed = false))
    }
}
