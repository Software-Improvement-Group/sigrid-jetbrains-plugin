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
    private val statusOptions: List<Pair<String, String>>,
    currentStatus: String,
    currentRemark: String,
) : DialogWrapper(project, true) {

    private val statusCombo = ComboBox(statusOptions.map { it.first }.toTypedArray()).apply {
        val currentIndex = statusOptions.indexOfFirst { it.second == currentStatus }
        if (currentIndex >= 0) selectedIndex = currentIndex
    }

    private val remarkArea = JBTextArea(currentRemark, 4, 40).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    init {
        title = "${SigridBundle["finding.edit.title"]}: $displayLocation"
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

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(JBLabel("<html>${description.replace("\n", "<br>")}</html>"), gbc)

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SigridBundle["finding.edit.status.label"]), gbc)

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(statusCombo, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SigridBundle["finding.edit.remark.label"]), gbc)

        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0
        val scrollPane = JBScrollPane(remarkArea).apply { preferredSize = Dimension(400, 80) }
        panel.add(scrollPane, gbc)

        return panel
    }

    fun getResult(): FindingRequest? {
        if (!isOK) return null
        val selectedIndex = statusCombo.selectedIndex
        val statusApiValue = if (selectedIndex >= 0) statusOptions[selectedIndex].second else null
        return FindingRequest(status = statusApiValue, remark = remarkArea.text)
    }
}
