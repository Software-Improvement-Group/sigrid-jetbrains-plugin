package com.softwareimprovementgroup.plugins.sigrid.services.aiAgents

import com.intellij.util.messages.Topic

fun interface AiAgentAvailabilityListener {
    fun availabilityChanged()
}

object AiAgentAvailabilityTopic {
    @JvmField
    val TOPIC: Topic<AiAgentAvailabilityListener> =
        Topic.create("sigrid.aiagent.availability.changed", AiAgentAvailabilityListener::class.java)
}
