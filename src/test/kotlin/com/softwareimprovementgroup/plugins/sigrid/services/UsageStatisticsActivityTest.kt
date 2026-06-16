package com.softwareimprovementgroup.plugins.sigrid.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UsageStatisticsActivityTest {

    @Test
    fun buildTrackingUrl_simpleCustomer_appendsCustomerName() {
        val url = buildTrackingUrl("acme")
        assertEquals("https://sigrid-says.com/usage/matomo.php?idsite=5&rec=1&ca=1&e_c=jetbrains&e_a=acme", url)
    }

    @Test
    fun buildTrackingUrl_customerWithSpaces_encodesSpaces() {
        val url = buildTrackingUrl("acme corp")
        assertTrue(url.endsWith("acme+corp") || url.endsWith("acme%20corp"),
            "Expected URL-encoded space in: $url")
    }

    @Test
    fun buildTrackingUrl_customerWithSpecialChars_encodesChars() {
        val url = buildTrackingUrl("acme&co")
        assertTrue(url.contains("%26"), "Expected encoded '&' in: $url")
    }

    @Test
    fun buildTrackingUrl_containsRequiredQueryParams() {
        val url = buildTrackingUrl("acme")
        assertTrue(url.contains("idsite=5"))
        assertTrue(url.contains("rec=1"))
        assertTrue(url.contains("ca=1"))
        assertTrue(url.contains("e_c=jetbrains"))
    }
}