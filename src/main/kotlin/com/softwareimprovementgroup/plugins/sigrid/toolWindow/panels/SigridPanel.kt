package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.toErrorMessage as httpErrorMessage
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.FixItContext
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import com.softwareimprovementgroup.plugins.sigrid.services.aiAgents.AiAgentAvailabilityListener
import com.softwareimprovementgroup.plugins.sigrid.services.aiAgents.AiAgentAvailabilityTopic
import com.softwareimprovementgroup.plugins.sigrid.services.aiAgents.AiAgentRegistry
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridSettingsListener
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridSettingsTopic
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Container
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.FlowLayout
import java.awt.FocusTraversalPolicy
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JButton
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer

private const val CARD_LOADING = "loading"
private const val CARD_ERROR = "error"
private const val CARD_NOT_CONFIGURED = "notConfigured"
private const val CARD_TABLE = "table"
private const val GROUP_CHECKBOX_GAP = 16

abstract class SigridPanel<T>(
    protected val project: Project,
    columns: Array<String>,
    centeredColumns: Set<String> = emptySet(),
    private val columnFilters: List<ColumnFilterDef<T>> = emptyList(),
    private val columnMaxWidths: Map<String, Int> = emptyMap(),
    private val fileGroupingSupported: Boolean = false,
    initialLimit: Int? = null,
) : JBPanel<SigridPanel<T>>(BorderLayout()) {

    protected abstract val emptyMessage: String

    protected abstract fun fetch(subsystem: String): List<T>
    protected abstract fun T.toRow(): Array<Any>
    protected abstract fun T.matchesSearch(query: String): Boolean
    protected abstract fun T.getFileLocations(): List<FileLocation>
    protected abstract fun T.toIssueFinding(): IssueFinding
    protected abstract fun T.toFixItContext(): FixItContext

    protected open fun T.isEditable(): Boolean = false
    protected open fun T.getId(): String = ""
    protected open fun T.getDisplayLocation(): String = ""
    protected open fun T.getEditDescription(): String = ""
    protected open fun T.getStatusOptions(): List<Pair<String, String>> = emptyList()
    protected open fun T.getCurrentStatus(): String = ""
    protected open fun T.getCurrentRemark(): String = ""
    protected open fun T.getHref(): String? = null
    protected open fun T.getGroupKey(): String = ""

    /** Findings the user should see first, in a section above the ranked list; they never count toward the cap. */
    protected open fun T.isPinned(): Boolean = false
    protected open val pinnedTitle: String get() = ""

    /** Order of file groups; null keeps the order in which each group's first finding appears. */
    protected open fun groupOrder(): Comparator<Map.Entry<String, List<T>>>? = null

    private var allFindings: List<T> = emptyList()
    // Overlapping loads (refresh, settings change, git becoming ready) must not let an older one overwrite a newer result.
    private val loadGeneration = AtomicInteger()
    private var groupByFile = false
    private val disclosure = ProgressiveDisclosure(initialLimit)

    private val riskIconRenderer = RiskIconCellRenderer()
    private val centeredCellRenderer = DefaultTableCellRenderer().apply {
        horizontalAlignment = SwingConstants.CENTER
    }

    // The first left-aligned column hosts the expander, so its text reads naturally next to the group arrows.
    private val table = FindingTreeTable<T>(columns, columns.indexOfFirst { it !in centeredColumns }.coerceAtLeast(0)).apply {
        isStriped = true
        setDefaultRenderer(RiskIcon::class.java, riskIconRenderer)
        columns.forEachIndexed { i, name ->
            // Auto-resize starts from preferredWidth (default 75), so the cap alone never widens a column.
            columnMaxWidths[name]?.let { columnModel.getColumn(i).apply { maxWidth = it; preferredWidth = it } }
            if (name in centeredColumns) {
                val col = columnModel.getColumn(i)
                col.headerRenderer = centeredCellRenderer
                col.cellRenderer = FindingCellRenderer(centeredCellRenderer, riskIconRenderer)
            }
        }
        columnFilters.forEach { def ->
            val colIndex = columns.indexOf(def.columnName)
            if (colIndex < 0) return@forEach
            val col = columnModel.getColumn(colIndex)
            val headerDelegate = DefaultTableCellRenderer().apply {
                if (def.columnName in centeredColumns) horizontalAlignment = SwingConstants.CENTER
            }
            col.headerRenderer = ColumnFilterHeaderRenderer(headerDelegate) { def.isActive }
        }
        tableHeader.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val viewCol = tableHeader.columnAtPoint(e.point)
                if (viewCol < 0) return
                val modelCol = convertColumnIndexToModel(viewCol)
                val def = columnFilters.firstOrNull { columns.indexOf(it.columnName) == modelCol } ?: return
                showColumnFilterPopup(def, e)
            }
        })
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1 && e.clickCount == 2) {
                    val finding = findingAt(rowAtPoint(e.point)) ?: return
                    navigator.navigate(finding.getFileLocations(), e)
                }
            }

            override fun mousePressed(e: MouseEvent) = contextMenuHandler.handlePopupTrigger(e)
            override fun mouseReleased(e: MouseEvent) = contextMenuHandler.handlePopupTrigger(e)
        })
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) openSelectedRow()
                if (e.keyCode == KeyEvent.VK_F3) openFirstSelectedFindingInBrowser()
            }
        })
    }
    private val navigator: FindingNavigator by lazy { FindingNavigator(project, table) }
    private val jiraHandler: JiraIntegrationHandler<T> by lazy {
        JiraIntegrationHandler(project, table) { it.toIssueFinding() }
    }
    private val azureDevOpsHandler: AzureDevOpsIntegrationHandler<T> by lazy {
        AzureDevOpsIntegrationHandler(project, table) { it.toIssueFinding() }
    }
    private val createIssueButton: CreateIssueButton<T> by lazy {
        CreateIssueButton(project, jiraHandler, azureDevOpsHandler, table)
    }
    private val fixItHandler: FixItHandler<T> by lazy {
        FixItHandler(project) { it.toFixItContext() }
    }
    private val contextMenuHandler: FindingContextMenuHandler<T> by lazy {
        FindingContextMenuHandler(
            project = project,
            table = table,
            isEditable = { it.isEditable() },
            getId = { it.getId() },
            getDisplayLocation = { it.getDisplayLocation() },
            getEditDescription = { it.getEditDescription() },
            getStatusOptions = { it.getStatusOptions() },
            getCurrentStatus = { it.getCurrentStatus() },
            getCurrentRemark = { it.getCurrentRemark() },
            onReload = ::loadData,
            getFileLocations = { it.getFileLocations() },
            getHref = { it.getHref() },
            navigator = navigator,
            openCreateJiraIssue = { jiraHandler.openCreateJiraIssueDialog() },
            openCreateAzureDevOpsWorkItem = { azureDevOpsHandler.openCreateAzureDevOpsWorkItemDialog() },
            isFixItAvailable = { AiAgentRegistry.agents.any { agent -> agent.isAvailable() } },
            openFixIt = { fixItHandler.openFixIt(it) },
        )
    }

    private val editButton = JButton(SigridBundle["finding.edit.button"]).apply {
        isEnabled = false
        isFocusable = false
        toolTipText = SigridBundle["finding.edit.button.tooltip"]
    }

    private val openInSigridButton = JButton(SigridBundle["finding.open.in.sigrid.button"]).apply {
        isEnabled = false
        isFocusable = false
        toolTipText = SigridBundle["finding.open.in.sigrid.button.tooltip"]
        addActionListener { openFirstSelectedFindingInBrowser() }
    }

    private val fixWithAiButton = JButton(SigridBundle["finding.fixit.button"]).apply {
        isEnabled = false
        isFocusable = false
        toolTipText = SigridBundle["finding.fixit.button.tooltip"]
        addActionListener { fixItHandler.openFixIt(contextMenuHandler.selectedFindings()) }
    }

    private val groupByFileCheckBox = JBCheckBox(SigridBundle["panel.group.by.file"]).apply {
        isFocusable = false
        toolTipText = SigridBundle["panel.group.by.file.tooltip"]
        addActionListener {
            groupByFile = isSelected
            applyFilter()
        }
    }

    private val fileFilterPanel = FileFilterPanel(project) { applyFilter() }

    private val disclosureLabel = JBLabel()
    private val showAllButton = JButton(SigridBundle["panel.show.all"]).apply {
        isFocusable = false
        addActionListener {
            disclosure.toggle()
            applyFilter()
            if (!disclosure.expanded) table.scrollRectToVisible(table.getCellRect(0, 0, true))
        }
    }
    private val disclosureBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 2)).apply {
        add(disclosureLabel)
        add(showAllButton)
        isVisible = false
    }

    private val cardLayout = CardLayout()
    private val cards = JPanel(cardLayout)
    private val statusLabel = JBLabel().apply { horizontalAlignment = JBLabel.CENTER }
    private val filteredEmptyLabel = JBLabel("").apply {
        horizontalAlignment = JBLabel.CENTER
        isVisible = false
    }

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
        subscribeToSettingsChanges()
        loadData()
        focusTraversalPolicy = object : FocusTraversalPolicy() {
            override fun getDefaultComponent(aContainer: Container) = table
            override fun getFirstComponent(aContainer: Container) = table
            override fun getLastComponent(aContainer: Container) = searchField.textEditor
            override fun getComponentAfter(aContainer: Container, aComponent: Component): Component =
                if (aComponent == table) searchField.textEditor else table
            override fun getComponentBefore(aContainer: Container, aComponent: Component): Component =
                if (aComponent == searchField.textEditor) table else searchField.textEditor
        }
        isFocusCycleRoot = true
    }

    private fun subscribeToSettingsChanges() {
        val listener = SigridSettingsListener {
            loadData()
            createIssueButton.updateButtonState()
        }
        project.messageBus.connect().subscribe(SigridSettingsTopic.PROJECT, listener)
        ApplicationManager.getApplication().messageBus.connect().subscribe(SigridSettingsTopic.GLOBAL, listener)
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            AiAgentAvailabilityTopic.TOPIC,
            AiAgentAvailabilityListener { ApplicationManager.getApplication().invokeLater { updateFixWithAiButtonState() } },
        )
    }

    private fun updateFixWithAiButtonState() {
        fixWithAiButton.isEnabled = table.selectedRows.isNotEmpty() &&
            AiAgentRegistry.agents.any { it.isAvailable() }
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
                val selected = table.selectedFindings()
                editButton.isEnabled = selected.any { it.isEditable() }
                openInSigridButton.isEnabled = selected.any { it.getHref().orEmpty().isNotEmpty() }
                updateFixWithAiButtonState()
                createIssueButton.updateButtonState()
            }
        }
    }

    private fun openFirstSelectedFindingInBrowser() {
        val href = table.selectedFindings().firstOrNull()?.getHref()?.takeIf { it.isNotEmpty() } ?: return
        BrowserUtil.browse(href)
    }

    private fun openSelectedRow() {
        val viewRow = table.selectedRow
        if (viewRow < 0) return
        val finding = table.findingAt(viewRow)
        if (finding == null) table.toggleExpansion(viewRow) else navigator.navigate(finding.getFileLocations(), null)
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
        val toolbar = JPanel(GridBagLayout()).apply {
            val leading = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.VERTICAL
                weightx = 1.0
            }
            val trailing = GridBagConstraints().apply {
                anchor = GridBagConstraints.EAST
                fill = GridBagConstraints.VERTICAL
            }
            val controlHeight = fileFilterPanel.filterControlHeight
            listOf(editButton, openInSigridButton, createIssueButton.button, fixWithAiButton).forEach { button ->
                button.preferredSize = java.awt.Dimension(button.preferredSize.width, controlHeight)
            }
            searchField.preferredSize = java.awt.Dimension(searchField.preferredSize.width, controlHeight)
            add(fileFilterPanel, leading)
            if (fileGroupingSupported) {
                add(groupByFileCheckBox, (trailing.clone() as GridBagConstraints).apply { insets = JBUI.insetsRight(GROUP_CHECKBOX_GAP) })
            }
            add(editButton, trailing)
            add(openInSigridButton, trailing)
            add(createIssueButton.button, trailing)
            add(fixWithAiButton, trailing)
            add(searchField, trailing)
        }
        cards.add(JBLabel(SigridBundle["panel.loading"]).apply { horizontalAlignment = JBLabel.CENTER }, CARD_LOADING)
        cards.add(statusLabel, CARD_ERROR)
        cards.add(buildNotConfiguredCard(project), CARD_NOT_CONFIGURED)
        val tableCard = JPanel(BorderLayout()).apply {
            add(JBScrollPane(table), BorderLayout.CENTER)
            add(JPanel(BorderLayout()).apply {
                add(filteredEmptyLabel, BorderLayout.NORTH)
                add(disclosureBar, BorderLayout.SOUTH)
            }, BorderLayout.SOUTH)
        }
        cards.add(tableCard, CARD_TABLE)
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

    /** Re-renders the findings already loaded, e.g. after pinning changed. Call on the EDT. */
    protected fun refreshView() = applyFilter()

    fun setActiveFileOnly(value: Boolean) = fileFilterPanel.setActiveFileOnly(value)

    private fun showColumnFilterPopup(def: ColumnFilterDef<T>, e: MouseEvent) {
        val popup = JPopupMenu()

        val clearItem = JMenuItem(SigridBundle["panel.filter.all"]).also { item ->
            item.isEnabled = def.isActive
            item.addActionListener {
                def.selectedIds = emptySet()
                table.tableHeader.repaint()
                applyFilter()
            }
        }
        popup.add(clearItem)
        popup.addSeparator()

        def.options.forEach { opt ->
            val item = object : JCheckBoxMenuItem(opt.label, opt.id in def.selectedIds) {
                override fun processMouseEvent(e: MouseEvent) {
                    if (e.id == MouseEvent.MOUSE_RELEASED && contains(e.point)) {
                        isSelected = !isSelected
                        def.selectedIds = if (isSelected) def.selectedIds + opt.id else def.selectedIds - opt.id
                        table.tableHeader.repaint()
                        applyFilter()
                        return
                    }
                    super.processMouseEvent(e)
                }
            }
            popup.add(item)
        }
        popup.show(e.component, e.x, e.y)
    }

    // Search, column filters and the active-file filter all narrow the list; a narrowed view is never capped.
    private fun isNarrowed(): Boolean =
        searchField.text.isNotBlank() || fileFilterPanel.activeFileOnly || columnFilters.any { it.isActive }

    private fun filterFindings(): List<T> {
        val query = searchField.text.trim()
        val afterColumnFilters = columnFilters.fold(filterByActiveFile()) { acc, def ->
            if (def.selectedIds.isEmpty()) acc else acc.filter { def.getOptionId(it) in def.selectedIds }
        }
        return if (query.isEmpty()) afterColumnFilters else afterColumnFilters.filter { it.matchesSearch(query) }
    }

    private fun filterByActiveFile(): List<T> {
        val activePath = fileFilterPanel.activeFilePath()
        if (!fileFilterPanel.activeFileOnly || activePath == null) return allFindings
        return allFindings.filter { finding ->
            finding.getFileLocations().any { loc -> FileFilterPanel.matchesActivePath(loc.filePath, activePath) }
        }
    }

    private fun applyFilter() {
        val (pinnedFindings, matching) = filterFindings().partition { it.isPinned() }
        val filtered = disclosure.visible(matching, isNarrowed())
        updateDisclosureBar(matching.size)
        val pinned = pinnedFindings.takeIf { it.isNotEmpty() }?.let { PinnedFindings(pinnedTitle, it) }
        val selectedIds = table.selectedFindings().map { it.getId() }.filter { it.isNotEmpty() }.toSet()

        table.setFindings(filtered, { it.toRow() }, if (groupByFile) { finding -> finding.getGroupKey() } else null, groupOrder(), pinned)
        if (filtered.isEmpty() && pinned == null) showNoFindings() else showFindings(selectedIds)
    }

    private fun updateDisclosureBar(total: Int) {
        val limit = disclosure.limit ?: return
        disclosureBar.isVisible = disclosure.canToggle(total, isNarrowed())
        disclosureLabel.text =
            if (disclosure.expanded) SigridBundle["panel.showing.all", total] else SigridBundle["panel.showing.top", limit, total]
        showAllButton.text =
            if (disclosure.expanded) SigridBundle["panel.show.top", limit] else SigridBundle["panel.show.all"]
    }

    private fun showNoFindings() {
        if (allFindings.isEmpty()) {
            filteredEmptyLabel.isVisible = false
            showSuccess(emptyMessage)
            return
        }
        val message = noMatchMessage()
        ApplicationManager.getApplication().invokeLater {
            filteredEmptyLabel.text = message
            filteredEmptyLabel.isVisible = true
            filteredEmptyLabel.foreground = JBColor.RED
        }
        showCard(CARD_TABLE)
    }

    private fun noMatchMessage(): String {
        val query = searchField.text.trim()
        return if (query.isEmpty()) SigridBundle["panel.no.findings.match.filter"] else SigridBundle["panel.no.findings.match", query]
    }

    private fun showFindings(selectedIds: Set<String>) {
        ApplicationManager.getApplication().invokeLater {
            filteredEmptyLabel.isVisible = false
        }
        showCard(CARD_TABLE)
        selectRowsById(selectedIds)
    }

    fun loadData() {
        showCard(CARD_LOADING)
        disclosure.reset()
        val generation = loadGeneration.incrementAndGet()

        ApplicationManager.getApplication().executeOnPooledThread {
            val projectConfig = SigridProjectConfiguration.getInstance(project)
            if (projectConfig.isUrlOverrideWithoutKeyOverride) {
                setFilterControlsEnabled(false)
                showError(SigridBundle["panel.error.url.override.no.key"])
                return@executeOnPooledThread
            }
            if (!projectConfig.isConfigurationValid) {
                setFilterControlsEnabled(false)
                showNotConfigured()
                return@executeOnPooledThread
            }

            setFilterControlsEnabled(true)

            try {
                val findings = fetch(projectConfig.subsystem)
                ApplicationManager.getApplication().invokeLater { applyLoaded(generation, findings) }
            } catch (e: Exception) {
                if (generation == loadGeneration.get()) showError(toErrorMessage(e))
            }
        }
    }

    /** Applies a finished load on the EDT unless a newer load has started; a failure shows as an error, not a stale card. */
    private fun applyLoaded(generation: Int, findings: List<T>) {
        if (generation != loadGeneration.get()) return
        try {
            allFindings = findings
            applyFilter()
        } catch (e: Exception) {
            thisLogger().warn("Could not display loaded findings", e)
            showError(toErrorMessage(e))
        }
    }

    private fun setFilterControlsEnabled(enabled: Boolean) {
        ApplicationManager.getApplication().invokeLater {
            fileFilterPanel.isEnabled = enabled
            searchField.isEnabled = enabled
            searchField.textEditor.isEnabled = enabled
        }
    }

    private fun selectRowsById(selectedIds: Set<String>) {
        if (selectedIds.isEmpty()) return
        table.clearSelection()
        val rows = (0 until table.rowCount).filter { table.findingAt(it)?.getId().orEmpty() in selectedIds }
        rows.forEach { table.selectionModel.addSelectionInterval(it, it) }
        rows.firstOrNull()?.let { table.scrollRectToVisible(table.getCellRect(it, 0, true)) }
    }

    private fun showNotConfigured() {
        ApplicationManager.getApplication().invokeLater {
            showCard(CARD_NOT_CONFIGURED)
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

    private fun toErrorMessage(e: Exception): String =
        httpErrorMessage(e) { SigridBundle["panel.error.generic", it] }

}
