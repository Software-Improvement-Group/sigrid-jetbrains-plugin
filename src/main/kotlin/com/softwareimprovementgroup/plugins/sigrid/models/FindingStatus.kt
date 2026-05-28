package com.softwareimprovementgroup.plugins.sigrid.models

enum class FindingStatus(val apiValue: String) {
    Raw("RAW"),
    Refined("REFINED"),
    WillFix("WILL_FIX"),
    Fixed("FIXED"),
    Accepted("ACCEPTED"),
    FalsePositive("FALSE_POSITIVE");

    companion object {
        fun from(value: String): FindingStatus =
            entries.firstOrNull { it.apiValue == value } ?: Raw
    }
}

enum class MaintainabilityFindingStatus(val apiValue: String) {
    Raw("RAW"),
    WillFix("WILL_FIX"),
    Accepted("ACCEPTED");

    companion object {
        fun from(value: String): MaintainabilityFindingStatus =
            entries.firstOrNull { it.apiValue == value } ?: Raw
    }
}

fun snakeCaseToTitleCase(value: String): String =
    value.split("_").joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }