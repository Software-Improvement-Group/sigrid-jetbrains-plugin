package com.softwareimprovementgroup.plugins.sigrid.models

data class OnboardingRequest(
    val mode: String = "ONBOARDING",
    val capabilities: List<String>,
    val subsystem: String?,
    val convert: String? = null,
)

data class OnboardingUploadResponse(val uploadUrl: String?)

data class LicensesResponse(val licenses: List<String>?)
