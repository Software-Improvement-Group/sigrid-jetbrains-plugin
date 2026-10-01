package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.ObjectiveEvaluationResponse
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityRank
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObjectivesGateTest {

    private fun makeObjective(feature: String, targetMetAtEnd: String?) = ObjectiveEvaluationResponse(
        type = "irrelevant",
        feature = feature,
        target = null,
        targetMetAtStart = null,
        targetMetAtEnd = targetMetAtEnd,
        delta = null,
        stateAtEnd = null,
        level = "SYSTEM",
        parentId = null,
    )

    private fun makeFinding(
        capability: PriorityCapability,
        priorityRank: PriorityRank = PriorityRank.Critical,
        promotionReason: List<String> = emptyList(),
    ) = PrioritizedFinding(
        id = "id",
        capability = capability,
        priorityRank = priorityRank,
        displayLocation = "",
        description = "",
        statusLabel = "",
        remark = "",
        fileLocations = emptyList(),
        href = null,
        editable = false,
        statusOptions = emptyList(),
        currentStatusValue = "",
        promotionReason = promotionReason,
    )

    // isObjectiveMet: Security/Reliability - severity-scale, resolved via objectives-evaluation's
    // pre-computed targetMetAtEnd (unchanged by the target/actual rework, since those objective types have
    // no cheap standalone "current worst severity" value to compare directly).

    @Test
    fun isObjectiveMet_severityScale_met_isTrue() {
        assertTrue(ObjectivesGate.isObjectiveMet(PriorityCapability.Security, listOf(makeObjective("SECURITY", "MET"))))
    }

    @Test
    fun isObjectiveMet_severityScale_unmet_isFalse() {
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.Reliability, listOf(makeObjective("RELIABILITY", "UNMET"))))
    }

    @Test
    fun isObjectiveMet_severityScale_unknown_isFalse() {
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.Security, listOf(makeObjective("SECURITY", "UNKNOWN"))))
    }

    @Test
    fun isObjectiveMet_severityScale_noMatchingObjective_isFalse() {
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.Security, listOf(makeObjective("RELIABILITY", "MET"))))
    }

    @Test
    fun isObjectiveMet_severityScale_noObjectivesAtAll_isFalse() {
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.Reliability, emptyList()))
    }

    // isObjectiveMet: Maintainability - rating-scale, computed directly as actual >= target. Target comes
    // from objectivesConfig (objectives/config, resolved via Sigrid's own precedence), actual from
    // currentRatings (SigridApiService.getMaintainabilityRating). objectives-evaluation is never consulted
    // for this capability.

    @Test
    fun isObjectiveMet_maintainability_actualAtOrAboveConfiguredTarget_isTrue() {
        val config = mapOf("MAINTAINABILITY" to 3.5)
        val ratings = mapOf(PriorityCapability.Maintainability to 3.5)
        assertTrue(ObjectivesGate.isObjectiveMet(PriorityCapability.Maintainability, emptyList(), config, ratings))
    }

    @Test
    fun isObjectiveMet_maintainability_actualBelowConfiguredTarget_isFalse() {
        // The design doc's own worked example: 3.48 actual vs. 3.5 target is a documented miss.
        val config = mapOf("MAINTAINABILITY" to 3.5)
        val ratings = mapOf(PriorityCapability.Maintainability to 3.48)
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.Maintainability, emptyList(), config, ratings))
    }

    @Test
    fun isObjectiveMet_maintainability_configPresentButNoRating_isFalse() {
        val config = mapOf("MAINTAINABILITY" to 3.5)
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.Maintainability, emptyList(), config, emptyMap()))
    }

    @Test
    fun isObjectiveMet_maintainability_objectivesEvaluationNeverConsulted() {
        // Even a MET severity-style entry under the MAINTAINABILITY feature must be ignored - Maintainability
        // only ever compares objectivesConfig's target against currentRatings' actual.
        val objectives = listOf(makeObjective("MAINTAINABILITY", "MET"))
        val ratings = mapOf(PriorityCapability.Maintainability to 1.0)
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.Maintainability, objectives, emptyMap(), ratings))
    }

    // isObjectiveMet: Maintainability market-benchmark fallback (no objectivesConfig entry at all)

    @Test
    fun isObjectiveMet_maintainability_noConfiguredTarget_ratingAboveMarketAverage_isTrue() {
        val ratings = mapOf(PriorityCapability.Maintainability to 3.5)
        assertTrue(ObjectivesGate.isObjectiveMet(PriorityCapability.Maintainability, emptyList(), emptyMap(), ratings))
    }

    @Test
    fun isObjectiveMet_maintainability_noConfiguredTarget_ratingExactlyAtMarketAverage_isTrue() {
        val ratings = mapOf(PriorityCapability.Maintainability to 3.0)
        assertTrue(ObjectivesGate.isObjectiveMet(PriorityCapability.Maintainability, emptyList(), emptyMap(), ratings))
    }

    @Test
    fun isObjectiveMet_maintainability_noConfiguredTarget_ratingBelowMarketAverage_isFalse() {
        val ratings = mapOf(PriorityCapability.Maintainability to 2.9)
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.Maintainability, emptyList(), emptyMap(), ratings))
    }

    @Test
    fun isObjectiveMet_maintainability_noConfiguredTargetAndNoRating_isFalse() {
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.Maintainability, emptyList(), emptyMap(), emptyMap()))
    }

    // isObjectiveMet: Open Source Health - Sigrid has no rating-scale OSH objective type at all, so this
    // always compares straight against the market-average benchmark; objectivesConfig/objectives are never
    // consulted for it.

    @Test
    fun isObjectiveMet_openSourceHealth_ratingAboveMarketAverage_isTrue() {
        val ratings = mapOf(PriorityCapability.OpenSourceHealth to 3.89)
        assertTrue(ObjectivesGate.isObjectiveMet(PriorityCapability.OpenSourceHealth, emptyList(), emptyMap(), ratings))
    }

    @Test
    fun isObjectiveMet_openSourceHealth_ratingBelowMarketAverage_isFalse() {
        val ratings = mapOf(PriorityCapability.OpenSourceHealth to 1.4)
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.OpenSourceHealth, emptyList(), emptyMap(), ratings))
    }

    @Test
    fun isObjectiveMet_openSourceHealth_noRatingAvailable_isFalse() {
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.OpenSourceHealth, emptyList(), emptyMap(), emptyMap()))
    }

    @Test
    fun isObjectiveMet_openSourceHealth_matchingSeverityObjective_neverConsulted() {
        // A real OSH_MAX_SEVERITY/OSH_MAX_LICENSE_RISK entry (feature=OPEN_SOURCE_HEALTH) is a *severity*
        // objective, not a rating one - it must never be mistaken for "OSH's rating objective is met".
        val objectives = listOf(makeObjective("OPEN_SOURCE_HEALTH", "MET"))
        assertFalse(ObjectivesGate.isObjectiveMet(PriorityCapability.OpenSourceHealth, objectives, emptyMap(), emptyMap()))
    }

    // dampen

    @Test
    fun dampen_criticalBecomesHigh() {
        assertEquals(PriorityRank.High, ObjectivesGate.dampen(PriorityRank.Critical))
    }

    @Test
    fun dampen_unknownStaysUnknown() {
        assertEquals(PriorityRank.Unknown, ObjectivesGate.dampen(PriorityRank.Unknown))
    }

    @Test
    fun dampen_lowFloorsAtLow_doesNotFallToUnknown() {
        // Regression guard: Unknown means "no severity data" elsewhere in the codebase - a merely
        // low-severity finding (e.g. an Information-severity Security finding) must not be dampened into
        // that same "Unknown" bucket, or it reads as a mapping bug rather than an objective-met dampening.
        assertEquals(PriorityRank.Low, ObjectivesGate.dampen(PriorityRank.Low))
    }

    @Test
    fun dampen_mediumBecomesLow() {
        assertEquals(PriorityRank.Low, ObjectivesGate.dampen(PriorityRank.Medium))
    }

    // applyIfMet

    @Test
    fun applyIfMet_severityScaleObjectiveMet_ranksDampened() {
        val finding = makeFinding(PriorityCapability.Security, PriorityRank.Critical)
        val result = ObjectivesGate.applyIfMet(finding, listOf(makeObjective("SECURITY", "MET")))
        assertEquals(PriorityRank.High, result.priorityRank)
    }

    @Test
    fun applyIfMet_severityScaleObjectiveNotMet_unchanged() {
        val finding = makeFinding(PriorityCapability.Security, PriorityRank.Critical)
        val result = ObjectivesGate.applyIfMet(finding, listOf(makeObjective("SECURITY", "UNMET")))
        assertEquals(PriorityRank.Critical, result.priorityRank)
    }

    @Test
    fun applyIfMet_maintainabilityTargetMet_ranksDampened() {
        val finding = makeFinding(PriorityCapability.Maintainability, PriorityRank.Critical)
        val config = mapOf("MAINTAINABILITY" to 3.5)
        val ratings = mapOf(PriorityCapability.Maintainability to 4.0)
        val result = ObjectivesGate.applyIfMet(finding, emptyList(), config, ratings)
        assertEquals(PriorityRank.High, result.priorityRank)
    }

    @Test
    fun applyIfMet_maintainabilityTargetMissed_unchanged() {
        val finding = makeFinding(PriorityCapability.Maintainability, PriorityRank.Critical)
        val config = mapOf("MAINTAINABILITY" to 3.5)
        val ratings = mapOf(PriorityCapability.Maintainability to 3.48)
        val result = ObjectivesGate.applyIfMet(finding, emptyList(), config, ratings)
        assertEquals(PriorityRank.Critical, result.priorityRank)
    }

    @Test
    fun applyIfMet_lowSeverityFindingWithMetObjective_staysLowNotUnknown() {
        // Reproduces the previously-reported bug: a Security finding derived from RiskSeverity.Information
        // maps to PriorityRank.Low (see RiskSeverity.toPriorityRank) - once its objective is met, it must
        // stay Low, not fall to Unknown.
        val finding = makeFinding(PriorityCapability.Security, PriorityRank.Low)
        val result = ObjectivesGate.applyIfMet(finding, listOf(makeObjective("SECURITY", "MET")))
        assertEquals(PriorityRank.Low, result.priorityRank)
    }

    // applyIfMet: promotionReason labeling - section 2.7 requires the market-benchmark fallback to be
    // "clearly labeled... so it's never mistaken for something the customer chose", so EXPLICIT_OBJECTIVE
    // and MARKET_BENCHMARK must each attach their own distinct, correct label.

    @Test
    fun applyIfMet_severityScaleObjectiveMet_addsExplicitObjectiveReason() {
        val finding = makeFinding(PriorityCapability.Security)
        val result = ObjectivesGate.applyIfMet(finding, listOf(makeObjective("SECURITY", "MET")))
        assertEquals(
            listOf(SigridBundle["prioritized.reason.objective.met", PriorityCapability.Security.label]),
            result.promotionReason,
        )
    }

    @Test
    fun applyIfMet_maintainabilityExplicitTargetMet_addsExplicitObjectiveReason() {
        val finding = makeFinding(PriorityCapability.Maintainability)
        val config = mapOf("MAINTAINABILITY" to 3.5)
        val ratings = mapOf(PriorityCapability.Maintainability to 4.0)
        val result = ObjectivesGate.applyIfMet(finding, emptyList(), config, ratings)
        assertEquals(
            listOf(SigridBundle["prioritized.reason.objective.met", PriorityCapability.Maintainability.label]),
            result.promotionReason,
        )
    }

    @Test
    fun applyIfMet_maintainabilityMarketBenchmarkFallback_addsMarketBenchmarkReason() {
        val finding = makeFinding(PriorityCapability.Maintainability)
        val ratings = mapOf(PriorityCapability.Maintainability to 3.5)
        val result = ObjectivesGate.applyIfMet(finding, emptyList(), emptyMap(), ratings)
        assertEquals(
            listOf(SigridBundle["prioritized.reason.market.benchmark", PriorityCapability.Maintainability.label]),
            result.promotionReason,
        )
    }

    @Test
    fun applyIfMet_openSourceHealth_alwaysUsesMarketBenchmarkReason() {
        // OSH has no rating-scale objective type at all, so it's always the fallback label, never the
        // explicit-objective one - even though a config/rating pair resembling an "explicit" case exists.
        val finding = makeFinding(PriorityCapability.OpenSourceHealth)
        val ratings = mapOf(PriorityCapability.OpenSourceHealth to 3.89)
        val result = ObjectivesGate.applyIfMet(finding, emptyList(), emptyMap(), ratings)
        assertEquals(
            listOf(SigridBundle["prioritized.reason.market.benchmark", PriorityCapability.OpenSourceHealth.label]),
            result.promotionReason,
        )
    }

    @Test
    fun applyIfMet_objectiveNotMet_promotionReasonUnchanged() {
        val finding = makeFinding(PriorityCapability.Security)
        val result = ObjectivesGate.applyIfMet(finding, listOf(makeObjective("SECURITY", "UNMET")))
        assertTrue(result.promotionReason.isEmpty())
    }

    @Test
    fun applyIfMet_appendsToExistingPromotionReason_doesNotOverwrite() {
        // Append-only design: a finding can legitimately already carry a reason from an earlier pipeline
        // stage (e.g. Security's "always first" label) - Gate 3 must add to that list, not replace it.
        val finding = makeFinding(PriorityCapability.Security, promotionReason = listOf("earlier reason"))
        val result = ObjectivesGate.applyIfMet(finding, listOf(makeObjective("SECURITY", "MET")))
        assertEquals(
            listOf("earlier reason", SigridBundle["prioritized.reason.objective.met", PriorityCapability.Security.label]),
            result.promotionReason,
        )
    }
}
