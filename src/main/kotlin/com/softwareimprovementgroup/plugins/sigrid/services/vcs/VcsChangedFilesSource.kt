package com.softwareimprovementgroup.plugins.sigrid.services.vcs

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListListener
import com.intellij.openapi.vcs.changes.ChangeListManager

class VcsChangedFilesSource : ChangedFilesSource {
    override fun uncommittedPaths(project: Project): Set<String> =
        ChangeListManager.getInstance(project).allChanges
            .mapNotNullTo(mutableSetOf()) { (it.afterRevision ?: it.beforeRevision)?.file?.path }

    override fun onUncommittedChanged(project: Project, onChange: () -> Unit) {
        project.messageBus.connect().subscribe(ChangeListListener.TOPIC, object : ChangeListListener {
            override fun changeListUpdateDone() = onChange()
        })
    }
}
