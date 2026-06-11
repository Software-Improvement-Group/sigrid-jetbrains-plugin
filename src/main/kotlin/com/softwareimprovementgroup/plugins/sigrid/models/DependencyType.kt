package com.softwareimprovementgroup.plugins.sigrid.models

enum class DependencyType(val label: String) {
    DIRECT("Direct"),
    TRANSITIVE("Transitive"),
    UNKNOWN("Unknown");

    companion object {
        fun from(value: String?): DependencyType = when {
            value == null -> UNKNOWN
            value.contains("direct", ignoreCase = true) -> DIRECT
            value.contains("transitive", ignoreCase = true) -> TRANSITIVE
            else -> UNKNOWN
        }
    }
}
