package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

/**
 * Shows the first [limit] findings until the user expands. Narrowing (search, filters) counts as expanding,
 * so a search can always reach findings that the default view hides. A null [limit] never caps.
 */
internal class ProgressiveDisclosure(private val limit: Int?) {
    var expanded = false

    fun <T> visible(ranked: List<T>, narrowed: Boolean): List<T> =
        if (limit == null || expanded || narrowed) ranked else ranked.take(limit)
}
