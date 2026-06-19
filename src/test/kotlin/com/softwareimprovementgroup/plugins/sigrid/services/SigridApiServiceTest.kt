package com.softwareimprovementgroup.plugins.sigrid.services

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SigridApiServiceTest {

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
}
