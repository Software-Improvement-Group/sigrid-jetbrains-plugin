package com.softwareimprovementgroup.plugins.sigrid.models

import org.junit.Assert.assertEquals
import org.junit.Test

class RiskSeverityTest {

    @Test
    fun from_null_returnsUnknown() = assertEquals(RiskSeverity.Unknown, RiskSeverity.from(null))

    @Test
    fun from_emptyString_returnsUnknown() = assertEquals(RiskSeverity.Unknown, RiskSeverity.from(""))

    @Test
    fun from_unrecognizedString_returnsUnknown() = assertEquals(RiskSeverity.Unknown, RiskSeverity.from("bogus"))

    @Test
    fun from_none_returnsNone() = assertEquals(RiskSeverity.None, RiskSeverity.from("none"))

    @Test
    fun from_noneUppercase_returnsNone() = assertEquals(RiskSeverity.None, RiskSeverity.from("NONE"))

    @Test
    fun from_information_returnsInformation() = assertEquals(RiskSeverity.Information, RiskSeverity.from("information"))

    @Test
    fun from_info_returnsInformation() = assertEquals(RiskSeverity.Information, RiskSeverity.from("info"))

    @Test
    fun from_infoUppercase_returnsInformation() = assertEquals(RiskSeverity.Information, RiskSeverity.from("INFO"))

    @Test
    fun from_low_returnsLow() = assertEquals(RiskSeverity.Low, RiskSeverity.from("low"))

    @Test
    fun from_medium_returnsMedium() = assertEquals(RiskSeverity.Medium, RiskSeverity.from("medium"))

    @Test
    fun from_high_returnsHigh() = assertEquals(RiskSeverity.High, RiskSeverity.from("high"))

    @Test
    fun from_critical_returnsCritical() = assertEquals(RiskSeverity.Critical, RiskSeverity.from("critical"))

    @Test
    fun from_criticalMixedCase_returnsCritical() = assertEquals(RiskSeverity.Critical, RiskSeverity.from("Critical"))

    @Test
    fun ordinalOrder_increasesFromNoneToCritical() {
        // OpenSourceHealthMapper uses maxOf() which relies on natural enum ordinal ordering
        assertEquals(0, RiskSeverity.None.ordinal)
        assertEquals(1, RiskSeverity.Unknown.ordinal)
        assertEquals(2, RiskSeverity.Information.ordinal)
        assertEquals(3, RiskSeverity.Low.ordinal)
        assertEquals(4, RiskSeverity.Medium.ordinal)
        assertEquals(5, RiskSeverity.High.ordinal)
        assertEquals(6, RiskSeverity.Critical.ordinal)
    }
}
