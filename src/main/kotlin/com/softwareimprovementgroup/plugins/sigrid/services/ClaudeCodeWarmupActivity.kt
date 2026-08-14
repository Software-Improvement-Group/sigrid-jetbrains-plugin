package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ClaudeCodeWarmupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ClaudeCodeDetector.getInstance().warmUpAsync()
    }
}
