package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.softwareimprovementgroup.plugins.sigrid.models.MaintainabilitySeverity
import com.softwareimprovementgroup.plugins.sigrid.models.RiskSeverity
import java.awt.Color

data class RiskIcon(val color: Color, val label: String)

fun MaintainabilitySeverity.toRiskIcon(): RiskIcon = when (this) {
    MaintainabilitySeverity.Unknown  -> RiskIcon(Color.decode("#d3d3d3"), "Unknown")
    MaintainabilitySeverity.Low      -> RiskIcon(Color.decode("#ffd700"), "Low")
    MaintainabilitySeverity.Medium   -> RiskIcon(Color.decode("#ff9900"), "Medium")
    MaintainabilitySeverity.Moderate -> RiskIcon(Color.decode("#ff9900"), "Moderate")
    MaintainabilitySeverity.High     -> RiskIcon(Color.decode("#ff0000"), "High")
    MaintainabilitySeverity.VeryHigh -> RiskIcon(Color.decode("#8b0000"), "Very High")
}

fun RiskSeverity.toRiskIcon(): RiskIcon = when (this) {
    RiskSeverity.None        -> RiskIcon(Color.decode("#5cb85c"), "None")
    RiskSeverity.Unknown     -> RiskIcon(Color.decode("#d3d3d3"), "Unknown")
    RiskSeverity.Information -> RiskIcon(Color.decode("#007cc3"), "Information")
    RiskSeverity.Low         -> RiskIcon(Color.decode("#ffd700"), "Low")
    RiskSeverity.Medium      -> RiskIcon(Color.decode("#ff9900"), "Medium")
    RiskSeverity.High        -> RiskIcon(Color.decode("#ff0000"), "High")
    RiskSeverity.Critical    -> RiskIcon(Color.decode("#8b0000"), "Critical")
}