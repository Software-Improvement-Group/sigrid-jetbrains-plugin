package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ColumnFilterDefTest {

    private data class Item(val risk: String, val status: String)

    private fun riskFilter(vararg selected: String): ColumnFilterDef<Item> =
        ColumnFilterDef<Item>(
            columnName = "Risk",
            options = listOf(FilterOption("High", "high"), FilterOption("Medium", "medium"), FilterOption("Low", "low")),
            getOptionId = { risk },
        ).also { it.selectedIds = selected.toSet() }

    private fun statusFilter(vararg selected: String): ColumnFilterDef<Item> =
        ColumnFilterDef<Item>(
            columnName = "Status",
            options = listOf(FilterOption("Open", "open"), FilterOption("Fixed", "fixed")),
            getOptionId = { this.status },
        ).also { it.selectedIds = selected.toSet() }

    private fun <T> applyColumnFilters(items: List<T>, filters: List<ColumnFilterDef<T>>): List<T> =
        filters.fold(items) { acc, def ->
            if (def.selectedIds.isEmpty()) acc
            else acc.filter { def.getOptionId(it) in def.selectedIds }
        }

    // isActive

    @Test
    fun isActive_noSelectedIds_returnsFalse() {
        assertFalse(riskFilter().isActive)
    }

    @Test
    fun isActive_oneSelectedId_returnsTrue() {
        assertTrue(riskFilter("high").isActive)
    }

    @Test
    fun isActive_multipleSelectedIds_returnsTrue() {
        assertTrue(riskFilter("high", "medium").isActive)
    }

    // selectedIds mutation

    @Test
    fun selectedIds_addId_isActiveBecomesTrue() {
        val def = riskFilter()
        def.selectedIds = def.selectedIds + "high"
        assertTrue(def.isActive)
    }

    @Test
    fun selectedIds_removeLastId_isActiveBecomesFalse() {
        val def = riskFilter("high")
        def.selectedIds = def.selectedIds - "high"
        assertFalse(def.isActive)
    }

    @Test
    fun selectedIds_replaceSet_reflectsNewContent() {
        val def = riskFilter("high", "medium")
        def.selectedIds = setOf("low")
        assertEquals(setOf("low"), def.selectedIds)
    }

    @Test
    fun selectedIds_clearToEmptySet_isActiveBecomesFalse() {
        val def = riskFilter("high")
        def.selectedIds = emptySet()
        assertFalse(def.isActive)
    }

    // applyColumnFilters — no filters

    @Test
    fun applyColumnFilters_noFilters_allItemsPassThrough() {
        val items = listOf(Item("high", "open"), Item("low", "fixed"))
        assertEquals(items, applyColumnFilters(items, emptyList()))
    }

    // applyColumnFilters — single filter

    @Test
    fun applyColumnFilters_inactiveFilter_allItemsPassThrough() {
        val items = listOf(Item("high", "open"), Item("low", "fixed"))
        assertEquals(items, applyColumnFilters(items, listOf(riskFilter())))
    }

    @Test
    fun applyColumnFilters_singleFilterOneSelection_onlyMatchingItemsPass() {
        val items = listOf(Item("high", "open"), Item("low", "open"), Item("medium", "fixed"))
        assertEquals(
            listOf(Item("high", "open")),
            applyColumnFilters(items, listOf(riskFilter("high")))
        )
    }

    @Test
    fun applyColumnFilters_singleFilterMultipleSelections_itemsMatchingAnyIdPass() {
        val items = listOf(Item("high", "open"), Item("low", "open"), Item("medium", "fixed"))
        assertEquals(
            listOf(Item("high", "open"), Item("medium", "fixed")),
            applyColumnFilters(items, listOf(riskFilter("high", "medium")))
        )
    }

    @Test
    fun applyColumnFilters_singleFilterNoMatch_returnsEmpty() {
        val items = listOf(Item("high", "open"), Item("medium", "open"))
        assertEquals(
            emptyList<Item>(),
            applyColumnFilters(items, listOf(riskFilter("low")))
        )
    }

    // applyColumnFilters — multiple filters (AND logic)

    @Test
    fun applyColumnFilters_twoActiveFilters_itemMustMatchBoth() {
        val items = listOf(
            Item("high", "open"),
            Item("high", "fixed"),
            Item("low", "open"),
        )
        assertEquals(
            listOf(Item("high", "open")),
            applyColumnFilters(items, listOf(riskFilter("high"), statusFilter("open")))
        )
    }

    @Test
    fun applyColumnFilters_oneActiveOneInactiveFilter_onlyActiveFilterApplied() {
        val items = listOf(Item("high", "open"), Item("low", "open"))
        assertEquals(
            listOf(Item("high", "open")),
            applyColumnFilters(items, listOf(riskFilter("high"), statusFilter()))
        )
    }

    @Test
    fun applyColumnFilters_twoFiltersNoItemMatchesBoth_returnsEmpty() {
        val items = listOf(Item("high", "open"), Item("low", "fixed"))
        assertEquals(
            emptyList<Item>(),
            applyColumnFilters(items, listOf(riskFilter("high"), statusFilter("fixed")))
        )
    }

    // applyColumnFilters — filter cleared

    @Test
    fun applyColumnFilters_filterCleared_allItemsPassThrough() {
        val items = listOf(Item("high", "open"), Item("low", "fixed"))
        val def = riskFilter("high")
        def.selectedIds = emptySet()
        assertEquals(items, applyColumnFilters(items, listOf(def)))
    }

    // applyColumnFilters — empty dataset

    @Test
    fun applyColumnFilters_emptyItemsInactiveFilter_returnsEmpty() {
        assertEquals(emptyList<Item>(), applyColumnFilters(emptyList(), listOf(riskFilter())))
    }

    @Test
    fun applyColumnFilters_emptyItemsActiveFilter_returnsEmpty() {
        assertEquals(emptyList<Item>(), applyColumnFilters(emptyList(), listOf(riskFilter("high"))))
    }
}
