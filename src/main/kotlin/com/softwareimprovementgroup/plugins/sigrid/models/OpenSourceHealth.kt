package com.softwareimprovementgroup.plugins.sigrid.models

data class OpenSourceHealthResponse(
    val bomFormat: String,
    val specVersion: String,
    val version: Int,
    val metadata: OshMetadataResponse,
    val components: List<OshDependencyResponse>,
    val vulnerabilities: List<Any>,
)

data class OshMetadataResponse(
    val timestamp: String,
    val properties: List<Property>,
)

data class OshDependencyResponse(
    val type: String,
    val name: String,
    val group: String,
    val version: String,
    val purl: String,
    val properties: List<Property>,
    val licenses: List<OshLicenseResponse>,
    val evidence: OshEvidenceResponse?,
)

data class OshLicenseResponse(
    val license: OshLicenseName,
)

data class OshLicenseName(
    val name: String,
)

data class OshEvidenceResponse(
    val occurrences: List<OshOccurrence>?,
)

data class OshOccurrence(
    val location: String?,
)

data class Property(
    val name: String,
    val value: String,
)