package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.util.ui.ColumnInfo
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import java.awt.Component
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class FindingTreeNode<T>(val finding: T, val row: Array<Any>) : DefaultMutableTreeNode(finding)

class FindingGroupNode(val key: String) : DefaultMutableTreeNode(key) {
    val label: String get() = SigridBundle["panel.group.label", key, childCount]
}

private class RowColumnInfo(name: String, private val index: Int) : ColumnInfo<DefaultMutableTreeNode, Any>(name) {
    override fun valueOf(node: DefaultMutableTreeNode): Any = (node as? FindingTreeNode<*>)?.row?.get(index) ?: ""
    override fun getColumnClass(): Class<*> = Any::class.java
}

private class FindingTreeCellRenderer(private val textColumn: Int) : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean,
    ) {
        when (value) {
            is FindingGroupNode -> append(value.label, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            is FindingTreeNode<*> -> append(value.row[textColumn].toString())
        }
    }
}

/**
 * A JTable can't span cells, so a group row's label (a file path, usually too long for the tree column) is
 * repainted over the columns to its right. The tree renderer draws it, so font, indent and selection colors
 * match the unspanned label it covers.
 */
private class GroupRowSpanPainter(
    private val table: TreeTable,
    private val treeColumn: Int,
    private val isGroupRow: (Int) -> Boolean,
) {
    private val treeViewColumn: Int get() = table.convertColumnIndexToView(treeColumn)

    fun paint(g: Graphics) {
        if (treeViewColumn < 0 || treeViewColumn >= table.columnCount - 1) return
        visibleRows(g.clipBounds ?: table.visibleRect).filter(isGroupRow).forEach { paintRow(g, it) }
    }

    private fun visibleRows(clip: Rectangle): IntRange {
        val first = table.rowAtPoint(Point(0, clip.y)).coerceAtLeast(0)
        val last = table.rowAtPoint(Point(0, clip.y + clip.height - 1)).let { if (it < 0) table.rowCount - 1 else it }
        return first..last
    }

    private fun paintRow(g: Graphics, row: Int) {
        val cell = table.getCellRect(row, treeViewColumn, false)
        // TreeTableTree reports bounds in table coordinates (it already adds the preceding columns' widths).
        val labelX = table.tree.getRowBounds(row)?.x ?: return
        val span = Rectangle(labelX, cell.y, table.width - labelX, cell.height)
        val spanGraphics = g.create(span.x, span.y, span.width, span.height)
        try {
            spanGraphics.color = backgroundOf(row, treeViewColumn + 1)
            spanGraphics.fillRect(0, 0, span.width, span.height)
            labelComponent(row).apply { setSize(span.width, span.height) }.paint(spanGraphics)
        } finally {
            spanGraphics.dispose()
        }
    }

    private fun backgroundOf(row: Int, viewColumn: Int) =
        table.prepareRenderer(table.getCellRenderer(row, viewColumn), row, viewColumn).background

    private fun labelComponent(row: Int): Component {
        val tree = table.tree
        return tree.cellRenderer.getTreeCellRendererComponent(
            tree, tree.getPathForRow(row).lastPathComponent, table.isRowSelected(row), tree.isExpanded(row), false, row, table.hasFocus(),
        )
    }
}

private fun columnInfos(columns: Array<String>, treeColumn: Int): Array<ColumnInfo<*, *>> =
    Array(columns.size) { i -> if (i == treeColumn) TreeColumnInfo(columns[i]) else RowColumnInfo(columns[i], i) }

/**
 * Table of findings whose [treeColumn] doubles as the tree column. Flat mode lists findings directly under a
 * hidden root; grouped mode nests them under expandable group nodes. Callers deal in findings and view rows,
 * never in nodes, so the flat and grouped shapes look the same to them.
 */
class FindingTreeTable<T>(
    columns: Array<String>,
    treeColumn: Int,
    private val root: DefaultMutableTreeNode = DefaultMutableTreeNode(),
) : TreeTable(ListTreeTableModelOnColumns(root, columnInfos(columns, treeColumn))) {

    private val collapsedGroupKeys = mutableSetOf<String>()
    private val spanPainter = GroupRowSpanPainter(this, treeColumn) { nodeAt(it) is FindingGroupNode }

    init {
        setRootVisible(false)
        setTreeCellRenderer(FindingTreeCellRenderer(treeColumn))
        tree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) = trackGroup(event) { collapsedGroupKeys.remove(it) }
            override fun treeCollapsed(event: TreeExpansionEvent) = trackGroup(event) { collapsedGroupKeys.add(it) }
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        spanPainter.paint(g)
    }

    fun setFindings(findings: List<T>, toRow: (T) -> Array<Any>, groupKeyOf: ((T) -> String)?) {
        root.removeAllChildren()
        if (groupKeyOf == null) {
            findings.forEach { root.add(FindingTreeNode(it, toRow(it))) }
        } else {
            findings.groupBy(groupKeyOf).forEach { (key, members) -> root.add(groupNode(key, members, toRow)) }
        }
        (tableModel as DefaultTreeModel).reload()
        tree.showsRootHandles = groupKeyOf != null
        expandUncollapsedGroups()
    }

    /** The finding on [viewRow]; null for group rows and rows out of range. */
    fun findingAt(viewRow: Int): T? = (nodeAt(viewRow) as? FindingTreeNode<*>)?.let { castFinding(it) }

    /** The finding on [viewRow], or all findings of a group row. */
    fun findingsAt(viewRow: Int): List<T> = nodeAt(viewRow)?.findings().orEmpty()

    fun selectedFindings(): List<T> = selectedRows.flatMap { findingsAt(it) }.distinct()

    fun toggleExpansion(viewRow: Int) {
        if (tree.isExpanded(viewRow)) tree.collapseRow(viewRow) else tree.expandRow(viewRow)
    }

    private fun groupNode(key: String, members: List<T>, toRow: (T) -> Array<Any>) =
        FindingGroupNode(key).also { group -> members.forEach { group.add(FindingTreeNode(it, toRow(it))) } }

    private fun expandUncollapsedGroups() {
        for (i in 0 until root.childCount) {
            val group = root.getChildAt(i) as? FindingGroupNode ?: continue
            if (group.key !in collapsedGroupKeys) tree.expandPath(TreePath(arrayOf(root, group)))
        }
    }

    private fun trackGroup(event: TreeExpansionEvent, update: (String) -> Unit) {
        (event.path.lastPathComponent as? FindingGroupNode)?.let { update(it.key) }
    }

    private fun nodeAt(viewRow: Int): DefaultMutableTreeNode? = tree.getPathForRow(viewRow)?.lastPathComponent as? DefaultMutableTreeNode

    @Suppress("UNCHECKED_CAST")
    private fun castFinding(node: FindingTreeNode<*>): T = node.finding as T

    private fun DefaultMutableTreeNode.findings(): List<T> = when (this) {
        is FindingTreeNode<*> -> listOf(castFinding(this))
        else -> (0 until childCount).flatMap { (getChildAt(it) as DefaultMutableTreeNode).findings() }
    }
}
