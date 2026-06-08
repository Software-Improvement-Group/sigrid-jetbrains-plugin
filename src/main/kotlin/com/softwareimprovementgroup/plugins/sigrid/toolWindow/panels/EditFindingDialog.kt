package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FindingRequest
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

private const val MAX_WIDTH = 600
private const val MIN_HEIGHT = 200

class EditFindingDialog(
    project: Project,
    displayLocation: String,
    private val description: String,
    statusOptions: List<Pair<String, String>>,
    // null means statuses differ across selected findings
    currentStatus: String?,
    // null means remarks differ across selected findings
    currentRemark: String?,
    private val count: Int = 1,
) : DialogWrapper(project, true) {

    private val isMixedStatus = currentStatus == null
    private val isMixedRemark = currentRemark == null

    // When statuses are mixed, prepend a "(mixed)" sentinel so the user can choose to leave them unchanged.
    private val effectiveStatusOptions: List<Pair<String, String>> =
        if (isMixedStatus) listOf(SigridBundle["finding.edit.status.mixed"] to "") + statusOptions else statusOptions

    private val statusCombo = ComboBox(effectiveStatusOptions.map { it.first }.toTypedArray()).apply {
        val currentIndex = effectiveStatusOptions.indexOfFirst { it.second == currentStatus }
        if (currentIndex >= 0) selectedIndex = currentIndex
        // When mixed, index 0 (the sentinel) is already selected by default — correct behaviour.
    }

    private val remarkArea = JBTextArea(currentRemark ?: "", 4, 40).apply {
        lineWrap = true
        wrapStyleWord = true
        if (isMixedRemark) emptyText.text = SigridBundle["finding.edit.remark.mixed"]
    }

    init {
        title = if (count > 1)
            SigridBundle["finding.edit.title.multi", count]
        else
            "${SigridBundle["finding.edit.title"]}: $displayLocation"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = object : JPanel(GridBagLayout()) {
            override fun getPreferredSize(): Dimension {
                val size = super.getPreferredSize()
                return Dimension(minOf(size.width, MAX_WIDTH), maxOf(size.height, MIN_HEIGHT))
            }
        }
        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(4)
        }

        var nextRow = 0

        if (count == 1) {
            gbc.gridx = 0; gbc.gridy = nextRow++; gbc.gridwidth = 2
            gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
            panel.add(JBLabel("<html>${description.replace("\n", "<br>")}</html>"), gbc)
            gbc.gridwidth = 1
        }

        gbc.gridx = 0; gbc.gridy = nextRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SigridBundle["finding.edit.status.label"]), gbc)

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(statusCombo, gbc)

        nextRow++

        gbc.gridx = 0; gbc.gridy = nextRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SigridBundle["finding.edit.remark.label"]), gbc)

        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0
        val scrollPane = JBScrollPane(remarkArea).apply { preferredSize = Dimension(400, 80) }
        panel.add(scrollPane, gbc)

        return panel
    }

    fun getResult(): FindingRequest? {
        if (!isOK) return null
        val selectedIndex = statusCombo.selectedIndex
        val statusApiValue = when {
            selectedIndex < 0 -> null
            isMixedStatus && selectedIndex == 0 -> null  // sentinel selected — leave status unchanged
            else -> effectiveStatusOptions[selectedIndex].second
        }
        // For mixed remarks, blank means "don't change"; non-blank applies to all.
        val remarkValue = when {
            isMixedRemark && remarkArea.text.isEmpty() -> null
            else -> remarkArea.text
        }
        return FindingRequest(status = statusApiValue, remark = remarkValue)
    }
}