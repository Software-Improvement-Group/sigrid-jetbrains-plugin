package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

private const val CARD_LOADING = "loading"
private const val CARD_ERROR = "error"
private const val CARD_TABLE = "table"

abstract class SigridPanel<T>(
    protected val project: Project,
    columns: Array<String>,
) : JBPanel<SigridPanel<T>>(BorderLayout()) {

    protected abstract val emptyMessage: String
    protected abstract fun fetch(subsystem: String): List<T>
    protected abstract fun T.toRow(): Array<String>

    private val tableModel = DefaultTableModel(columns, 0)
    private val table = JBTable(tableModel).apply {
        isStriped = true
        setDefaultEditor(Any::class.java, null)
        columnModel.getColumn(0).maxWidth = 80
        columnModel.getColumn(columns.size - 1).maxWidth = 100
    }

    private val cardLayout = CardLayout()
    private val cards = JPanel(cardLayout)
    private val statusLabel = JBLabel().apply { horizontalAlignment = JBLabel.CENTER }

    init {
        val toolbar = JPanel(BorderLayout()).apply {
            add(JButton("Refresh").apply { addActionListener { loadData() } }, BorderLayout.EAST)
        }

        cards.add(JBLabel("Loading…").apply { horizontalAlignment = JBLabel.CENTER }, CARD_LOADING)
        cards.add(statusLabel, CARD_ERROR)
        cards.add(JBScrollPane(table), CARD_TABLE)

        add(toolbar, BorderLayout.NORTH)
        add(cards, BorderLayout.CENTER)

        loadData()
    }

    private fun loadData() {
        showCard(CARD_LOADING)

        ApplicationManager.getApplication().executeOnPooledThread {
            val projectConfig = SigridProjectConfiguration.getInstance(project)
            if (!projectConfig.isConfigurationValid) {
                showError("Sigrid is not configured. Go to Settings → Tools → Sigrid.")
                return@executeOnPooledThread
            }

            try {
                val findings = fetch(projectConfig.subsystem)
                ApplicationManager.getApplication().invokeLater {
                    tableModel.rowCount = 0
                    if (findings.isEmpty()) {
                        showError(emptyMessage)
                    } else {
                        findings.forEach { tableModel.addRow(it.toRow()) }
                        showCard(CARD_TABLE)
                    }
                }
            } catch (e: Exception) {
                showError(toErrorMessage(e))
            }
        }
    }

    private fun showError(message: String) {
        ApplicationManager.getApplication().invokeLater {
            statusLabel.text = message
            statusLabel.foreground = JBColor.RED
            showCard(CARD_ERROR)
        }
    }

    private fun showCard(name: String) {
        ApplicationManager.getApplication().invokeLater {
            cardLayout.show(cards, name)
        }
    }

    private fun toErrorMessage(e: Exception): String {
        val status = e.message?.substringAfter("HTTP ")?.substringBefore(" ")?.toIntOrNull()
        return when (status) {
            401  -> "Unauthorized — please verify your API key."
            403  -> "Forbidden — you do not have access to this system."
            404  -> "Not found — please verify the customer and system name."
            else -> "Failed to load findings: ${e.message}"
        }
    }
}