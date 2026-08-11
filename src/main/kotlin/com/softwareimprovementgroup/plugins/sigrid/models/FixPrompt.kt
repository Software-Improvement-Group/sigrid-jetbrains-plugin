package com.softwareimprovementgroup.plugins.sigrid.models

/** The prompt handed to an AI agent. */
data class FixPrompt(
    /** The opening instruction on its own, for adapters that cannot pass the full text verbatim. */
    val lead: String,
    /** The complete prompt, including the lead. */
    val text: String,
)

/** The Sigrid system context woven into the prompt so the agent knows what it is querying. */
data class FixPromptContext(
    val customer: String,
    val system: String,
)
