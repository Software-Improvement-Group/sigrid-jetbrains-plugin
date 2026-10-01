package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.ObjectiveEvaluationResponse
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityRank
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding

// GATE 3 of the Epic 432 prioritization pipeline (design doc section 3/2.7): system-level dampening when a
// capability's own quality objective is already met - a system that deteriorated but still meets its
// target isn't the hotspot Epic 432 is trying to surface.
//
// Two different comparisons live here, because Sigrid's objective types split the same way:
//  - Severity-scale (Security: SECURITY_MAX_SEVERITY, Reliability: RELIABILITY_MAX_SEVERITY) - resolved via
//    objectives-evaluation's own pre-computed `targetMetAtEnd`, keyed by `feature`. There's no cheap way to
//    fetch "current worst severity" as a standalone value, so this trusts the server's own comparison.
//  - Rating-scale, 0.5-5.5 stars (Maintainability only - see below on OSH) - computed here directly as
//    actual >= target, matching section 2.7's own worked example ("Maintainability 3.48 vs. 3.5 target" is
//    a documented miss). `target` comes from the objectives/config end point (SigridApiService.
//    getObjectivesConfig - resolved via Sigrid's system-over-portfolio precedence, no date range needed),
//    `actual` from SigridApiService.getMaintainabilityRating(). This deliberately does NOT use
//    objectives-evaluation for Maintainability: that endpoint's targetMetAtEnd is computed as of a
//    caller-supplied date range's end date, which is an extra, avoidable source of staleness/mismatch
//    when a simpler, date-range-free target+actual comparison is available instead.
//
// Open Source Health has no rating-scale objective type in Sigrid at all (its type table only has
// OSH_MAX_SEVERITY/OSH_MAX_FRESHNESS_RISK/OSH_MAX_LICENSE_RISK, all severity-scale) - so despite section
// 2.7 naming "Maintainability, OSH overall rating" together as the two rating-based capabilities eligible
// for the market-benchmark fallback, OSH can never have an explicit *rating* objective to check first.
// Its rating (from OpenSourceHealthMapper.systemRating) is therefore always compared straight against the
// 3.0 market-average benchmark - there's no "explicit objective" branch to fall through from.
//
// Section 2.7 explicitly requires the market-benchmark fallback to be "clearly labeled wherever shown...
// so it's never mistaken for something the customer chose" - MetVia tracks which of the two applied so
// applyIfMet() can attach the right label as a promotionReason (see PrioritizedFinding.promotionReason).
object ObjectivesGate {
    private const val MET = "MET"
    private const val MARKET_AVERAGE_RATING = 3.0
    private const val MAINTAINABILITY_CONFIG_KEY = "MAINTAINABILITY"

    private val SEVERITY_SCALE_FEATURE_BY_CAPABILITY = mapOf(
        PriorityCapability.Security to "SECURITY",
        PriorityCapability.Reliability to "RELIABILITY",
    )

    private enum class MetVia { NOT_MET, EXPLICIT_OBJECTIVE, MARKET_BENCHMARK }

    fun isObjectiveMet(
        capability: PriorityCapability,
        objectives: List<ObjectiveEvaluationResponse>,
        objectivesConfig: Map<String, Any> = emptyMap(),
        currentRatings: Map<PriorityCapability, Double> = emptyMap(),
    ): Boolean = metVia(capability, objectives, objectivesConfig, currentRatings) != MetVia.NOT_MET

    private fun metVia(
        capability: PriorityCapability,
        objectives: List<ObjectiveEvaluationResponse>,
        objectivesConfig: Map<String, Any>,
        currentRatings: Map<PriorityCapability, Double>,
    ): MetVia = when (capability) {
        PriorityCapability.Maintainability -> maintainabilityMetVia(objectivesConfig, currentRatings)
        PriorityCapability.OpenSourceHealth ->
            if (isAboveMarketAverage(currentRatings[capability])) MetVia.MARKET_BENCHMARK else MetVia.NOT_MET
        else -> if (isSeverityObjectiveMet(capability, objectives)) MetVia.EXPLICIT_OBJECTIVE else MetVia.NOT_MET
    }

    private fun isSeverityObjectiveMet(capability: PriorityCapability, objectives: List<ObjectiveEvaluationResponse>): Boolean {
        val feature = SEVERITY_SCALE_FEATURE_BY_CAPABILITY[capability] ?: return false
        return objectives.any { it.feature == feature && it.targetMetAtEnd == MET }
    }

    private fun maintainabilityMetVia(objectivesConfig: Map<String, Any>, currentRatings: Map<PriorityCapability, Double>): MetVia {
        val actual = currentRatings[PriorityCapability.Maintainability] ?: return MetVia.NOT_MET
        val configuredTarget = (objectivesConfig[MAINTAINABILITY_CONFIG_KEY] as? Number)?.toDouble()
        val target = configuredTarget ?: MARKET_AVERAGE_RATING
        if (actual < target) return MetVia.NOT_MET
        return if (configuredTarget != null) MetVia.EXPLICIT_OBJECTIVE else MetVia.MARKET_BENCHMARK
    }

    private fun isAboveMarketAverage(rating: Double?): Boolean = rating != null && rating >= MARKET_AVERAGE_RATING

    // Dampens by exactly one severity tier - coarse, not a primary sort key. Floors at Low rather than
    // Unknown: Unknown specifically means "no severity data available" elsewhere in this codebase (see
    // RiskSeverity.toPriorityRank/MaintainabilitySeverity.toPriorityRank) - reusing it as the dampening
    // floor would make a merely-low-severity, objective-met finding indistinguishable in the UI from one
    // whose severity couldn't be determined at all.
    fun dampen(rank: PriorityRank): PriorityRank =
        if (rank.ordinal <= PriorityRank.Low.ordinal) rank else PriorityRank.entries[rank.ordinal - 1]

    fun applyIfMet(
        finding: PrioritizedFinding,
        objectives: List<ObjectiveEvaluationResponse>,
        objectivesConfig: Map<String, Any> = emptyMap(),
        currentRatings: Map<PriorityCapability, Double> = emptyMap(),
    ): PrioritizedFinding {
        val via = metVia(finding.capability, objectives, objectivesConfig, currentRatings)
        if (via == MetVia.NOT_MET) return finding
        return finding.copy(
            priorityRank = dampen(finding.priorityRank),
            promotionReason = finding.promotionReason + reasonFor(finding.capability, via),
        )
    }

    private fun reasonFor(capability: PriorityCapability, via: MetVia): String = when (via) {
        MetVia.EXPLICIT_OBJECTIVE -> SigridBundle["prioritized.reason.objective.met", capability.label]
        MetVia.MARKET_BENCHMARK -> SigridBundle["prioritized.reason.market.benchmark", capability.label]
        MetVia.NOT_MET -> ""
    }
}
