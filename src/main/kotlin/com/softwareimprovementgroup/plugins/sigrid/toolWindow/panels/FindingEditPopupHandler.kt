package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.table.JBTable
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.KeyStroke

private const val MAX_EDIT_ITEMS_SIZE = 25

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
        if (!table.isRowSelected(viewRow)) table.setRowSelectionInterval(viewRow, viewRow)
        if (!hasEditableFindings()) return
        val popup = JPopupMenu()
        val editItem = JMenuItem(SigridBundle["finding.edit.menu.item"])
        editItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)
        editItem.addActionListener { triggerEditForSelectedRow() }
        popup.add(editItem)
        popup.show(e.component, e.x, e.y)
    }

    fun triggerEditForSelectedRow() {
        val findings = selectedEditableFindings() ?: return
        if (findings.isEmpty()) return
        triggerEdit(findings)
    }

    private fun hasEditableFindings(): Boolean {
        val displayedFindings = getDisplayedFindings()
        return table.selectedRows
            .map { table.convertRowIndexToModel(it) }
            .mapNotNull { displayedFindings.getOrNull(it) }
            .any { isEditable(it) }
    }

    private fun selectedEditableFindings(): List<T>? {
        val displayedFindings = getDisplayedFindings()
        val findings = table.selectedRows
            .map { table.convertRowIndexToModel(it) }
            .mapNotNull { displayedFindings.getOrNull(it) }
            .filter { isEditable(it) }
        if (findings.size > MAX_EDIT_ITEMS_SIZE) {
            Messages.showErrorDialog(table, SigridBundle["finding.edit.too.many", MAX_EDIT_ITEMS_SIZE])
            return null
        }
        return findings
    }

    private fun triggerEdit(findings: List<T>) {
        val count = findings.size
        val statusOptions = getStatusOptions(findings.first())
        val commonStatus = findings.map { getCurrentStatus(it) }.toSet().singleOrNull()
        val commonRemark = findings.map { getCurrentRemark(it) }.toSet().singleOrNull()
        val displayLocation = if (count == 1) getDisplayLocation(findings.first()) else ""
        val description = if (count == 1) getEditDescription(findings.first()) else ""

        val dialog = EditFindingDialog(
            project = project,
            displayLocation = displayLocation,
            description = description,
            statusOptions = statusOptions,
            currentStatus = commonStatus,
            currentRemark = commonRemark,
            count = count,
        )
        if (dialog.showAndGet()) {
            val request = dialog.getResult() ?: return
            ApplicationManager.getApplication().executeOnPooledThread {
                for (finding in findings) {
                    SigridApiService.getInstance().editFinding(project, getId(finding), request)
                }
                ApplicationManager.getApplication().invokeLater { onReload() }
            }
        }
    }
}