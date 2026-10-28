package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

/**
 * Shows the first [limit] findings until the user expands, and lets them collapse again. Narrowing (search,
 * filters) counts as expanding, so a search can always reach findings that the default view hides. A null
 * [limit] never caps.
 */
internal class ProgressiveDisclosure(val limit: Int?) {
    var expanded = false
        private set

    fun toggle() {
        expanded = !expanded
    }

    fun reset() {
        expanded = false
    }

    /** True when there are more findings than the limit, so a Show all / Show top toggle makes sense. */
    fun canToggle(total: Int, narrowed: Boolean): Boolean = limit != null && !narrowed && total > limit

    fun <T> visible(ranked: List<T>, narrowed: Boolean): List<T> =
        if (limit == null || expanded || narrowed) ranked else ranked.take(limit)
}
