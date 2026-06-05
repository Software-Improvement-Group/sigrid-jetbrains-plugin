package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

private const val CARD_LOADING = "loading"
private const val CARD_ERROR = "error"
private const val CARD_TABLE = "table"

abstract class SigridPanel<T>(
    protected val project: Project,
    columns: Array<String>,
    centeredColumns: Set<String> = emptySet(),
) : JBPanel<SigridPanel<T>>(BorderLayout()) {

    protected abstract val emptyMessage: String
    protected abstract fun fetch(subsystem: String): List<T>
    protected abstract fun T.toRow(): Array<Any>
    protected abstract fun T.matchesSearch(query: String): Boolean
    protected abstract fun T.getFileLocations(): List<FileLocation>

    protected open fun T.isEditable(): Boolean = false
    protected open fun T.getId(): String = ""
    protected open fun T.getDisplayLocation(): String = ""
    protected open fun T.getEditDescription(): String = ""
    protected open fun T.getStatusOptions(): List<Pair<String, String>> = emptyList()
    protected open fun T.getCurrentStatus(): String = ""
    protected open fun T.getCurrentRemark(): String = ""

    private var allFindings: List<T> = emptyList()
    private var displayedFindings: List<T> = emptyList()

    private val tableModel = object : DefaultTableModel(columns, 0) {
        override fun getColumnClass(column: Int): Class<*> =
            if (rowCount > 0) getValueAt(0, column)?.javaClass ?: Any::class.java else Any::class.java
    }
    private val riskIconRenderer = RiskIconCellRenderer()
    private val centeredCellRenderer = DefaultTableCellRenderer().apply {
        horizontalAlignment = SwingConstants.CENTER
    }

    private val table = JBTable(tableModel).apply {
        isStriped = true
        setDefaultEditor(Any::class.java, null)
        setDefaultRenderer(RiskIcon::class.java, riskIconRenderer)
        columnModel.getColumn(0).maxWidth = 80
        columnModel.getColumn(columns.size - 1).maxWidth = 100
        columns.forEachIndexed { i, name ->
            if (name in centeredColumns) {
                val col = columnModel.getColumn(i)
                col.headerRenderer = centeredCellRenderer
                col.cellRenderer = FindingCellRenderer(centeredCellRenderer, riskIconRenderer)
            }
        }
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val viewRow = rowAtPoint(e.point)
                    if (viewRow < 0) return
                    val modelRow = convertRowIndexToModel(viewRow)
                    val finding = displayedFindings.getOrNull(modelRow) ?: return
                    navigator.navigate(finding.getFileLocations(), e)
                }
            }

            override fun mousePressed(e: MouseEvent) = editPopupHandler.maybeShow(e)
            override fun mouseReleased(e: MouseEvent) = editPopupHandler.maybeShow(e)
        })
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    val viewRow = selectedRow
                    if (viewRow < 0) return
                    val modelRow = convertRowIndexToModel(viewRow)
                    val finding = displayedFindings.getOrNull(modelRow) ?: return
                    navigator.navigate(finding.getFileLocations(), null)
                }
            }
        })
    }
    private val navigator: FindingNavigator by lazy { FindingNavigator(project, table) }
    private val editPopupHandler: FindingEditPopupHandler<T> by lazy {
        FindingEditPopupHandler(
            project = project,
            table = table,
            getDisplayedFindings = { displayedFindings },
            isEditable = { it.isEditable() },
            getId = { it.getId() },
            getDisplayLocation = { it.getDisplayLocation() },
            getEditDescription = { it.getEditDescription() },
            getStatusOptions = { it.getStatusOptions() },
            getCurrentStatus = { it.getCurrentStatus() },
            getCurrentRemark = { it.getCurrentRemark() },
            onReload = ::loadData,
        )
    }

    private val editButton = JButton(SigridBundle["finding.edit.button"]).apply {
        isEnabled = false
    }

    private val cardLayout = CardLayout()
    private val cards = JPanel(cardLayout)
    private val statusLabel = JBLabel().apply { horizontalAlignment = JBLabel.CENTER }

    private val searchField = JBTextField().apply {
        emptyText.text = SigridBundle["panel.search.placeholder"]
    }

    // Suppresses onSearchChange during setSearchText to avoid feedback loops
    private var suppressSearchCallback = false

    var onRefresh: () -> Unit = ::loadData
    var onSearchChange: (String) -> Unit = {}

    init {
        editButton.addActionListener { editPopupHandler.triggerEditForSelectedRow() }
        table.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_F2) editPopupHandler.triggerEditForSelectedRow()
            }
        })
        table.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val viewRow = table.selectedRow
                val editable = viewRow >= 0 && run {
                    val modelRow = table.convertRowIndexToModel(viewRow)
                    displayedFindings.getOrNull(modelRow)?.isEditable() == true
                }
                editButton.isEnabled = editable
            }
        }

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onSearchFieldChanged()
            override fun removeUpdate(e: DocumentEvent) = onSearchFieldChanged()
            override fun changedUpdate(e: DocumentEvent) = onSearchFieldChanged()
        })

        val toolbar = JPanel(BorderLayout()).apply {
            val buttons = JPanel().apply {
                add(editButton)
                add(JButton(SigridBundle["panel.refresh.button"]).apply { addActionListener { onRefresh() } })
            }
            add(searchField, BorderLayout.CENTER)
            add(buttons, BorderLayout.EAST)
        }

        cards.add(JBLabel(SigridBundle["panel.loading"]).apply { horizontalAlignment = JBLabel.CENTER }, CARD_LOADING)
        cards.add(statusLabel, CARD_ERROR)
        cards.add(JBScrollPane(table), CARD_TABLE)

        add(toolbar, BorderLayout.NORTH)
        add(cards, BorderLayout.CENTER)

        loadData()
    }

    private fun onSearchFieldChanged() {
        applyFilter()
        if (!suppressSearchCallback) {
            onSearchChange(searchField.text)
        }
    }

    fun setSearchText(text: String) {
        suppressSearchCallback = true
        try {
            searchField.text = text
        } finally {
            suppressSearchCallback = false
        }
        applyFilter()
    }

    private fun applyFilter() {
        val query = searchField.text.trim()
        val filtered = if (query.isEmpty()) allFindings else allFindings.filter { it.matchesSearch(query) }

        val selectedId = table.selectedRow
            .takeIf { it >= 0 }
            ?.let { table.convertRowIndexToModel(it) }
            ?.let { displayedFindings.getOrNull(it)?.getId()?.takeIf { id -> id.isNotEmpty() } }

        tableModel.rowCount = 0
        if (filtered.isEmpty()) {
            displayedFindings = emptyList()
            if (allFindings.isEmpty()) {
                showError(emptyMessage)
            } else {
                showError(SigridBundle["panel.no.findings.match", query])
            }
        } else {
            displayedFindings = filtered
            filtered.forEach { tableModel.addRow(it.toRow()) }
            showCard(CARD_TABLE)
            selectRowById(selectedId, filtered)
        }
    }

    fun loadData() {
        showCard(CARD_LOADING)

        ApplicationManager.getApplication().executeOnPooledThread {
            val projectConfig = SigridProjectConfiguration.getInstance(project)
            if (!projectConfig.isConfigurationValid) {
                showError(SigridBundle["panel.not.configured"])
                return@executeOnPooledThread
            }

            try {
                val findings = fetch(projectConfig.subsystem)
                ApplicationManager.getApplication().invokeLater {
                    allFindings = findings
                    applyFilter()
                }
            } catch (e: Exception) {
                showError(toErrorMessage(e))
            }
        }
    }

    private fun selectRowById(
        selectedId: String?,
        filtered: List<T>
    ) {
        if (selectedId != null) {
            val rowToSelect = filtered.indexOfFirst { it.getId() == selectedId }
            if (rowToSelect >= 0) {
                val viewRow = table.convertRowIndexToView(rowToSelect)
                table.selectionModel.setSelectionInterval(viewRow, viewRow)
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true))
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
            401  -> SigridBundle["panel.error.unauthorized"]
            403  -> SigridBundle["panel.error.forbidden"]
            404  -> SigridBundle["panel.error.not.found"]
            else -> SigridBundle["panel.error.generic", e.message ?: ""]
        }
    }

}