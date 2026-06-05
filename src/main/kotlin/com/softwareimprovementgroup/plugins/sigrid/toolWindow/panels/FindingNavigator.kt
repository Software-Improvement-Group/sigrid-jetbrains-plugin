package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.awt.RelativePoint
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import java.awt.event.MouseEvent
import javax.swing.JComponent

class FindingNavigator(private val project: Project, private val anchor: JComponent) {

    fun navigate(locations: List<FileLocation>, event: MouseEvent?) {
        val valid = filterValidLocations(locations)
        if (valid.isEmpty()) return
        if (valid.size == 1) {
            openFileLocation(valid[0])
        } else {
            showLocationPopup(valid, event)
        }
    }

    private fun openFileLocation(location: FileLocation) {
        val basePath = project.basePath ?: return
        val absolutePath = "$basePath/${location.filePath}"
        val vFile = LocalFileSystem.getInstance().findFileByPath(absolutePath) ?: return
        OpenFileDescriptor(project, vFile, editorLine(location), 0).navigate(true)
    }

    private fun showLocationPopup(locations: List<FileLocation>, event: MouseEvent?) {
        val renderer = object : javax.swing.DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: javax.swing.JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): java.awt.Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                val loc = value as? FileLocation ?: return this
                text = popupItemText(loc)
                toolTipText = loc.filePath
                border = javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8)
                return this
            }
        }
        val popup = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(locations)
            .setTitle(SigridBundle["panel.location.popup.title"])
            .setRenderer(renderer)
            .setItemChosenCallback { openFileLocation(it) }
            .createPopup()
        if (event != null) {
            popup.show(RelativePoint(event.component, event.point))
        } else {
            popup.showInCenterOf(anchor)
        }
    }

    companion object {
        fun filterValidLocations(locations: List<FileLocation>): List<FileLocation> =
            locations.filter { it.filePath.isNotBlank() }

        fun editorLine(location: FileLocation): Int =
            if (location.startLine != null && location.startLine > 0) location.startLine - 1 else 0

        fun popupItemText(location: FileLocation): String {
            val line = if (location.startLine != null && location.startLine > 0) ":${location.startLine}" else ""
            return "${location.filePath.substringAfterLast("/")}$line"
        }
    }
}