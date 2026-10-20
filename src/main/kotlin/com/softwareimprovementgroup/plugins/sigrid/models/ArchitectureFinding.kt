package com.softwareimprovementgroup.plugins.sigrid.models

data class ArchitectureElementResponse(
    val id: String?,
    val type: String?,
    val name: String?,
    val shortName: String?,
)

data class ArchitectureDependencyResponse(
    val id: String?,
    val type: String?,
    val sourceElementId: String?,
    val targetElementId: String?,
    val qualification: String?,
    val qualifications: List<String>?,
    val lines: List<List<Int>>?,
)

/** The part of the raw architecture graph (`architecture-quality/.../raw`) that findings are derived from. */
data class ArchitectureGraphResponse(
    val systemElements: List<ArchitectureElementResponse>?,
    val dependencies: List<ArchitectureDependencyResponse>?,
)

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
