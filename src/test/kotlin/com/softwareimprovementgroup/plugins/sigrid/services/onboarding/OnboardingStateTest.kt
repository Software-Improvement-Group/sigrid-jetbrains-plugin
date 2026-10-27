package com.softwareimprovementgroup.plugins.sigrid.services.onboarding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnboardingStateTest {

    @Test
    fun statusAfterCheck_onboarded_isOnboarded() =
        assertEquals(OnboardingStatus.ONBOARDED, statusAfterCheck(onboarded = true, onboardingStarted = true))

    @Test
    fun statusAfterCheck_missingAfterUpload_isOnboardingStarted() =
        assertEquals(OnboardingStatus.ONBOARDING_STARTED, statusAfterCheck(onboarded = false, onboardingStarted = true))

    @Test
    fun statusAfterCheck_missingWithoutUpload_isNotOnboarded() =
        assertEquals(OnboardingStatus.NOT_ONBOARDED, statusAfterCheck(onboarded = false, onboardingStarted = false))
}
