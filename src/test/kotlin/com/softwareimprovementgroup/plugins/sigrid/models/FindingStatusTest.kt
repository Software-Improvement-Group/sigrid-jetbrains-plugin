package com.softwareimprovementgroup.plugins.sigrid.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FindingStatusTest {

    // FindingStatus.from

    @Test
    fun findingStatus_from_raw() = assertEquals(FindingStatus.Raw, FindingStatus.from("RAW"))

    @Test
    fun findingStatus_from_refined() = assertEquals(FindingStatus.Refined, FindingStatus.from("REFINED"))

    @Test
    fun findingStatus_from_willFix() = assertEquals(FindingStatus.WillFix, FindingStatus.from("WILL_FIX"))

    @Test
    fun findingStatus_from_fixed() = assertEquals(FindingStatus.Fixed, FindingStatus.from("FIXED"))

    @Test
    fun findingStatus_from_accepted() = assertEquals(FindingStatus.Accepted, FindingStatus.from("ACCEPTED"))

    @Test
    fun findingStatus_from_falsePositive() = assertEquals(FindingStatus.FalsePositive, FindingStatus.from("FALSE_POSITIVE"))

    @Test
    fun findingStatus_from_unrecognized_returnsRaw() = assertEquals(FindingStatus.Raw, FindingStatus.from("UNKNOWN_VALUE"))

    @Test
    fun findingStatus_from_lowercase_returnsRaw() {
        // apiValue comparison is exact; lowercase does not match
        assertEquals(FindingStatus.Raw, FindingStatus.from("will_fix"))
    }

    @Test
    fun findingStatus_from_emptyString_returnsRaw() = assertEquals(FindingStatus.Raw, FindingStatus.from(""))

    // MaintainabilityFindingStatus.from

    @Test
    fun maintainabilityFindingStatus_from_raw() = assertEquals(MaintainabilityFindingStatus.Raw, MaintainabilityFindingStatus.from("RAW"))

    @Test
    fun maintainabilityFindingStatus_from_willFix() = assertEquals(MaintainabilityFindingStatus.WillFix, MaintainabilityFindingStatus.from("WILL_FIX"))

    @Test
    fun maintainabilityFindingStatus_from_accepted() = assertEquals(MaintainabilityFindingStatus.Accepted, MaintainabilityFindingStatus.from("ACCEPTED"))

    @Test
    fun maintainabilityFindingStatus_from_refined_returnsRaw() {
        // REFINED is not in the enum; falls back to Raw
        assertEquals(MaintainabilityFindingStatus.Raw, MaintainabilityFindingStatus.from("REFINED"))
    }

    // snakeCaseToTitleCase

    @Test
    fun snakeCaseToTitleCase_singleWord() = assertEquals("Raw", snakeCaseToTitleCase("RAW"))

    @Test
    fun snakeCaseToTitleCase_twoWords() = assertEquals("Will Fix", snakeCaseToTitleCase("WILL_FIX"))

    @Test
    fun snakeCaseToTitleCase_threeWords() = assertEquals("False Positive Value", snakeCaseToTitleCase("FALSE_POSITIVE_VALUE"))

    @Test
    fun snakeCaseToTitleCase_alreadyLowercase() = assertEquals("Will Fix", snakeCaseToTitleCase("will_fix"))

    @Test
    fun snakeCaseToTitleCase_emptyString() = assertEquals("", snakeCaseToTitleCase(""))
}
