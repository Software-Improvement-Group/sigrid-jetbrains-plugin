package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.icons.AllIcons
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.TableCellRenderer

class ColumnFilterHeaderRenderer(
    private val delegate: TableCellRenderer,
    private val isActive: () -> Boolean,
) : TableCellRenderer {
    private val activeIcon by lazy { IconUtil.colorize(AllIcons.General.Filter, JBUI.CurrentTheme.Link.Foreground.ENABLED) }
    private val inactiveIcon by lazy { IconUtil.desaturate(AllIcons.General.Filter) }

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int,
    ): Component {
        val base = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
        base.icon = if (isActive()) activeIcon else inactiveIcon
        base.horizontalTextPosition = SwingConstants.LEFT
        base.iconTextGap = 4
        return base
    }
}
