package com.softwareimprovementgroup.plugins.sigrid.models

// Mirrors /rest/analysis-results/api/v1/objectives-evaluation/{customer}. This end point is portfolio-wide -
// it returns every system the caller has access to - so SigridApiService.getObjectivesEvaluation() filters
// the `systems` array down to the current project's system before returning it.
data class ObjectivesEvaluationResponse(
    val systems: List<SystemObjectivesResponse>,
)

data class SystemObjectivesResponse(
    val systemName: String,
    val objectives: List<ObjectiveEvaluationResponse>,
)

// `target` and `stateAtEnd` are polymorphic in the underlying API: a severity-scale objective (e.g.
// OSH_MAX_LICENSE_RISK, SECURITY_MAX_SEVERITY) reports them as a string ("LOW", "CRITICAL", ...), while a
// rating-scale objective (e.g. ARCHITECTURE_QUALITY, MAINTAINABILITY) reports them as a number (a 0.5-5.5
// star rating). Both are JSON scalars rather than objects, so Gson deserializes each into the matching
// Kotlin type at runtime (String or Double) - callers must check `is String` / `is Double` before use,
// which is exactly the "no equivalent benchmark for count/enum objectives" distinction Gate 3 needs to make.
data class ObjectiveEvaluationResponse(
    val type: String,
    val feature: String,
    val target: Any?,
    val targetMetAtStart: String?,
    val targetMetAtEnd: String?,
    val delta: String?,
    val stateAtEnd: Any?,
    val level: String,
    val parentId: Int?,
)
