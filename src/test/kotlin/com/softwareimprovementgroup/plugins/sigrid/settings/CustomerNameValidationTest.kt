package com.softwareimprovementgroup.plugins.sigrid.settings

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomerNameValidationTest {

    private fun valid(value: String) = value.matches(CUSTOMER_NAME_REGEX)

    @Test
    fun customerName_lowercaseLetters_valid() {
        assertTrue(valid("acme"))
    }

    @Test
    fun customerName_digits_valid() {
        assertTrue(valid("12"))
    }

    @Test
    fun customerName_mixedLettersAndDigits_valid() {
        assertTrue(valid("acme123"))
    }

    @Test
    fun customerName_exactly65Chars_valid() {
        assertTrue(valid("a".repeat(65)))
    }

    @Test
    fun customerName_singleChar_invalid() {
        assertFalse(valid("a"))
    }

    @Test
    fun customerName_66Chars_invalid() {
        assertFalse(valid("a".repeat(66)))
    }

    @Test
    fun customerName_uppercase_invalid() {
        assertFalse(valid("Acme"))
    }

    @Test
    fun customerName_hyphen_invalid() {
        assertFalse(valid("my-company"))
    }

    @Test
    fun customerName_space_invalid() {
        assertFalse(valid("my company"))
    }

    @Test
    fun customerName_underscore_invalid() {
        assertFalse(valid("my_company"))
    }

    @Test
    fun customerName_dot_invalid() {
        assertFalse(valid("my.company"))
    }
}