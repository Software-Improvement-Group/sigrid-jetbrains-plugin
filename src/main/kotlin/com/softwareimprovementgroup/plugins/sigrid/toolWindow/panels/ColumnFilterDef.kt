package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

data class FilterOption(val label: String, val id: String)

class ColumnFilterDef<T>(
    val columnName: String,
    val options: List<FilterOption>,
    val getOptionId: T.() -> String,
) {
    var selectedIds: Set<String> = emptySet()
    val isActive: Boolean get() = selectedIds.isNotEmpty()
}
