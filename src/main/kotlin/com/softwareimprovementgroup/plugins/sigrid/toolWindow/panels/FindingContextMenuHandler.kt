package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.table.JBTable
import com.softwareimprovementgroup.plugins.sigrid.NOTIFICATION_GROUP_ID
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.KeyStroke

class FindingContextMenuHandler<T>(
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
    private val getFileLocations: (T) -> List<FileLocation>,
    private val getHref: (T) -> String?,
    private val navigator: FindingNavigator,
    private val openCreateJiraIssue: () -> Unit,
) {
    fun handlePopupTrigger(e: MouseEvent) {
        if (!e.isPopupTrigger) return
        val viewRow = table.rowAtPoint(e.point)
        if (viewRow < 0) return
        if (!table.isRowSelected(viewRow)) table.setRowSelectionInterval(viewRow, viewRow)

        val navigableLocations = navigableLocationsAtPoint(e)
        val href = hrefAtPoint(e)
        val hasEditable = hasEditableFindings()
        val isJiraConfigured = SigridProjectConfiguration.getInstance(project).isJiraConfigured
        if (navigableLocations == null && href == null && !hasEditable && !isJiraConfigured) return

        val popup = JPopupMenu()
        if (navigableLocations != null) {
            val navigateItem = JMenuItem(SigridBundle["finding.navigate.menu.item"])
            navigateItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
            navigateItem.addActionListener { navigator.navigate(navigableLocations, e) }
            popup.add(navigateItem)
        }

        if (hasEditable) {
            val editItem = JMenuItem(SigridBundle["finding.edit.menu.item"])
            editItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)
            editItem.addActionListener { triggerEditForSelectedRow() }
            popup.add(editItem)
        }

        val openItem = JMenuItem(SigridBundle["finding.open.in.sigrid.menu.item"])
        openItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)
        openItem.isEnabled = href != null
        openItem.addActionListener { href?.let { BrowserUtil.browse(it) } }
        popup.add(openItem)

        if (isJiraConfigured) {
            val jiraItem = JMenuItem(SigridBundle["jira.create.menu.item"])
            jiraItem.addActionListener { openCreateJiraIssue() }
            popup.add(jiraItem)
        }

        popup.show(e.component, e.x, e.y)
    }

    fun triggerEditForSelectedRow() {
        val findings = selectedEditableFindings() ?: return
        if (findings.isEmpty()) return
        triggerEdit(findings)
    }

    private fun hrefAtPoint(e: MouseEvent): String? {
        val viewRow = table.rowAtPoint(e.point)
        if (viewRow < 0) return null
        val modelRow = table.convertRowIndexToModel(viewRow)
        val finding = getDisplayedFindings().getOrNull(modelRow) ?: return null
        return getHref(finding)?.takeIf { it.isNotEmpty() }
    }

    private fun navigableLocationsAtPoint(e: MouseEvent): List<FileLocation>? {
        val viewRow = table.rowAtPoint(e.point)
        if (viewRow < 0) return null
        val displayedFindings = getDisplayedFindings()
        val modelRow = table.convertRowIndexToModel(viewRow)
        val finding = displayedFindings.getOrNull(modelRow) ?: return null
        val locations = FindingNavigator.filterValidLocations(getFileLocations(finding))
        return if (locations.isNotEmpty()) locations else null
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
        val commonStatus = detectCommonValue(findings.map { getCurrentStatus(it) })
        val commonRemark = detectCommonValue(findings.map { getCurrentRemark(it) })
        val displayLocation = if (count == 1) getDisplayLocation(findings.first()) else ""
        val description = if (count == 1) getEditDescription(findings.first()) else ""

        if (SigridProjectConfiguration.getInstance(project).isUrlOverrideWithoutKeyOverride) {
            Messages.showErrorDialog(table, SigridBundle["panel.error.url.override.no.key"])
            return
        }
        val dialog = EditFindingDialog(
            project = project,
            displayLocation = displayLocation,
            description = description,
            statusOptions = statusOptions,
            currentStatus = commonStatus,
            currentRemark = commonRemark,
            count = count,
            saveAction = { request, onSuccess, onFailure ->
                try {
                    for (finding in findings) {
                        SigridApiService.getInstance().editFinding(project, getId(finding), request)
                    }
                    onSuccess()
                } catch (e: Exception) {
                    onFailure(e)
                }
            },
        )
        if (dialog.showAndGet()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(if (count == 1) SigridBundle["finding.edit.success"] else SigridBundle["finding.edit.success.plural", count], NotificationType.INFORMATION)
                .notify(project)
            onReload()
        }
    }

    companion object {
        internal const val MAX_EDIT_ITEMS_SIZE = 25

        internal fun detectCommonValue(values: List<String>): String? =
            values.toSet().singleOrNull()
    }
}