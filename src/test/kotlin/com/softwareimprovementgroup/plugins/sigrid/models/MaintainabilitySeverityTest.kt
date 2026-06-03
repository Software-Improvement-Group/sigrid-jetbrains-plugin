package com.softwareimprovementgroup.plugins.sigrid.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MaintainabilitySeverityTest {

    @Test
    fun from_veryHigh_returnsVeryHigh() = assertEquals(MaintainabilitySeverity.VeryHigh, MaintainabilitySeverity.from("very_high"))

    @Test
    fun from_veryHighUppercase_returnsVeryHigh() = assertEquals(MaintainabilitySeverity.VeryHigh, MaintainabilitySeverity.from("VERY_HIGH"))

    @Test
    fun from_high_returnsHigh() = assertEquals(MaintainabilitySeverity.High, MaintainabilitySeverity.from("high"))

    @Test
    fun from_moderate_returnsModerate() = assertEquals(MaintainabilitySeverity.Moderate, MaintainabilitySeverity.from("moderate"))

    @Test
    fun from_medium_returnsMedium() = assertEquals(MaintainabilitySeverity.Medium, MaintainabilitySeverity.from("medium"))

    @Test
    fun from_low_returnsLow() = assertEquals(MaintainabilitySeverity.Low, MaintainabilitySeverity.from("low"))

    @Test
    fun from_unrecognized_returnsUnknown() = assertEquals(MaintainabilitySeverity.Unknown, MaintainabilitySeverity.from("bogus"))

    @Test
    fun from_emptyString_returnsUnknown() = assertEquals(MaintainabilitySeverity.Unknown, MaintainabilitySeverity.from(""))
}
