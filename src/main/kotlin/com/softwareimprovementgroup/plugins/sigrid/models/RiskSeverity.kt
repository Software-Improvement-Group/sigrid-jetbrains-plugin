package com.softwareimprovementgroup.plugins.sigrid.models

enum class RiskSeverity {
    None, Unknown, Information, Low, Medium, High, Critical;

    companion object {
        fun from(value: String?): RiskSeverity = when (value?.lowercase()) {
            "none"                 -> None
            "information", "info" -> Information
            "low"                 -> Low
            "medium"              -> Medium
            "high"                -> High
            "critical"            -> Critical
            else                  -> Unknown
        }
    }
}