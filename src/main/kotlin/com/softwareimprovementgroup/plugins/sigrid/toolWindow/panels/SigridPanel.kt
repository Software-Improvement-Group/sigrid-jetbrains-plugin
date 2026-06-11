package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
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
                if (e.button == MouseEvent.BUTTON1 && e.clickCount == 2) {
                    val viewRow = rowAtPoint(e.point)
                    if (viewRow < 0) return
                    val modelRow = convertRowIndexToModel(viewRow)
                    val finding = displayedFindings.getOrNull(modelRow) ?: return
                    navigator.navigate(finding.getFileLocations(), e)
                }
            }

            override fun mousePressed(e: MouseEvent) = contextMenuHandler.handlePopupTrigger(e)
            override fun mouseReleased(e: MouseEvent) = contextMenuHandler.handlePopupTrigger(e)
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
    private val contextMenuHandler: FindingContextMenuHandler<T> by lazy {
        FindingContextMenuHandler(
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
            getFileLocations = { it.getFileLocations() },
            navigator = navigator,
        )
    }

    private val editButton = JButton(SigridBundle["finding.edit.button"]).apply {
        isEnabled = false
        toolTipText = SigridBundle["finding.edit.button.tooltip"]
    }

    private val fileFilterPanel = FileFilterPanel(project) { applyFilter() }

    private val cardLayout = CardLayout()
    private val cards = JPanel(cardLayout)
    private val statusLabel = JBLabel().apply { horizontalAlignment = JBLabel.CENTER }

    private val searchField = SearchTextField(false).apply {
        textEditor.emptyText.text = SigridBundle["panel.search.placeholder"]
    }

    // Suppresses onSearchChange during setSearchText to avoid feedback loops
    private var suppressSearchCallback = false

    var onSearchChange: (String) -> Unit = {}
    var onFileFilterChange: (Boolean) -> Unit = {}
        set(value) { field = value; fileFilterPanel.onFileFilterChange = value }

    init {
        setupEditButton()
        setupSearchField()
        setupLayout()
        loadData()
    }

    private fun setupEditButton() {
        editButton.addActionListener { contextMenuHandler.triggerEditForSelectedRow() }
        table.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_F2) contextMenuHandler.triggerEditForSelectedRow()
            }
        })
        table.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val editable = table.selectedRows.any { viewRow ->
                    val modelRow = table.convertRowIndexToModel(viewRow)
                    displayedFindings.getOrNull(modelRow)?.isEditable() == true
                }
                editButton.isEnabled = editable
            }
        }
    }

    private fun setupSearchField() {
        searchField.textEditor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onSearchFieldChanged()
            override fun removeUpdate(e: DocumentEvent) = onSearchFieldChanged()
            override fun changedUpdate(e: DocumentEvent) = onSearchFieldChanged()
        })
        searchField.preferredSize = java.awt.Dimension(220, searchField.preferredSize.height)
    }

    private fun setupLayout() {
        val toolbar = JPanel(BorderLayout()).apply {
            add(editButton, BorderLayout.WEST)
            add(fileFilterPanel, BorderLayout.CENTER)
            add(searchField, BorderLayout.EAST)
        }
        cards.add(JBLabel(SigridBundle["panel.loading"]).apply { horizontalAlignment = JBLabel.CENTER }, CARD_LOADING)
        cards.add(statusLabel, CARD_ERROR)
        cards.add(JBScrollPane(table), CARD_TABLE)
        add(toolbar, BorderLayout.NORTH)
        add(cards, BorderLayout.CENTER)
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

    fun setActiveFileOnly(value: Boolean) = fileFilterPanel.setActiveFileOnly(value)

    private fun applyFilter() {
        val query = searchField.text.trim()

        val afterActiveFilter = if (fileFilterPanel.activeFileOnly) {
            val activePath = fileFilterPanel.activeFilePath()
            if (activePath != null) {
                allFindings.filter { finding ->
                    finding.getFileLocations().any { loc ->
                        loc.filePath == activePath || activePath.endsWith("/${loc.filePath}")
                    }
                }
            } else allFindings
        } else allFindings

        val filtered = if (query.isEmpty()) afterActiveFilter else afterActiveFilter.filter { it.matchesSearch(query) }

        val selectedIds = table.selectedRows
            .map { table.convertRowIndexToModel(it) }
            .mapNotNull { displayedFindings.getOrNull(it)?.getId()?.takeIf { id -> id.isNotEmpty() } }
            .toSet()

        tableModel.rowCount = 0
        if (filtered.isEmpty()) {
            displayedFindings = emptyList()
            if (afterActiveFilter.isEmpty()) {
                showSuccess(emptyMessage)
            } else {
                showError(SigridBundle["panel.no.findings.match", query])
            }
        } else {
            displayedFindings = filtered
            filtered.forEach { tableModel.addRow(it.toRow()) }
            showCard(CARD_TABLE)
            selectRowsById(selectedIds, filtered)
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

    private fun selectRowsById(selectedIds: Set<String>, filtered: List<T>) {
        if (selectedIds.isEmpty()) return
        table.clearSelection()
        var firstSelectedViewRow = -1
        filtered.forEachIndexed { modelRow, finding ->
            if (finding.getId() in selectedIds) {
                val viewRow = table.convertRowIndexToView(modelRow)
                if (viewRow >= 0) {
                    table.selectionModel.addSelectionInterval(viewRow, viewRow)
                    if (firstSelectedViewRow < 0) firstSelectedViewRow = viewRow
                }
            }
        }
        if (firstSelectedViewRow >= 0) {
            table.scrollRectToVisible(table.getCellRect(firstSelectedViewRow, 0, true))
        }
    }

    private fun showSuccess(message: String) =
        showCard(message, JBColor.GREEN)

    private fun showError(message: String) =
        showCard(message, JBColor.RED)

    private fun showCard(message: String, color: JBColor) {
        ApplicationManager.getApplication().invokeLater {
            statusLabel.text = message
            statusLabel.foreground = color
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
