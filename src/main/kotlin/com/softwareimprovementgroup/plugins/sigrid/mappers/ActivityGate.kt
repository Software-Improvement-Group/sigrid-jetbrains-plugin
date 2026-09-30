package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.FileActivityData
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding

// GATE 2 of the Epic 432 prioritization pipeline (design doc section 3, Dennis's decision): Maintainability
// only, a coarse filter (not a ranking weight) on real per-file Git activity - a Maintainability finding in
// a file nobody has touched isn't something a developer working right now needs to see promoted. Uses
// ArchitectureQualityMapper's FileActivity data (per-file CHURN), joined by normalized file path since AQ's
// raw export has no `component` field on FILE elements (see ArchitectureQuality.kt).
//
// Fails open (keeps the finding) whenever activity data can't settle the question one way or the other:
// no history at all for the system, or no matching entry for this specific file - both mean "we don't
// know", not "it's dormant", and the design doc explicitly wants systems/files with no coverage to fall
// back to severity-only ranking rather than being silently hidden.
object ActivityGate {
    fun isActiveOrUnknown(finding: PrioritizedFinding, activity: FileActivityData): Boolean {
        if (finding.capability != PriorityCapability.Maintainability) return true
        if (!activity.hasHistory) return true

        val paths = finding.fileLocations.map { it.filePath }.filter { it.isNotBlank() }
        if (paths.isEmpty()) return true

        val churnByPath = activity.fileActivities.associateBy { it.filePath }
        val known = paths.mapNotNull { churnByPath[it] }
        if (known.isEmpty()) return true

        return known.any { it.averageChurn > 0.0 }
    }
}
