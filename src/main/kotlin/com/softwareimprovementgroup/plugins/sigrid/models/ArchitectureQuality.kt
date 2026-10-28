package com.softwareimprovementgroup.plugins.sigrid.models

// Mirrors Sigrid's "raw" Architecture Quality export
// (see https://docs.sigrid-says.com/reference/aq-json-export-format.html). This is a much larger export; only
// the fields needed by its two consumers are modelled: the per-file Git activity signal (used by the Prioritization Epic's Activity Gate,
// `measurementValues`) and the Architecture tab's findings (`shortName` and `dependencies`).
//
// Confirmed against a real export: FILE elements carry their Git activity only as scalar
// `measurementValues` (CHURN, COMMITS, AUTHORS) aggregated over the whole history window. Their
// `measurementTimeSeries` is always empty - weekly time series exist only on CODE_COMPONENT elements.

data class ArchitectureQualityRawResponse(
    val metadata: ArchitectureQualityMetadataResponse,
    val systemElements: List<SystemElementResponse>,
    val dependencies: List<ArchitectureDependencyResponse>? = null,
)

data class ArchitectureQualityMetadataResponse(
    val historyInterval: String?,
    val historyStartDate: String?,
    val historyEndDate: String?,
    val historyCommitCount: Int?,
)

data class SystemElementResponse(
    val id: String?,
    val type: String?,
    val name: String?,
    val shortName: String? = null,
    val measurementValues: Map<String, Double>? = null,
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

// Per-file Git activity (CHURN = lines changed, COMMITS = commit count, both over the export's history
// window), extracted from the FILE-type system
// elements. Only files where Sigrid actually reported a CHURN or COMMITS value are represented - a missing entry means
// "no activity data for this file", not "zero activity", which matters for the Activity Gate's decision to fall back to
// severity-only when a system (or a specific file within it) has no change-history coverage.
//
// Confirmed against a real export: a FILE element's `name` is the full repo-relative path
// (e.g. "src/main/java/com/exin/astride/controller/FooController.java"), unlike Maintainability/Security
// responses, which carry an explicit `component` field. Architecture Quality has no such field on FILE
// elements - a file's owning component only exists via the CODE_COMPONENT hierarchy and CONTAINS edges in
// the dependency graph (not modelled here), so there's deliberately no `component` on this class. The Activity Gate
// should join FileActivity to findings by normalized file path, not by a fabricated component match.
data class FileActivity(
    val filePath: String,
    val churn: Double,
    val commits: Double,
) {
    // Churn and commits can disagree at the edges (e.g. a commit that changes no lines), so either one counts.
    val hasActivity: Boolean get() = churn > 0.0 || commits > 0.0
}

data class FileActivityData(
    val hasHistory: Boolean,
    val historyStartDate: String?,
    val historyEndDate: String?,
    val fileActivities: List<FileActivity>,
)
