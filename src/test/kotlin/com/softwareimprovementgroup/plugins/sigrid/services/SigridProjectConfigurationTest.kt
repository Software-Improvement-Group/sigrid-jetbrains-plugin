package com.softwareimprovementgroup.plugins.sigrid.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SigridProjectConfigurationTest {

    // --- effectiveApiKey ---

    @Test
    fun effectiveApiKey_noOverrides_returnsGlobalKey() {
        val key = SigridProjectConfiguration.computeEffectiveApiKey("", "", "global-key")
        assertEquals("global-key", key)
    }

    @Test
    fun effectiveApiKey_projectKeySet_returnsProjectKey() {
        val key = SigridProjectConfiguration.computeEffectiveApiKey("", "project-key", "global-key")
        assertEquals("project-key", key)
    }

    @Test
    fun effectiveApiKey_urlOverrideAndProjectKey_returnsProjectKey() {
        val key = SigridProjectConfiguration.computeEffectiveApiKey("https://custom.example", "project-key", "global-key")
        assertEquals("project-key", key)
    }

    @Test
    fun effectiveApiKey_urlOverrideWithoutProjectKey_returnsBlank() {
        val key = SigridProjectConfiguration.computeEffectiveApiKey("https://attacker.example", "", "global-key")
        assertEquals("", key)
    }

    @Test
    fun effectiveApiKey_urlOverrideWithoutProjectKey_doesNotLeakGlobalKey() {
        val globalKey = "super-secret-global-key"
        val key = SigridProjectConfiguration.computeEffectiveApiKey("https://attacker.example", "", globalKey)
        assertFalse(key.contains(globalKey), "Global API key must not be sent to a project-controlled URL override")
    }

    // --- isUrlOverrideWithoutKeyOverride ---

    @Test
    fun isUrlOverrideWithoutKeyOverride_noUrlOverride_returnsFalse() {
        assertFalse(SigridProjectConfiguration.computeIsUrlOverrideWithoutKeyOverride("", ""))
    }

    @Test
    fun isUrlOverrideWithoutKeyOverride_urlOverrideAndProjectKey_returnsFalse() {
        assertFalse(SigridProjectConfiguration.computeIsUrlOverrideWithoutKeyOverride("https://custom.example", "project-key"))
    }

    @Test
    fun isUrlOverrideWithoutKeyOverride_urlOverrideWithoutProjectKey_returnsTrue() {
        assertTrue(SigridProjectConfiguration.computeIsUrlOverrideWithoutKeyOverride("https://attacker.example", ""))
    }

    @Test
    fun isUrlOverrideWithoutKeyOverride_blankUrlOverride_returnsFalse() {
        assertFalse(SigridProjectConfiguration.computeIsUrlOverrideWithoutKeyOverride("   ", ""))
    }
}
