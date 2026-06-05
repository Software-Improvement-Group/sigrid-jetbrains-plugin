package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.table.JBTable
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class FindingEditPopupHandler<T>(
    private val project: Project,
    private val table: JBTable,
    private val getDisplayedFindings: () -> List<T>,
    private val isEditable: (T) -> Boolean,
    private val getId: (T) -> String,
    private val getDisplayLocation: (T) -> String,
    private val getEditDescription: (T) -> String,
    private val getStatusOptions: (T) -> List<Pair<String, String>>,
    private val getCurrentStatus: (T) -> String,
    private val getCurrentRemark: (T) -> String,
    private val onReload: () -> Unit,
) {
    fun maybeShow(e: MouseEvent) {
        if (!e.isPopupTrigger) return
        val viewRow = table.rowAtPoint(e.point)
        if (viewRow < 0) return
        table.setRowSelectionInterval(viewRow, viewRow)
        val modelRow = table.convertRowIndexToModel(viewRow)
        val finding = getDisplayedFindings().getOrNull(modelRow) ?: return
        if (!isEditable(finding)) return
        val popup = JPopupMenu()
        val editItem = JMenuItem(SigridBundle["finding.edit.menu.item"])
        editItem.addActionListener {
            val dialog = EditFindingDialog(
                project,
                getDisplayLocation(finding),
                getEditDescription(finding),
                getStatusOptions(finding),
                getCurrentStatus(finding),
                getCurrentRemark(finding),
            )
            if (dialog.showAndGet()) {
                val request = dialog.getResult() ?: return@addActionListener
                ApplicationManager.getApplication().executeOnPooledThread {
                    SigridApiService.getInstance().editFinding(project, getId(finding), request)
                    ApplicationManager.getApplication().invokeLater { onReload() }
                }
            }
        }
        popup.add(editItem)
        popup.show(e.component, e.x, e.y)
    }
}