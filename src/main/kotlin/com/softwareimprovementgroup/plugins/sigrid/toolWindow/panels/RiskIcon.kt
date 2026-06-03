package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilitySeverity
import com.softwareimprovementgroup.plugins.sigrid.models.RiskSeverity
import java.awt.Color

data class RiskIcon(val color: Color, val label: String)

fun MaintainabilitySeverity.toRiskIcon(): RiskIcon = when (this) {
    MaintainabilitySeverity.Unknown  -> RiskIcon(Color.decode("#d3d3d3"), SigridBundle["risk.maintainability.unknown"])
    MaintainabilitySeverity.Low      -> RiskIcon(Color.decode("#ffd700"), SigridBundle["risk.maintainability.low"])
    MaintainabilitySeverity.Medium   -> RiskIcon(Color.decode("#ff9900"), SigridBundle["risk.maintainability.medium"])
    MaintainabilitySeverity.Moderate -> RiskIcon(Color.decode("#ff9900"), SigridBundle["risk.maintainability.moderate"])
    MaintainabilitySeverity.High     -> RiskIcon(Color.decode("#ff0000"), SigridBundle["risk.maintainability.high"])
    MaintainabilitySeverity.VeryHigh -> RiskIcon(Color.decode("#8b0000"), SigridBundle["risk.maintainability.very.high"])
}

fun RiskSeverity.toRiskIcon(): RiskIcon = when (this) {
    RiskSeverity.None        -> RiskIcon(Color.decode("#5cb85c"), SigridBundle["risk.none"])
    RiskSeverity.Unknown     -> RiskIcon(Color.decode("#d3d3d3"), SigridBundle["risk.unknown"])
    RiskSeverity.Information -> RiskIcon(Color.decode("#007cc3"), SigridBundle["risk.information"])
    RiskSeverity.Low         -> RiskIcon(Color.decode("#ffd700"), SigridBundle["risk.low"])
    RiskSeverity.Medium      -> RiskIcon(Color.decode("#ff9900"), SigridBundle["risk.medium"])
    RiskSeverity.High        -> RiskIcon(Color.decode("#ff0000"), SigridBundle["risk.high"])
    RiskSeverity.Critical    -> RiskIcon(Color.decode("#8b0000"), SigridBundle["risk.critical"])
}