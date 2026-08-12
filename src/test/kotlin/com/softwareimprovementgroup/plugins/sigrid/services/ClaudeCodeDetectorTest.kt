package com.softwareimprovementgroup.plugins.sigrid.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ClaudeCodeDetectorTest {

    @Test
    fun detect_pathNotFound_returnsFalse() {
        val detector = ClaudeCodeDetector()
        assertFalse(detector.detect(resolvePath = { null }, runVersionCheck = { true }))
    }

    @Test
    fun detect_pathFoundAndVersionCheckSucceeds_returnsTrue() {
        val detector = ClaudeCodeDetector()
        assertTrue(detector.detect(resolvePath = { "/usr/local/bin/claude" }, runVersionCheck = { true }))
    }

    @Test
    fun detect_pathFoundButVersionCheckFails_returnsFalse() {
        val detector = ClaudeCodeDetector()
        assertFalse(detector.detect(resolvePath = { "/usr/local/bin/claude" }, runVersionCheck = { false }))
    }

    @Test
    fun detect_versionCheckThrows_returnsFalse() {
        val detector = ClaudeCodeDetector()
        assertFalse(detector.detect(resolvePath = { "/usr/local/bin/claude" }, runVersionCheck = { throw RuntimeException("boom") }))
    }

    @Test
    fun isAvailableCached_beforeDetection_returnsNull() {
        assertNull(ClaudeCodeDetector().isAvailableCached())
    }

    @Test
    fun resolvedPathCached_beforeDetection_returnsNull() {
        assertNull(ClaudeCodeDetector().resolvedPathCached())
    }

    @Test
    fun detect_pathFound_cachesResolvedPath() {
        val detector = ClaudeCodeDetector()
        detector.detect(resolvePath = { "/opt/homebrew/bin/claude" }, runVersionCheck = { true })
        assertEquals("/opt/homebrew/bin/claude", detector.resolvedPathCached())
    }

    @Test
    fun detect_pathNotFound_leavesResolvedPathCacheUntouched() {
        val detector = ClaudeCodeDetector()
        detector.detect(resolvePath = { null }, runVersionCheck = { true })
        assertNull(detector.resolvedPathCached())
    }

    @Test
    fun findInWellKnownDirectories_returnsHomebrewPathWhenPresent() {
        val found = findInWellKnownDirectories(home = "/Users/tester") { it.path == "/opt/homebrew/bin/claude" }
        assertEquals("/opt/homebrew/bin/claude", found)
    }

    @Test
    fun findInWellKnownDirectories_probesUserLocalBinUnderHome() {
        val found = findInWellKnownDirectories(home = "/Users/tester") { it == File("/Users/tester", ".local/bin/claude") }
        assertEquals(File("/Users/tester", ".local/bin/claude").absolutePath, found)
    }

    @Test
    fun findInWellKnownDirectories_returnsNullWhenNowhere() {
        assertNull(findInWellKnownDirectories(home = "/Users/tester") { false })
    }
}
