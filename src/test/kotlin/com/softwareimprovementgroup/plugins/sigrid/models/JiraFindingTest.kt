package com.softwareimprovementgroup.plugins.sigrid.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JiraFindingTest {

    // region MaintainabilitySeverity.toJiraEmoji

    @Test
    fun maintainabilityToJiraEmoji_veryHigh_isRed() {
        assertEquals("🔴", MaintainabilitySeverity.VeryHigh.toJiraEmoji())
    }

    @Test
    fun maintainabilityToJiraEmoji_high_isRed() {
        assertEquals("🔴", MaintainabilitySeverity.High.toJiraEmoji())
    }

    @Test
    fun maintainabilityToJiraEmoji_moderate_isOrange() {
        assertEquals("🟠", MaintainabilitySeverity.Moderate.toJiraEmoji())
    }

    @Test
    fun maintainabilityToJiraEmoji_medium_isOrange() {
        assertEquals("🟠", MaintainabilitySeverity.Medium.toJiraEmoji())
    }

    @Test
    fun maintainabilityToJiraEmoji_low_isYellow() {
        assertEquals("🟡", MaintainabilitySeverity.Low.toJiraEmoji())
    }

    @Test
    fun maintainabilityToJiraEmoji_unknown_isWhite() {
        assertEquals("⚪", MaintainabilitySeverity.Unknown.toJiraEmoji())
    }

    // endregion

    // region RiskSeverity.toJiraEmoji

    @Test
    fun riskToJiraEmoji_critical_isRed() {
        assertEquals("🔴", RiskSeverity.Critical.toJiraEmoji())
    }

    @Test
    fun riskToJiraEmoji_high_isRed() {
        assertEquals("🔴", RiskSeverity.High.toJiraEmoji())
    }

    @Test
    fun riskToJiraEmoji_medium_isOrange() {
        assertEquals("🟠", RiskSeverity.Medium.toJiraEmoji())
    }

    @Test
    fun riskToJiraEmoji_low_isYellow() {
        assertEquals("🟡", RiskSeverity.Low.toJiraEmoji())
    }

    @Test
    fun riskToJiraEmoji_information_isBlue() {
        assertEquals("🔵", RiskSeverity.Information.toJiraEmoji())
    }

    @Test
    fun riskToJiraEmoji_none_isGreen() {
        assertEquals("🟢", RiskSeverity.None.toJiraEmoji())
    }

    @Test
    fun riskToJiraEmoji_unknown_isWhite() {
        assertEquals("⚪", RiskSeverity.Unknown.toJiraEmoji())
    }

    // endregion
}
