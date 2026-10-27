package com.softwareimprovementgroup.plugins.sigrid.services.onboarding

import com.intellij.util.messages.Topic

enum class OnboardingStatus { CHECKING, ONBOARDED, NOT_ONBOARDED, ONBOARDING, ONBOARDING_STARTED, ERROR }

data class OnboardingState(val status: OnboardingStatus, val error: String? = null)

fun interface SigridOnboardingListener {
    fun stateChanged(state: OnboardingState)
}

object SigridOnboardingTopic {
    @JvmField
    val TOPIC: Topic<SigridOnboardingListener> =
        Topic.create("sigrid.onboarding.changed", SigridOnboardingListener::class.java)
}

/**
 * A 404 right after a successful upload means provisioning is still running, not that the upload was lost,
 * so it is reported as [OnboardingStatus.ONBOARDING_STARTED] rather than [OnboardingStatus.NOT_ONBOARDED].
 */
internal fun statusAfterCheck(onboarded: Boolean, onboardingStarted: Boolean): OnboardingStatus = when {
    onboarded -> OnboardingStatus.ONBOARDED
    onboardingStarted -> OnboardingStatus.ONBOARDING_STARTED
    else -> OnboardingStatus.NOT_ONBOARDED
}
