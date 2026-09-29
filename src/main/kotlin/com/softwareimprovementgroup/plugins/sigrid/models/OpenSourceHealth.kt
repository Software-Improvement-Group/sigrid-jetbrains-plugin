package com.softwareimprovementgroup.plugins.sigrid.models

import com.google.gson.annotations.SerializedName

data class OpenSourceHealthResponse(
    val bomFormat: String,
    val specVersion: String,
    val version: Int,
    val metadata: OshMetadataResponse,
    val components: List<OshDependencyResponse>?,
    val vulnerabilities: List<OshVulnerabilityResponse>?,
)

data class OshMetadataResponse(
    val timestamp: String,
    val properties: List<Property>,
)

data class OshDependencyResponse(
    val type: String,
    val name: String,
    val group: String?,
    val version: String,
    val purl: String?,
    val properties: List<Property>,
    val licenses: List<OshLicenseResponse>,
    val evidence: OshEvidenceResponse?,
    val externalReferences: List<OshExternalReference>?,
    @SerializedName("bom-ref") val bomRef: String? = null,
)

// CycloneDX vulnerability entries live in a separate top-level array (OpenSourceHealthResponse.vulnerabilities)
// and are linked back to a component via `affects[].ref`, which matches that component's `bom-ref` (falling
// back to `purl`, since Sigrid's SBOM export sets bom-ref to the purl value in practice).
data class OshVulnerabilityResponse(
    val id: String,
    val ratings: List<OshVulnerabilityRatingResponse>?,
    val affects: List<OshVulnerabilityAffectsResponse>?,
)

data class OshVulnerabilityRatingResponse(
    val score: Double?,
    val severity: String?,
    val method: String?,
)

data class OshVulnerabilityAffectsResponse(
    val ref: String,
)

data class OshLicenseResponse(
    val license: OshLicenseName,
)

data class OshExternalReference (
    val type: String,
    val url: String?,
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

data class OpenSourceHealthDependency(
    val name: String,
    val displayName: String,
    val version: String,
    val group: String?,
    val dependencyType: DependencyType,
    val purl: String?,
    val risk: RiskSeverity,
    val licenseRisk: RiskSeverity,
    val vulnerabilityRisk: RiskSeverity,
    val freshnessRisk: RiskSeverity,
    val activityRisk: RiskSeverity,
    val stabilityRisk: RiskSeverity,
    val managementRisk: RiskSeverity,
    val fileLocations: List<FileLocation>,
    val href: String?,
    val vulnerabilities: List<OshVulnerability> = emptyList(),
)

// One real CVE affecting this dependency, with its worst reported CVSS rating (a vulnerability can carry
// ratings from multiple scoring methods, e.g. CVSSv2 and CVSSv3 - see mapVulnerability in OpenSourceHealthMapper).
data class OshVulnerability(
    val id: String,
    val severity: RiskSeverity,
    val score: Double?,
    val method: String?,
)