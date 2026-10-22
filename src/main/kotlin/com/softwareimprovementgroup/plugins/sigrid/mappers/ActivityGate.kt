package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.intellij.openapi.diagnostic.thisLogger
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.ChangedFiles
import com.softwareimprovementgroup.plugins.sigrid.models.FileActivityData
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import com.softwareimprovementgroup.plugins.sigrid.services.vcs.ChangedFilesSource

// GATE 2 of the Epic 432 prioritization pipeline (design doc section 3, Dennis's decision): Maintainability
// only, a coarse demotion (not a ranking weight) on real per-file Git activity - a Maintainability finding in
// a file nobody has touched isn't something a developer working right now needs to see promoted. Findings are
// demoted one tier rather than removed: the AQ export only covers a short window, so "no activity" can just
// mean "quiet lately" and hiding the finding would lose it. A file is
// active when it has CHURN or COMMITS over Sigrid's change-history window. That window is whatever the system's
// history covers (up to ~1 year); the export has no per-file time series, so it can't be narrowed to "last N
// months" per file (see ArchitectureQuality.kt). Joined by normalized file path since AQ's raw export has no
// `component` field on FILE elements.
//
// Files the developer has changed on the current branch (or locally) are active by definition, whatever the
// export says, so they are never demoted.
//
// Fails open (no demotion) whenever activity data can't settle the question one way or the other:
// no history at all for the system, or no matching entry for this specific file - both mean "we don't
// know", not "it's dormant", and the design doc explicitly wants systems/files with no coverage to fall
// back to severity-only ranking rather than being silently hidden.
class ActivityGate(
    private val activity: FileActivityData,
    private val changedFiles: ChangedFiles = ChangedFiles.EMPTY,
    // Files touched in the local Git history over a longer window than the export covers; null = unavailable.
    private val localHistory: ChangedFiles? = null,
) {
    private val activityByPath = activity.fileActivities.associateBy { it.filePath }

    // Applies the gate to a whole list and logs what it did, so "is the gate demoting anything?" is
    // answerable from idea.log rather than by guessing. Never changes the list's size or order.
    fun apply(findings: List<PrioritizedFinding>): List<PrioritizedFinding> {
        val judged = findings.map { it to outcomeOf(it) }
        logSummary(judged.map { it.second })
        TempFilterLog.log("ActivityGate", "no churn or commits in ${activity.historyStartDate}..${activity.historyEndDate}, demoted", judged.filter { it.second == Outcome.Dormant }.map { it.first })
        return judged.map { (finding, outcome) -> if (outcome == Outcome.Dormant) demote(finding) else finding }
    }

    private fun demote(finding: PrioritizedFinding): PrioritizedFinding =
        finding.copy(
            priorityRank = ObjectivesGate.dampen(finding.priorityRank),
            promotionReason = finding.promotionReason + dormantReason(),
        )

    private fun dormantReason(): String {
        if (localHistory != null) return SigridBundle["prioritized.reason.dormant.local", ChangedFilesSource.RECENT_HISTORY_DAYS / DAYS_PER_MONTH]
        val start = activity.historyStartDate
        val end = activity.historyEndDate
        return if (start != null && end != null) SigridBundle["prioritized.reason.dormant", start, end] else SigridBundle["prioritized.reason.dormant.no.window"]
    }

    private fun outcomeOf(finding: PrioritizedFinding): Outcome {
        if (finding.capability != PriorityCapability.Maintainability) return Outcome.NotApplicable
        if (!activity.hasHistory) return Outcome.UnknownNoHistory

        val paths = finding.fileLocations.map { it.filePath }.filter { it.isNotBlank() }
        return if (paths.isEmpty()) Outcome.UnknownNoPaths else outcomeForPaths(paths)
    }

    private fun outcomeForPaths(paths: List<String>): Outcome {
        val known = paths.mapNotNull { activityByPath[it] }
        return when {
            paths.any { changedFiles.matches(it) || localHistory?.matches(it) == true } -> Outcome.Active
            known.isEmpty() -> Outcome.UnknownNoMatch
            known.any { it.hasActivity } -> Outcome.Active
            else -> Outcome.Dormant
        }
    }

    private fun logSummary(outcomes: List<Outcome>) {
        val counts = outcomes.groupingBy { it }.eachCount()
        thisLogger().info(
            "Activity gate: ${outcomes.count { it != Outcome.NotApplicable }} Maintainability findings evaluated, " +
                "${counts[Outcome.Dormant] ?: 0} demoted (no churn or commits in ${activity.historyStartDate}..${activity.historyEndDate}), " +
                "${counts[Outcome.Active] ?: 0} kept active, " +
                "kept unknown: noHistory=${counts[Outcome.UnknownNoHistory] ?: 0} noPaths=${counts[Outcome.UnknownNoPaths] ?: 0} " +
                "noMatch=${counts[Outcome.UnknownNoMatch] ?: 0}; hasHistory=${activity.hasHistory}, " +
                "files with churn data=${activity.fileActivities.size}",
        )
    }

    private companion object {
        const val DAYS_PER_MONTH = 30
    }

    private enum class Outcome { NotApplicable, Active, Dormant, UnknownNoHistory, UnknownNoPaths, UnknownNoMatch }
}
