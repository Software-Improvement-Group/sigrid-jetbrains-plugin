package com.softwareimprovementgroup.plugins.sigrid.models

data class ArchitectureFinding(
    val id: String,
    val severity: RiskSeverity,
    val description: String,
    val sourceComponent: String,
    val targetComponent: String,
    val filePath: String,
    val displayFilePath: String,
    val line: Int,
    val fileLocations: List<FileLocation>,
)
