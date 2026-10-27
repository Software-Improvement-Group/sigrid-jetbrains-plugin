package com.softwareimprovementgroup.plugins.sigrid.services

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SigridApiServiceTest {

    private val service = SigridApiService()

    @Test
    fun requireHttpsUrl_httpsUrl_doesNotThrow() {
        assertDoesNotThrow { SigridApiService.requireHttpsUrl("https://sigrid-says.com") }
    }

    @Test
    fun requireHttpsUrl_httpsUrlWithPath_doesNotThrow() {
        assertDoesNotThrow { SigridApiService.requireHttpsUrl("https://custom.example.com/api") }
    }

    @Test
    fun requireHttpsUrl_httpUrl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SigridApiService.requireHttpsUrl("http://sigrid-says.com")
        }
    }

    @Test
    fun requireHttpsUrl_ftpUrl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SigridApiService.requireHttpsUrl("ftp://example.com")
        }
    }

    @Test
    fun requireHttpsUrl_malformedUrl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SigridApiService.requireHttpsUrl("not-a-url")
        }
    }

    @Test
    fun joinUrl_plainSegments_joinsWithSlashes() {
        assertEquals(
            "https://example.com/api/findings/mycustomer/mysystem",
            service.joinUrl("https://example.com/api/", "findings", "mycustomer", "mysystem")
        )
    }

    @Test
    fun joinUrl_spaceInSegment_encodedAsPercent20() {
        assertEquals(
            "https://example.com/api/findings/my%20customer/my%20system",
            service.joinUrl("https://example.com/api/", "findings", "my customer", "my system")
        )
    }

    @Test
    fun joinUrl_slashInSegment_encodedAsPercent2F() {
        assertEquals(
            "https://example.com/api/findings/cust%2Fomer/sys",
            service.joinUrl("https://example.com/api/", "findings", "cust/omer", "sys")
        )
    }

    @Test
    fun joinUrl_hashInSegment_encoded() {
        assertEquals(
            "https://example.com/api/findings/cust%23omer/sys",
            service.joinUrl("https://example.com/api/", "findings", "cust#omer", "sys")
        )
    }

    @Test
    fun joinUrl_questionMarkInSegment_encoded() {
        assertEquals(
            "https://example.com/api/findings/cust%3Fomer/sys",
            service.joinUrl("https://example.com/api/", "findings", "cust?omer", "sys")
        )
    }

    @Test
    fun joinUrl_dotDotInSegment_encodedToPreventTraversal() {
        assertEquals(
            "https://example.com/api/findings/..%2Fother/sys",
            service.joinUrl("https://example.com/api/", "findings", "../other", "sys")
        )
    }

    @Test
    fun isOnboardedStatus_noContent_isOnboarded() =
        assertEquals(true, SigridApiService.isOnboardedStatus(204, null))

    @Test
    fun isOnboardedStatus_notFound_isNotOnboarded() =
        assertEquals(false, SigridApiService.isOnboardedStatus(404, null))

    @Test
    fun isOnboardedStatus_serverError_throwsWithStatus() {
        val e = assertThrows(Exception::class.java) { SigridApiService.isOnboardedStatus(500, "uri") }
        assertEquals("HTTP 500 from uri", e.message)
    }

    @Test
    fun licensedCapabilities_dropsUnknownLicenses() =
        assertEquals(
            listOf("MAINTAINABILITY", "SECURITY"),
            SigridApiService.licensedCapabilities(listOf("MAINTAINABILITY", "SOME_UNKNOWN", "SECURITY")),
        )

    @Test
    fun licensedCapabilities_null_isEmpty() =
        assertTrue(SigridApiService.licensedCapabilities(null).isEmpty())
}
