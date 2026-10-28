package com.softwareimprovementgroup.plugins.sigrid.models

/**
 * Files the developer is currently changing: uncommitted edits plus everything on the branch since it left
 * the default branch. Paths may be absolute or repo-relative, so matching is suffix-based - the same rule used
 * for the "active file" filter.
 */
class ChangedFiles(val paths: Set<String>) {
    fun matches(findingPath: String): Boolean = findingPath.isNotBlank() && paths.any { pathMatches(findingPath, it) }

    fun isEmpty(): Boolean = paths.isEmpty()

    companion object {
        val EMPTY = ChangedFiles(emptySet())

        /** [findingPath] may be shorter than [filePath] (Sigrid reports paths relative to its own root). */
        fun pathMatches(findingPath: String, filePath: String): Boolean =
            filePath == findingPath || filePath.endsWith("/$findingPath")
    }
}
