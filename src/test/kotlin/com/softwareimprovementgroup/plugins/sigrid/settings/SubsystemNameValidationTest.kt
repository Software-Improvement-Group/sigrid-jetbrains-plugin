package com.softwareimprovementgroup.plugins.sigrid.settings

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubsystemNameValidationTest {

    private fun valid(value: String) = value.matches(SUBSYSTEM_NAME_REGEX)

    @Test
    fun subsystemName_twoLetters_valid() {
        assertTrue(valid("ab"))
    }

    @Test
    fun subsystemName_letterAndDigit_valid() {
        assertTrue(valid("a1"))
    }

    @Test
    fun subsystemName_plainName_valid() {
        assertTrue(valid("backend"))
    }

    @Test
    fun subsystemName_mixedCase_valid() {
        assertTrue(valid("BackEnd"))
    }

    @Test
    fun subsystemName_withDot_valid() {
        assertTrue(valid("my.module"))
    }

    @Test
    fun subsystemName_withUnderscore_valid() {
        assertTrue(valid("my_module"))
    }

    @Test
    fun subsystemName_withHyphen_valid() {
        assertTrue(valid("my-module"))
    }

    @Test
    fun subsystemName_withForwardSlash_valid() {
        assertTrue(valid("src/backend"))
    }

    @Test
    fun subsystemName_deepPath_valid() {
        assertTrue(valid("src/main/java"))
    }

    @Test
    fun subsystemName_leadingDot_invalid() {
        assertFalse(valid(".hidden"))
    }

    @Test
    fun subsystemName_trailingDot_invalid() {
        assertFalse(valid("module."))
    }

    @Test
    fun subsystemName_leadingHyphen_invalid() {
        assertFalse(valid("-module"))
    }

    @Test
    fun subsystemName_trailingHyphen_invalid() {
        assertFalse(valid("module-"))
    }

    @Test
    fun subsystemName_leadingSlash_invalid() {
        assertFalse(valid("/src"))
    }

    @Test
    fun subsystemName_trailingSlash_invalid() {
        assertFalse(valid("src/"))
    }

    @Test
    fun subsystemName_space_invalid() {
        assertFalse(valid("my module"))
    }
}