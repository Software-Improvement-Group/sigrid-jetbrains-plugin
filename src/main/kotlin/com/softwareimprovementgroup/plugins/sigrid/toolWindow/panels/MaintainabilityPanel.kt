package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import javax.swing.table.DefaultTableModel

class MaintainabilityPanel : JBPanel<MaintainabilityPanel>(BorderLayout()) {

    init {
        val columns = arrayOf("Risk", "Location", "Description", "Status", "Actions")
        val rows = arrayOf(
            arrayOf("High", "src/main/Foo.kt:42", "Method is too long (120 lines)", "Open", "View"),
            arrayOf("Medium", "src/main/Bar.kt:17", "Duplicated code block (35 lines)", "Open", "View"),
            arrayOf("Low", "src/main/Baz.kt:88", "Complex conditional expression", "Acknowledged", "View"),
            arrayOf("High", "src/main/Auth.kt:5", "Unit interfaces not used", "Open", "View"),
            arrayOf("Medium", "src/main/Config.kt:101", "Magic number in expression", "Fixed", "View"),
        )

        val model = DefaultTableModel(rows, columns)
        val table = JBTable(model).apply {
            isStriped = true
            setDefaultEditor(Any::class.java, null)
        }

        add(JBScrollPane(table), BorderLayout.CENTER)
    }
}