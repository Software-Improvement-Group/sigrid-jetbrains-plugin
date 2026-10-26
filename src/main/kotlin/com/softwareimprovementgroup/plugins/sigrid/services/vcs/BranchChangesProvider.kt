package com.softwareimprovementgroup.plugins.sigrid.services.vcs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.models.ChangedFiles

/**
 * Finds the files the developer is working on and remembers the answer in [current]. Never throws: when the
 * VCS can't answer, that part is empty.
 */
@Service(Service.Level.PROJECT)
class BranchChangesProvider(private val project: Project) {
    @Volatile
    var current: ChangedFiles = ChangedFiles.EMPTY
        private set

    /** Files touched within [ChangedFilesSource.RECENT_HISTORY_DAYS]; null when no source could tell. */
    @Volatile
    var recentHistory: ChangedFiles? = null
        private set

    @Volatile
    private var committed: Set<String> = emptySet()

    /** Re-reads both the branch diff and the local changes. Runs git: call off the EDT. */
    fun refresh(): ChangedFiles {
        committed = committedPaths()
        recentHistory = readRecentHistory()
        return refreshUncommitted()
    }

    /** Re-reads only the local changes (cheap), keeping the last branch diff. */
    fun refreshUncommitted(): ChangedFiles =
        ChangedFiles(committed + uncommittedPaths()).also { current = it }

    /** Calls [onChange] on a pooled thread when the local changes differ from the last [current]. */
    fun onUncommittedChanged(onChange: () -> Unit) {
        forEachSource { it.onUncommittedChanged(project) { refreshIfChanged(onChange) } }
    }

    /** Calls [onChange] when repositories show up after a read that found no history (the startup race). */
    fun onHistoryAvailable(onChange: () -> Unit) {
        forEachSource { it.onRepositoriesReady(project) { if (recentHistory == null) onChange() } }
    }

    private fun refreshIfChanged(onChange: () -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val before = current.paths
            if (refreshUncommitted().paths != before) onChange()
        }
    }

    private fun readRecentHistory(): ChangedFiles? {
        var answered = false
        val paths = mutableSetOf<String>()
        forEachSource { source -> source.recentlyChangedPaths(project)?.let { answered = true; paths += it } }
        return if (answered) ChangedFiles(paths) else null
    }

    private fun committedPaths(): Set<String> = collectPaths { it.committedPaths(project) }

    private fun uncommittedPaths(): Set<String> = collectPaths { it.uncommittedPaths(project) }

    private fun collectPaths(read: (ChangedFilesSource) -> Set<String>): Set<String> {
        val paths = mutableSetOf<String>()
        forEachSource { paths += read(it) }
        return paths
    }

    private fun forEachSource(action: (ChangedFilesSource) -> Unit) {
        ChangedFilesSource.EP_NAME.extensionList.distinctBy { it.javaClass }.forEach { source ->
            try {
                action(source)
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Exception) {
                thisLogger().warn("Could not read changed files with ${source.javaClass.simpleName}", e)
            }
        }
    }

    companion object {
        fun getInstance(project: Project): BranchChangesProvider = project.service()
    }
}
