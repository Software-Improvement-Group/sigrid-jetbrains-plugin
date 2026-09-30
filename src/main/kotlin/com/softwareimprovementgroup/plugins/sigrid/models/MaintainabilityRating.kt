package com.softwareimprovementgroup.plugins.sigrid.models

// Mirrors GET .../maintainability/{customer}/{system} - used only by ObjectivesGate's Gate 3
// market-benchmark fallback, for Maintainability findings with no explicit quality objective configured
// for this system.
//
// NOTE on a prior mistake: this used to model .../model-ratings/{customer}/{system}?feature=X instead,
// on the assumption that endpoint accepted MAINTAINABILITY/OPEN_SOURCE_HEALTH as feature values. It
// doesn't - model-ratings returns per-named-model adherence (OWASP Top 10, SIG Security, CWE Top-25, ...)
// for SECURITY/RELIABILITY specifically, which have no "overall star rating" concept the way
// Maintainability and OSH do; calling it with feature=MAINTAINABILITY returns HTTP 400. Maintainability's
// actual overall rating lives on its own dedicated endpoint (this one); OSH's lives inside the OSH SBOM
// response's own metadata.properties (see OpenSourceHealthMapper.systemRating) - no separate endpoint at
// all is needed for OSH.
//
// The public API docs show only one example response for this endpoint's *customer*-level (portfolio-wide)
// variant, wrapped in a "systems" array. Every other endpoint in this API follows the same pattern
// (system-metadata, architecture-quality): customer-level returns an array/wrapper, system-level returns a
// bare single object. This models the system-level shape accordingly - not confirmed against a live
// response, so worth double-checking if `maintainability` here ever comes back null unexpectedly.
data class MaintainabilityRatingResponse(
    val system: String?,
    val maintainability: Double?,
    val maintainabilityDate: String?,
)
