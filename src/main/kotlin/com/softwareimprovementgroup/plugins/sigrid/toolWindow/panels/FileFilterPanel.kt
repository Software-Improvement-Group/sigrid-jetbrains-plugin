package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.panel
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import java.awt.FlowLayout
import javax.swing.JPanel

private enum class FileFilter { ALL, ACTIVE }

class FileFilterPanel(
    private val project: Project,
    private val onChanged: () -> Unit,
) : JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)) {

    private val propertyGraph = PropertyGraph()
    private val selectedFileFilterProperty = propertyGraph.property(FileFilter.ALL)
    private lateinit var fileFilterSegmentedButton: SegmentedButton<FileFilter>
    private val activeFileLabel = JBLabel("").apply {
        isVisible = false
        foreground = JBColor.GRAY
    }

    internal var activeFileOnly: Boolean = false
        private set

    private var suppressCallback = false
    var onFileFilterChange: (Boolean) -> Unit = {}

    init {
        val filterButton = panel {
            row {
                fileFilterSegmentedButton = segmentedButton(FileFilter.entries.toList()) { value ->
                    val label = when (value) {
                        FileFilter.ALL -> SigridBundle["panel.filter.all"]
                        FileFilter.ACTIVE -> SigridBundle["panel.filter.active"]
                    }
                    text = if (selectedFileFilterProperty.get() == value) "● $label" else label
                    toolTipText = SigridBundle["panel.filter.active.tooltip"]
                }.bind(selectedFileFilterProperty)
            }
        }
        add(filterButton)
        add(activeFileLabel)

        selectedFileFilterProperty.afterChange { value ->
            activeFileOnly = value == FileFilter.ACTIVE
            fileFilterSegmentedButton.update(*FileFilter.entries.toTypedArray())
            updateActiveFileLabel()
            onChanged()
            if (!suppressCallback) onFileFilterChange(activeFileOnly)
        }

        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    if (activeFileOnly) {
                        updateActiveFileLabel()
                        onChanged()
                    }
                }
            }
        )
    }

    fun setActiveFileOnly(value: Boolean) {
        suppressCallback = true
        try {
            selectedFileFilterProperty.set(if (value) FileFilter.ACTIVE else FileFilter.ALL)
        } finally {
            suppressCallback = false
        }
    }

    internal fun activeFilePath(): String? {
        val vFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: return null
        val base = project.basePath ?: return null
        return relativePath(vFile.path, base)
    }

    companion object {
        internal fun relativePath(absolutePath: String, basePath: String): String =
            absolutePath.removePrefix("$basePath/")

        internal fun matchesActivePath(locFilePath: String, activePath: String): Boolean =
            locFilePath == activePath || activePath.endsWith("/$locFilePath")
    }

    private fun updateActiveFileLabel() {
        if (activeFileOnly) {
            val fileName = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()?.name
            activeFileLabel.text = if (fileName != null)
                SigridBundle["panel.filter.active.file", fileName]
            else
                SigridBundle["panel.filter.active.no.file"]
            activeFileLabel.isVisible = true
        } else {
            activeFileLabel.isVisible = false
        }
    }
}
