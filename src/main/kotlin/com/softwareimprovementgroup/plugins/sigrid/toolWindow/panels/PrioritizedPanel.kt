package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.mappers.ArchitectureQualityMapper
import com.softwareimprovementgroup.plugins.sigrid.mappers.OpenSourceHealthMapper
import com.softwareimprovementgroup.plugins.sigrid.mappers.PrioritizedFindingMapper
import com.softwareimprovementgroup.plugins.sigrid.mappers.RefactoringCandidateMapper
import com.softwareimprovementgroup.plugins.sigrid.mappers.SecurityFindingMapper
import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.FixItContext
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import com.softwareimprovementgroup.plugins.sigrid.models.OpenSourceHealthResponse
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityRank
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import com.softwareimprovementgroup.plugins.sigrid.models.toSeverityEmoji
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService
import com.softwareimprovementgroup.plugins.sigrid.services.vcs.BranchChangesProvider
import java.time.LocalDate

// Findings shown before "Show all": enough to be a focused first view without hiding anything permanently.
private const val INITIAL_FINDING_LIMIT = 50

class PrioritizedPanel(project: Project) : SigridPanel<PrioritizedFinding>(
    project,
    arrayOf(
        SigridBundle["column.capability"], SigridBundle["column.risk"], SigridBundle["column.location"],
        SigridBundle["column.description"], SigridBundle["column.status"], SigridBundle["column.reason"],
    ),
    centeredColumns = setOf(SigridBundle["column.capability"], SigridBundle["column.risk"], SigridBundle["column.status"]),
    columnFilters = listOf(
        ColumnFilterDef(
            columnName = SigridBundle["column.capability"],
            options = PriorityCapability.entries.map { FilterOption(it.label, it.name) },
            getOptionId = { capability.name },
        ),
        ColumnFilterDef(
            columnName = SigridBundle["column.risk"],
            options = PriorityRank.entries.map { FilterOption(it.toRiskIcon().label, it.name) },
            getOptionId = { priorityRank.name },
        ),
    ),
    // Capability and Status hold short, fixed-vocabulary text/icon labels, so they're capped like the
    // other panels' Risk/Status columns - without a cap they'd take an even share of leftover space
    // alongside genuinely long-text columns (Location, Description, Why), leaving those too narrow.
    // Risk here is icon-only, same as every other panel. Why isn't capped: its sentences are often the
    // longest content in the row, so it should compete for space on equal footing with Description.
    columnMaxWidths = mapOf(
        SigridBundle["column.capability"] to 150,
        SigridBundle["column.risk"] to 80,
        SigridBundle["column.status"] to 100,
    ),
    fileGroupingSupported = true,
    initialLimit = INITIAL_FINDING_LIMIT,
) {
    override val emptyMessage = SigridBundle["prioritized.empty"]
    override val pinnedTitle: String get() = SigridBundle["prioritized.your.files"]

    init {
        // Only the local changes are re-read: the branch diff doesn't change under the developer's feet and
        // costs a git call. No API refetch either - pinning just regroups the findings already loaded.
        BranchChangesProvider.getInstance(project).onUncommittedChanged {
            ApplicationManager.getApplication().invokeLater { refreshView() }
        }
        // At startup git may not know its repositories yet; the gate then falls back to the API's activity
        // data, so reload once git can answer.
        BranchChangesProvider.getInstance(project).onHistoryAvailable {
            ApplicationManager.getApplication().invokeLater { loadData() }
        }
    }

    override fun fetch(subsystem: String): List<PrioritizedFinding> {
        val api = SigridApiService.getInstance()
        val maintainability = RefactoringCandidateMapper.map(api.getAllRefactoringCandidates(project), subsystem)
        val security = SecurityFindingMapper.map(api.getSecurityFindings(project), subsystem)
        val reliability = SecurityFindingMapper.map(api.getReliabilityFindings(project), subsystem)
        val oshResponse = api.getOpenSourceHealthFindings(project)
        val openSourceHealth = OpenSourceHealthMapper.map(oshResponse, subsystem)
        val fileActivity = ArchitectureQualityMapper.map(api.getArchitectureQualityRaw(project), subsystem)
        // Used by the Objectives Gate for Security/Reliability's severity-scale objectives only (targetMetAtEnd) - 90
        // days is enough to establish that without depending on exactly when the user happens to refresh.
        val objectives = api.getObjectivesEvaluation(project, ninetyDaysAgo(), today())
        // Used by the Objectives Gate for Maintainability's rating-scale objective - the resolved target, with no date
        // range dependency (see ObjectivesGate for why this replaces objectives-evaluation for this case).
        val objectivesConfig = api.getObjectivesConfig(project)
        // The Objectives Gate's market-benchmark fallback, and Maintainability's "actual" side of its target comparison.
        val currentRatings = currentRatings(api, oshResponse)
        val branchChanges = BranchChangesProvider.getInstance(project)
        val changedFiles = branchChanges.refresh()
        // .testCodeFindings is intentionally unused here - test code is laned out of the main list per
        // the Prioritization Epic, but no dedicated view for that lane exists in the UI yet.
        return PrioritizedFindingMapper.map(
            maintainability, security, reliability, openSourceHealth, fileActivity, objectives, objectivesConfig, currentRatings, changedFiles, branchChanges.recentHistory,
        ).findings
    }

    private fun currentRatings(api: SigridApiService, oshResponse: OpenSourceHealthResponse): Map<PriorityCapability, Double> = buildMap {
        // Normally a cache hit - MaintainabilityRatingWarmupActivity keeps this warm and re-fetches it on
        // settings changes, rather than this refresh path hitting the network for it.
        api.getMaintainabilityRating(project)?.let { put(PriorityCapability.Maintainability, it) }
        // No separate call needed: OSH's rating already lives in the SBOM response fetched above.
        OpenSourceHealthMapper.systemRating(oshResponse)?.let { put(PriorityCapability.OpenSourceHealth, it) }
    }

    private fun today(): String = LocalDate.now().toString()
    private fun ninetyDaysAgo(): String = LocalDate.now().minusDays(90).toString()

    // Output stage of the Prioritization Epic's pipeline: "why it's here" rendered as its own column rather than a hover
    // tooltip, so it's visible at a glance rather than hidden behind a hover. Empty promotionReason means
    // plain severity ranking - shown as a baseline label rather than a blank cell, so every row answers
    // the "why" question.
    // A pinned row's reasons were computed at fetch time and may say "deprioritized", which contradicts
    // the row sitting in "Your files", so pinning replaces them.
    private val PrioritizedFinding.reasonLabel: String
        get() = when {
            isPinned() -> SigridBundle["prioritized.reason.your.file"]
            promotionReason.isNotEmpty() -> promotionReason.joinToString(" · ")
            else -> SigridBundle["prioritized.reason.default"]
        }

    override fun PrioritizedFinding.matchesSearch(query: String) =
        capability.label.contains(query, ignoreCase = true) ||
        locationLabel.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true) ||
        statusLabel.contains(query, ignoreCase = true) ||
        reasonLabel.contains(query, ignoreCase = true)

    override fun PrioritizedFinding.toRow(): Array<Any> = arrayOf(
        capability.label,
        priorityRank.toRiskIcon(),
        locationLabel,
        description,
        statusLabel,
        reasonLabel,
    )

    override fun PrioritizedFinding.isPinned(): Boolean {
        val changedFiles = BranchChangesProvider.getInstance(project).current
        return fileLocations.any { changedFiles.matches(it.filePath) }
    }

    override fun PrioritizedFinding.getFileLocations(): List<FileLocation> = fileLocations

    override fun PrioritizedFinding.toIssueFinding() = IssueFinding(
        title = description,
        severityEmoji = priorityRank.toSeverityEmoji(),
        fileLocations = fileLocations,
    )

    override fun PrioritizedFinding.toFixItContext() = FixItContext(
        category = capability.label,
        severity = priorityRank.toRiskIcon().label,
        title = "$locationLabel: $description",
        fileLocations = fileLocations,
        href = href,
    )

    override fun PrioritizedFinding.getHref() = href
    override fun PrioritizedFinding.getGroupKey() = fileGroupKey
    override fun groupOrder() = PrioritizedFinding.fileGroupOrder
    override fun PrioritizedFinding.isEditable() = editable
    override fun PrioritizedFinding.getId() = id
    override fun PrioritizedFinding.getDisplayLocation() = locationLabel
    override fun PrioritizedFinding.getEditDescription() = description
    override fun PrioritizedFinding.getStatusOptions() = statusOptions
    override fun PrioritizedFinding.getCurrentStatus() = currentStatusValue
    override fun PrioritizedFinding.getCurrentRemark() = remark
}
