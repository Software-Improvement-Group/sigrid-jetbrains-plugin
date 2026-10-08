package com.softwareimprovementgroup.plugins.sigrid.models

// Mirrors Sigrid's "raw" Architecture Quality export
// (see https://docs.sigrid-says.com/reference/aq-json-export-format.html). This is a much larger export -
// it also contains a full dependency graph - but only the fields needed for the per-file Git activity
// signal (Epic 432 Gate 2) are modelled here.
//
// Confirmed against a real export (sigrid-backend): FILE elements carry their Git activity only as scalar
// `measurementValues` (CHURN, COMMITS, AUTHORS) aggregated over the whole history window. Their
// `measurementTimeSeries` is always empty - weekly time series exist only on CODE_COMPONENT elements.

data class ArchitectureQualityRawResponse(
    val metadata: ArchitectureQualityMetadataResponse,
    val systemElements: List<SystemElementResponse>,
)

data class ArchitectureQualityMetadataResponse(
    val historyInterval: String?,
    val historyStartDate: String?,
    val historyEndDate: String?,
    val historyCommitCount: Int?,
)

data class SystemElementResponse(
    val id: String,
    val name: String,
    val type: String,
    val measurementValues: Map<String, Double>?,
)

// Per-file Git activity (CHURN = lines changed, COMMITS = commit count, both over the export's history
// window), extracted from the FILE-type system
// elements. Only files where Sigrid actually reported a CHURN or COMMITS value are represented - a missing entry means
// "no activity data for this file", not "zero activity", which matters for Gate 2's decision to fall back to
// severity-only when a system (or a specific file within it) has no change-history coverage.
//
// Confirmed against a real export: a FILE element's `name` is the full repo-relative path
// (e.g. "src/main/java/com/exin/astride/controller/FooController.java"), unlike Maintainability/Security
// responses, which carry an explicit `component` field. Architecture Quality has no such field on FILE
// elements - a file's owning component only exists via the CODE_COMPONENT hierarchy and CONTAINS edges in
// the dependency graph (not modelled here), so there's deliberately no `component` on this class. Gate 2
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
