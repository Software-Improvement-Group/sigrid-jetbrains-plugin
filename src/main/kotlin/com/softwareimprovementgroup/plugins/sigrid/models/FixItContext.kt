package com.softwareimprovementgroup.plugins.sigrid.models

/**
 * A single Sigrid finding, reduced to what the AI prompt needs. Mirrors the VS Code extension's
 * `FixFinding`: the descriptive detail is folded into [title], the category keys the per-category
 * instructions, and [href] adds an optional Sigrid link for humans/agents.
 */
data class FixItContext(
    val category: String,
    val severity: String,
    val title: String,
    val fileLocations: List<FileLocation>,
    val href: String?,
)
