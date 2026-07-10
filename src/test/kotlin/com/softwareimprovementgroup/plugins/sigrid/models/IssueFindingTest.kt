package com.softwareimprovementgroup.plugins.sigrid.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IssueFindingTest {

    // region MaintainabilitySeverity.toSeverityEmoji

    @Test
    fun maintainabilityToSeverityEmoji_veryHigh_isRed() {
        assertEquals("🔴", MaintainabilitySeverity.VeryHigh.toSeverityEmoji())
    }

    @Test
    fun maintainabilityToSeverityEmoji_high_isRed() {
        assertEquals("🔴", MaintainabilitySeverity.High.toSeverityEmoji())
    }

    @Test
    fun maintainabilityToSeverityEmoji_moderate_isOrange() {
        assertEquals("🟠", MaintainabilitySeverity.Moderate.toSeverityEmoji())
    }

    @Test
    fun maintainabilityToSeverityEmoji_medium_isOrange() {
        assertEquals("🟠", MaintainabilitySeverity.Medium.toSeverityEmoji())
    }

    @Test
    fun maintainabilityToSeverityEmoji_low_isYellow() {
        assertEquals("🟡", MaintainabilitySeverity.Low.toSeverityEmoji())
    }

    @Test
    fun maintainabilityToSeverityEmoji_unknown_isWhite() {
        assertEquals("⚪", MaintainabilitySeverity.Unknown.toSeverityEmoji())
    }

    // endregion

    // region RiskSeverity.toSeverityEmoji

    @Test
    fun riskToSeverityEmoji_critical_isRed() {
        assertEquals("🔴", RiskSeverity.Critical.toSeverityEmoji())
    }

    @Test
    fun riskToSeverityEmoji_high_isRed() {
        assertEquals("🔴", RiskSeverity.High.toSeverityEmoji())
    }

    @Test
    fun riskToSeverityEmoji_medium_isOrange() {
        assertEquals("🟠", RiskSeverity.Medium.toSeverityEmoji())
    }

    @Test
    fun riskToSeverityEmoji_low_isYellow() {
        assertEquals("🟡", RiskSeverity.Low.toSeverityEmoji())
    }

    @Test
    fun riskToSeverityEmoji_information_isBlue() {
        assertEquals("🔵", RiskSeverity.Information.toSeverityEmoji())
    }

    @Test
    fun riskToSeverityEmoji_none_isGreen() {
        assertEquals("🟢", RiskSeverity.None.toSeverityEmoji())
    }

    @Test
    fun riskToSeverityEmoji_unknown_isWhite() {
        assertEquals("⚪", RiskSeverity.Unknown.toSeverityEmoji())
    }

    // endregion
}