package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import com.softwareimprovementgroup.plugins.sigrid.services.onboarding.OnboardingState
import com.softwareimprovementgroup.plugins.sigrid.services.onboarding.OnboardingStatus
import com.softwareimprovementgroup.plugins.sigrid.services.onboarding.SystemOnboardingService
import java.awt.FlowLayout
import java.awt.GridBagLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

/** Shown instead of the findings tabs while the project's system does not exist in Sigrid yet. */
internal class OnboardingPanel(private val project: Project) : JBPanel<OnboardingPanel>(GridBagLayout()) {

    private val service = SystemOnboardingService.getInstance(project)
    private val message = JBLabel()
    private val buttons = JPanel(FlowLayout(FlowLayout.CENTER))

    init {
        val column = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            message.alignmentX = CENTER_ALIGNMENT
            buttons.alignmentX = CENTER_ALIGNMENT
            add(message)
            add(buttons)
        }
        add(column)
    }

    fun render(state: OnboardingState) {
        val system = StringUtil.escapeXmlEntities(systemLabel())
        message.text = "<html><div style='text-align:center'>${messageFor(state, system)}</div></html>"
        message.foreground = if (state.status == OnboardingStatus.ERROR) JBColor.RED else JBColor.foreground()
        buttons.removeAll()
        buttonsFor(state.status, systemLabel()).forEach(buttons::add)
        revalidate()
        repaint()
    }

    private fun systemLabel(): String {
        val config = SigridProjectConfiguration.getInstance(project)
        return "${config.effectiveCustomer}/${config.system}"
    }

    private fun messageFor(state: OnboardingState, system: String): String = when (state.status) {
        OnboardingStatus.CHECKING, OnboardingStatus.ONBOARDED -> SigridBundle["onboarding.checking", system]
        OnboardingStatus.NOT_ONBOARDED, OnboardingStatus.ONBOARDING -> SigridBundle["onboarding.not.onboarded", system]
        OnboardingStatus.ONBOARDING_STARTED -> SigridBundle["onboarding.started", system]
        OnboardingStatus.ERROR -> StringUtil.escapeXmlEntities(state.error.orEmpty())
    }

    private fun buttonsFor(status: OnboardingStatus, system: String): List<JButton> = when (status) {
        OnboardingStatus.NOT_ONBOARDED -> listOf(button(SigridBundle["onboarding.button", system]) { service.onboard() }, refreshButton())
        OnboardingStatus.ONBOARDING -> listOf(button(SigridBundle["onboarding.in.progress"]) {}.apply { isEnabled = false })
        OnboardingStatus.ONBOARDING_STARTED -> listOf(refreshButton())
        OnboardingStatus.ERROR -> listOf(button(SigridBundle["onboarding.retry"]) { service.check() })
        OnboardingStatus.CHECKING, OnboardingStatus.ONBOARDED -> emptyList()
    }

    private fun refreshButton() = button(SigridBundle["onboarding.refresh"]) { service.check() }

    private fun button(text: String, onClick: () -> Unit) = JButton(text).apply { addActionListener { onClick() } }
}
