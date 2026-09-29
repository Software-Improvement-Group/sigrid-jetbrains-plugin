package com.softwareimprovementgroup.plugins.sigrid.models

// Mirrors /rest/analysis-results/api/v1/system-metadata/{customer}/{system}. Used by the prioritization feature
// (Epic 432) as the secondary dampener (businessCriticality, lifecyclePhase) and to gate duplication-first sequencing
// by technologyCategory (see section 2.8 of the prioritization design doc) - not as a primary sort key.
data class SystemMetadataResponse(
    val displayName: String?,
    val divisionName: String?,
    val teamNames: List<String>?,
    val supplierNames: List<String>?,
    val lifecyclePhase: String?,
    val inProductionSince: Int?,
    val businessCriticality: String?,
    val targetIndustry: String?,
    val deploymentType: String?,
    val applicationType: String?,
    val softwareDistributionStrategy: String?,
    val remark: String?,
    val externalID: String?,
    val isDevelopmentOnly: Boolean?,
    val mainTechnology: String?,
    val technologyCategory: String?,
)
