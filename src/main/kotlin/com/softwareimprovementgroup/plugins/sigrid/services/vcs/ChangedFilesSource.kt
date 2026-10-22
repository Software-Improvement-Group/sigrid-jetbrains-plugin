package com.softwareimprovementgroup.plugins.sigrid.services.vcs

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * Tells which files the developer is working on. Every implementation lives behind an optional dependency
 * (see withVcs.xml and withGit.xml), so the core plugin never loads VCS classes the IDE doesn't provide.
 * Each source answers only what it knows; the rest stays empty.
 */
interface ChangedFilesSource {
    /** Files changed on the current branch since it left the default branch. Runs git: call off the EDT. */
    fun committedPaths(project: Project): Set<String> = emptySet()

    /**
     * Files committed within the last [RECENT_HISTORY_DAYS] days, or null when this source can't tell (no
     * repository, no history). Runs git: call off the EDT.
     */
    fun recentlyChangedPaths(project: Project): Set<String>? = null

    /** Locally modified files. Cheap: reads cached state. */
    fun uncommittedPaths(project: Project): Set<String> = emptySet()

    /** Calls [onChange] whenever the local changes may have changed, until the project closes. */
    fun onUncommittedChanged(project: Project, onChange: () -> Unit) {}

    companion object {
        const val RECENT_HISTORY_DAYS = 365

        val EP_NAME: ExtensionPointName<ChangedFilesSource> =
            ExtensionPointName.create("com.softwareimprovementgroup.plugins.sigrid.changedFilesSource")
    }
}
