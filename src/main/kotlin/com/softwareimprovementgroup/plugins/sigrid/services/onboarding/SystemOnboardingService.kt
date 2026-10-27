package com.softwareimprovementgroup.plugins.sigrid.services.onboarding

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.services.SigridApiService
import com.softwareimprovementgroup.plugins.sigrid.services.SigridProjectConfiguration
import com.softwareimprovementgroup.plugins.sigrid.services.toErrorMessage
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridSettingsListener
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridSettingsTopic
import java.nio.file.Files
import java.nio.file.Path

/** Tracks whether the project's system exists in Sigrid, and uploads the repository to onboard it. */
@Service(Service.Level.PROJECT)
class SystemOnboardingService(private val project: Project) : Disposable {

    companion object {
        fun getInstance(project: Project): SystemOnboardingService = project.service()
    }

    @Volatile
    var state = OnboardingState(OnboardingStatus.CHECKING)
        private set

    @Volatile
    private var onboardingStarted = false

    init {
        val recheck = SigridSettingsListener { check() }
        project.messageBus.connect(this).subscribe(SigridSettingsTopic.PROJECT, recheck)
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(SigridSettingsTopic.GLOBAL, recheck)
    }

    /** While onboarded the state is kept, so a settings change does not flash the onboarding view. */
    fun check() {
        if (state.status != OnboardingStatus.ONBOARDED) publish(OnboardingStatus.CHECKING)
        if (!SigridProjectConfiguration.getInstance(project).isConfigurationValid) return
        ApplicationManager.getApplication().executeOnPooledThread { runCheck() }
    }

    fun onboard() {
        publish(OnboardingStatus.ONBOARDING)
        ApplicationManager.getApplication().executeOnPooledThread { runOnboarding() }
    }

    private fun runCheck() {
        try {
            val onboarded = SigridApiService.getInstance().isSystemOnboarded(project)
            publish(statusAfterCheck(onboarded, onboardingStarted))
        } catch (e: Exception) {
            publish(OnboardingStatus.ERROR, toErrorMessage(e) { SigridBundle["onboarding.error.check", it] })
        }
    }

    private fun runOnboarding() {
        try {
            uploadRepository()
            onboardingStarted = true
            publish(OnboardingStatus.ONBOARDING_STARTED)
        } catch (e: Exception) {
            publish(OnboardingStatus.ERROR, toErrorMessage(e) { SigridBundle["onboarding.error.upload", it] })
        }
    }

    private fun uploadRepository() {
        val api = SigridApiService.getInstance()
        val uploadUrl = api.requestOnboardingUpload(project, api.getLicensedCapabilities(project))
        val archive = RepositoryArchiver.createArchive(repositoryRoot())
        try {
            api.uploadArchive(uploadUrl, archive)
        } finally {
            Files.deleteIfExists(archive)
        }
    }

    private fun repositoryRoot(): Path =
        Path.of(project.basePath ?: throw IllegalStateException(SigridBundle["onboarding.error.no.root"]))

    private fun publish(status: OnboardingStatus, error: String? = null) {
        state = OnboardingState(status, error)
        project.messageBus.syncPublisher(SigridOnboardingTopic.TOPIC).stateChanged(state)
    }

    override fun dispose() = Unit
}
