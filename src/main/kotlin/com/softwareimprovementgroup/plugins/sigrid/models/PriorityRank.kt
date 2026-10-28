package com.softwareimprovementgroup.plugins.sigrid.models

enum class PriorityRank {
    Unknown, Low, Medium, High, Critical
}

// PROVISIONAL mapping: no validated cross-capability severity translation exists yet
// (Maintainability's Unknown/Low/Medium/Moderate/High/VeryHigh vs. Security/OSH's
// None/Unknown/Information/Low/Medium/High/Critical are structurally different scales).
// Revisit once a real translation is proposed and validated.
fun MaintainabilitySeverity.toPriorityRank(): PriorityRank = when (this) {
    MaintainabilitySeverity.Unknown  -> PriorityRank.Unknown
    MaintainabilitySeverity.Low      -> PriorityRank.Low
    MaintainabilitySeverity.Medium   -> PriorityRank.Medium
    MaintainabilitySeverity.Moderate -> PriorityRank.Medium
    MaintainabilitySeverity.High     -> PriorityRank.High
    MaintainabilitySeverity.VeryHigh -> PriorityRank.Critical
}

fun RiskSeverity.toPriorityRank(): PriorityRank = when (this) {
    RiskSeverity.None        -> PriorityRank.Unknown
    RiskSeverity.Unknown     -> PriorityRank.Unknown
    RiskSeverity.Information -> PriorityRank.Low
    RiskSeverity.Low         -> PriorityRank.Low
    RiskSeverity.Medium      -> PriorityRank.Medium
    RiskSeverity.High        -> PriorityRank.High
    RiskSeverity.Critical    -> PriorityRank.Critical
}
