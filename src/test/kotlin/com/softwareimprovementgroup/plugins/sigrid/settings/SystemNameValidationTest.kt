package com.softwareimprovementgroup.plugins.sigrid.settings

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SystemNameValidationTest {

    private fun valid(value: String) = value.matches(SYSTEM_NAME_REGEX)

    @Test
    fun systemName_lowercaseLetters_valid() {
        assertTrue(valid("acme"))
    }

    @Test
    fun systemName_digits_valid() {
        assertTrue(valid("12"))
    }

    @Test
    fun systemName_mixedLettersAndDigits_valid() {
        assertTrue(valid("acme123"))
    }

    @Test
    fun systemName_uppercaseLetters_valid() {
        assertTrue(valid("Acme"))
    }

    @Test
    fun systemName_hyphenSeparated_valid() {
        assertTrue(valid("my-system"))
    }

    @Test
    fun systemName_multipleHyphenSeparatedSegments_valid() {
        assertTrue(valid("my-big-system"))
    }

    @Test
    fun systemName_exactly65Chars_valid() {
        assertTrue(valid("a".repeat(65)))
    }

    @Test
    fun systemName_singleChar_invalid() {
        assertFalse(valid("a"))
    }

    @Test
    fun systemName_66Chars_invalid() {
        assertFalse(valid("a".repeat(66)))
    }

    @Test
    fun systemName_leadingHyphen_invalid() {
        assertFalse(valid("-system"))
    }

    @Test
    fun systemName_trailingHyphen_invalid() {
        assertFalse(valid("system-"))
    }

    @Test
    fun systemName_consecutiveHyphens_invalid() {
        assertFalse(valid("my--system"))
    }

    @Test
    fun systemName_space_invalid() {
        assertFalse(valid("my system"))
    }

    @Test
    fun systemName_underscore_invalid() {
        assertFalse(valid("my_system"))
    }

    @Test
    fun systemName_dot_invalid() {
        assertFalse(valid("my.system"))
    }
}