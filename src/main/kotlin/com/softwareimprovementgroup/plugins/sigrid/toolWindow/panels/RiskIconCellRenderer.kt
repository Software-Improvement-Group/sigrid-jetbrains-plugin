package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

class FindingCellRenderer(
    private val centered: DefaultTableCellRenderer,
    private val icon: RiskIconCellRenderer,
) : TableCellRenderer {
    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int,
    ): Component = if (value is RiskIcon) {
        icon.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    } else {
        centered.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    }
}

class RiskIconCellRenderer : DefaultTableCellRenderer() {

    private var riskIcon: RiskIcon? = null

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): Component {
        super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column)
        riskIcon = value as? RiskIcon
        toolTipText = riskIcon?.label
        horizontalAlignment = CENTER
        return this
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val icon = riskIcon ?: return
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val diameter = minOf(width, height) - 8
            val x = (width - diameter) / 2
            val y = (height - diameter) / 2
            g2.color = icon.color
            g2.fillOval(x, y, diameter, diameter)
        } finally {
            g2.dispose()
        }
    }
}