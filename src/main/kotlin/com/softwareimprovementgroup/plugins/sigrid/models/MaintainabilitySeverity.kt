package com.softwareimprovementgroup.plugins.sigrid.models

enum class MaintainabilitySeverity {
    Unknown, Low, Medium, Moderate, High, VeryHigh;

    companion object {
        fun from(value: String): MaintainabilitySeverity = when (value.lowercase()) {
            "very_high" -> VeryHigh
            "high"      -> High
            "moderate"  -> Moderate
            "medium"    -> Medium
            "low"       -> Low
            else        -> Unknown
        }
    }
}