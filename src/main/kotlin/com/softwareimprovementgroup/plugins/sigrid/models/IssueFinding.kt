package com.softwareimprovementgroup.plugins.sigrid.models

data class IssueFinding(
    val title: String,
    val severityEmoji: String,
    val fileLocations: List<FileLocation>,
)

fun MaintainabilitySeverity.toSeverityEmoji(): String = when (this) {
    MaintainabilitySeverity.VeryHigh -> "🔴"
    MaintainabilitySeverity.High     -> "🔴"
    MaintainabilitySeverity.Moderate -> "🟠"
    MaintainabilitySeverity.Medium   -> "🟠"
    MaintainabilitySeverity.Low      -> "🟡"
    MaintainabilitySeverity.Unknown  -> "⚪"
}

fun RiskSeverity.toSeverityEmoji(): String = when (this) {
    RiskSeverity.Critical    -> "🔴"
    RiskSeverity.High        -> "🔴"
    RiskSeverity.Medium      -> "🟠"
    RiskSeverity.Low         -> "🟡"
    RiskSeverity.Information -> "🔵"
    RiskSeverity.None        -> "🟢"
    RiskSeverity.Unknown     -> "⚪"
}
