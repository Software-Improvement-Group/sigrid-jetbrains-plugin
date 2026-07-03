package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilitySeverity
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCandidate
import java.awt.Color
import java.awt.event.MouseEvent
import javax.swing.Timer
import javax.swing.event.HyperlinkEvent

class RefactoringCandidateHighlighter(private val project: Project) {

    private var findings: List<RefactoringCandidate> = emptyList()

    init {
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    source.getAllEditors(file).filterIsInstance<TextEditor>().forEach {
                        applyHighlightsToEditor(it.editor, file)
                    }
                }

                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val file = event.newFile ?: return
                    val textEditor = event.newEditor as? TextEditor ?: return
                    applyHighlightsToEditor(textEditor.editor, file)
                }
            }
        )
    }

    fun updateFindings(findings: List<RefactoringCandidate>) {
        this.findings = findings
        val fem = FileEditorManager.getInstance(project)
        fem.openFiles.forEach { vFile ->
            fem.getAllEditors(vFile).filterIsInstance<TextEditor>().forEach {
                applyHighlightsToEditor(it.editor, vFile)
            }
        }
    }

    private fun applyHighlightsToEditor(editor: Editor, vFile: VirtualFile) {
        clearSigridHighlights(editor)
        val basePath = project.basePath ?: return
        val relativePath = vFile.path.removePrefix("$basePath/")
        findings.forEach { candidate ->
            candidate.fileLocations
                .filter { loc ->
                    loc.startLine != null && loc.endLine != null &&
                    FileFilterPanel.matchesActivePath(loc.filePath, relativePath)
                }
                .forEach { loc -> addHighlight(editor, candidate, loc) }
        }
        ensureMouseListener(editor)
    }

    private fun ensureMouseListener(editor: Editor) {
        if (editor.getUserData(SIGRID_LISTENER_KEY) == true) return
        editor.putUserData(SIGRID_LISTENER_KEY, true)
        var lastRange: IntRange? = null
        var currentBalloon: Balloon? = null
        var hideTimer: Timer? = null
        editor.addEditorMouseMotionListener(object : EditorMouseMotionListener {
            override fun mouseMoved(e: EditorMouseEvent) {
                val offset = editor.logicalPositionToOffset(e.logicalPosition)
                val highlighter = editor.markupModel.allHighlighters
                    .filter { it.getUserData(SIGRID_HIGHLIGHT_KEY) == true }
                    .firstOrNull { it.startOffset <= offset && offset <= it.endOffset }
                val range = highlighter?.let { it.startOffset..it.endOffset }
                if (range == lastRange) return
                lastRange = range
                if (range != null) {
                    hideTimer?.stop()
                    hideTimer = null
                    currentBalloon?.hide()
                    val tooltip = highlighter.getUserData(SIGRID_TOOLTIP_KEY) ?: return
                    currentBalloon = showTooltip(tooltip, e.mouseEvent)
                } else {
                    // Grace period: keep balloon visible long enough to move the mouse to it.
                    hideTimer?.stop()
                    hideTimer = Timer(500) {
                        currentBalloon?.hide()
                        currentBalloon = null
                    }.apply { isRepeats = false; start() }
                }
            }
        })
    }

    private fun showTooltip(tooltip: String, mouseEvent: MouseEvent): Balloon {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(tooltip, MessageType.INFO) { event ->
                if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    BrowserUtil.browse(event.url.toExternalForm())
                }
            }
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon()
        balloon.show(RelativePoint(mouseEvent), Balloon.Position.below)
        return balloon
    }

    private fun clearSigridHighlights(editor: Editor) {
        editor.markupModel.allHighlighters
            .filter { it.getUserData(SIGRID_HIGHLIGHT_KEY) == true }
            .forEach { editor.markupModel.removeHighlighter(it) }
    }

    private fun addHighlight(editor: Editor, candidate: RefactoringCandidate, loc: FileLocation) {
        val doc = editor.document
        val start = loc.startLine!! - 1
        val end = loc.endLine!! - 1
        if (start < 0 || end >= doc.lineCount || start > end) return
        val startOffset = doc.getLineStartOffset(start)
        val endOffset = doc.getLineEndOffset(end)
        val attrs = TextAttributes().apply { backgroundColor = severityBackground(candidate.severity) }
        val h = editor.markupModel.addRangeHighlighter(
            startOffset, endOffset,
            HighlighterLayer.WARNING,
            attrs,
            HighlighterTargetArea.EXACT_RANGE,
        )
        val tooltip = buildTooltip(candidate)
        h.errorStripeTooltip = tooltip
        h.putUserData(SIGRID_HIGHLIGHT_KEY, true)
        h.putUserData(SIGRID_TOOLTIP_KEY, tooltip)
    }

    private fun buildTooltip(candidate: RefactoringCandidate): String {
        val sb = StringBuilder("<html>${candidate.description}")
        if (!candidate.href.isNullOrEmpty()) {
            sb.append("<br><a href=\"${candidate.href}\">${SigridBundle["highlight.view.in.sigrid"]}</a>")
        }
        sb.append("</html>")
        return sb.toString()
    }

    companion object {
        private val SIGRID_HIGHLIGHT_KEY = Key.create<Boolean>("sigrid.refactoring.highlight")
        private val SIGRID_TOOLTIP_KEY = Key.create<String>("sigrid.refactoring.tooltip")
        private val SIGRID_LISTENER_KEY = Key.create<Boolean>("sigrid.refactoring.listener")

        fun severityBackground(severity: MaintainabilitySeverity): Color = when (severity) {
            MaintainabilitySeverity.High,
            MaintainabilitySeverity.VeryHigh  -> JBColor(Color(0xFF, 0xCC, 0xCC), Color(0x5A, 0x00, 0x00))
            MaintainabilitySeverity.Medium,
            MaintainabilitySeverity.Moderate  -> JBColor(Color(0xFF, 0xF0, 0xCC), Color(0x4A, 0x30, 0x00))
            MaintainabilitySeverity.Low       -> JBColor(Color(0xFF, 0xFA, 0xCC), Color(0x3A, 0x30, 0x00))
            MaintainabilitySeverity.Unknown   -> JBColor(Color(0xF5F5F5), Color(0x3A3A3A))
        }
    }
}
