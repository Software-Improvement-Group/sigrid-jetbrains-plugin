package com.softwareimprovementgroup.plugins.sigrid.settings

import com.intellij.util.messages.Topic

fun interface SigridSettingsListener {
    fun settingsChanged()
}

object SigridSettingsTopic {
    @JvmField
    val GLOBAL: Topic<SigridSettingsListener> =
        Topic.create("sigrid.settings.changed", SigridSettingsListener::class.java)

    @JvmField
    val PROJECT: Topic<SigridSettingsListener> =
        Topic.create("sigrid.project.settings.changed", SigridSettingsListener::class.java)
}