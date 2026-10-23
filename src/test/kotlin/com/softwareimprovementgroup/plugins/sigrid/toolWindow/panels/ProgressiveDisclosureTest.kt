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
        val disclosure = ProgressiveDisclosure(3).apply { toggle() }
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

    @Test
    fun toggle_expandsThenCollapsesAgain() {
        val disclosure = ProgressiveDisclosure(3)

        disclosure.toggle()
        assertEquals(ranked, disclosure.visible(ranked, narrowed = false))
        disclosure.toggle()
        assertEquals(listOf(1, 2, 3), disclosure.visible(ranked, narrowed = false))
    }

    @Test
    fun reset_collapsesAnExpandedList() {
        val disclosure = ProgressiveDisclosure(3).apply { toggle() }

        disclosure.reset()

        assertEquals(listOf(1, 2, 3), disclosure.visible(ranked, narrowed = false))
    }

    @Test
    fun canToggle_onlyWhenOverLimitAndNotNarrowed() {
        val disclosure = ProgressiveDisclosure(3)

        assertEquals(true, disclosure.canToggle(total = 10, narrowed = false))
        assertEquals(false, disclosure.canToggle(total = 3, narrowed = false))
        assertEquals(false, disclosure.canToggle(total = 10, narrowed = true))
        assertEquals(false, ProgressiveDisclosure(null).canToggle(total = 10, narrowed = false))
    }
}
