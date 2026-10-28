package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridSettingsListener
import com.softwareimprovementgroup.plugins.sigrid.settings.SigridSettingsTopic

// Eagerly warms SigridApiService's Maintainability-rating cache on project open, and again whenever Sigrid
// settings change (global or project-level - either can change the effective customer/system/URL). Eager
// rather than lazy-on-first-access specifically to avoid a race where the Prioritized tab's first refresh
// reads ObjectivesGate's market-benchmark fallback (see ObjectivesGate.kt) before the rating has ever been
// fetched. getMaintainabilityRating() still falls back to a lazy fetch-and-cache on a genuine cache miss,
// so nothing breaks if this warmup hasn't completed yet - it just avoids the common case needing to wait.
//
// Open Source Health's equivalent rating needs no warmup at all - it rides along with the OSH findings
// this plugin already fetches every refresh (see OpenSourceHealthMapper.systemRating).
class MaintainabilityRatingWarmupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        warmUp(project)
        subscribeToSettingsChanges(project)
    }

    private fun subscribeToSettingsChanges(project: Project) {
        val listener = SigridSettingsListener {
            SigridApiService.getInstance().invalidateMaintainabilityRatingCache()
            warmUp(project)
        }
        project.messageBus.connect(project).subscribe(SigridSettingsTopic.PROJECT, listener)
        ApplicationManager.getApplication().messageBus.connect(project).subscribe(SigridSettingsTopic.GLOBAL, listener)
    }

    private fun warmUp(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            // Same guard UsageStatisticsActivity uses: without this, a project that hasn't configured
            // Sigrid yet (or has a blank system, e.g. right after a settings change clears it) fires a
            // request with an empty {system} path segment - that's a malformed URL, not a permissions
            // issue, even though the server happens to answer it with 403 rather than 404.
            if (!SigridProjectConfiguration.getInstance(project).isConfigurationValid) return@executeOnPooledThread
            SigridApiService.getInstance().getMaintainabilityRating(project)
        }
    }
}
