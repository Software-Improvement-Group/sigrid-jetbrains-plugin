package com.softwareimprovementgroup.plugins.sigrid.models

enum class RefactoringCategory(val value: String) {
    Duplication("duplication"),
    UnitSize("unitSize"),
    UnitComplexity("unitComplexity"),
    UnitInterfacing("unitInterfacing"),
    ModuleCoupling("moduleCoupling"),
}

data class RefactoringCandidatesResponse(
    val refactoringCandidates: List<RefactoringCandidateResponse>,
)

data class RefactoringCandidateResponse(
    val id: String,
    val severity: String,
    val weight: Int,
    val status: String,
    val technology: String,
    val snapshotDate: String,
    val sameComponent: Boolean?,
    val sameFile: Boolean?,
    val mcCabe: Int?,
    val fanIn: Int?,
    val loc: Int?,
    val parameters: Int?,
    val locations: List<CandidateLocation>?,
    val component: String?,
    val file: String?,
    val name: String?,
    val moduleId: Int?,
    val startLine: Int?,
    val endLine: Int?,
    val lineRanges: List<LineRange>?,
)

data class CandidateLocation(
    val component: String,
    val file: String,
    val moduleId: Int,
    val startLine: Int,
    val endLine: Int,
)

data class LineRange(
    val startLine: Int,
    val endLine: Int,
)