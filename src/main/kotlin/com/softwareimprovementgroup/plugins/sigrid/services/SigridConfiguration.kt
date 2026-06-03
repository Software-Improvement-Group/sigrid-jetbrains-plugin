package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.openapi.components.*

@State(
    name = "SigridConfiguration",
    storages = [Storage("SigridConfiguration.xml")]
)
@Service(Service.Level.APP)
class SigridConfiguration : PersistentStateComponent<SigridConfiguration.State> {
    companion object {
        const val SIGRID_DEFAULT_URL = "https://sigrid-says.com"
        const val SIGRID_API_BASE_PATH = "/rest/analysis-results/api/v1"

        fun getInstance(): SigridConfiguration = service()
    }

    data class State(
        var sigridUrl: String = SIGRID_DEFAULT_URL,
        var customer: String = "",
        var jiraUser: String = "",
    )

    private var _state = State()
    private val apiKeyCredential = PasswordSafeCredential("com.softwareimprovementgroup.plugins.sigrid/apiKey")
    private val jiraTokenCredential = PasswordSafeCredential("com.softwareimprovementgroup.plugins.sigrid/jiraToken")

    init {
        apiKeyCredential.loadAsync()
        jiraTokenCredential.loadAsync()
    }

    override fun getState(): State = _state

    override fun loadState(state: State) {
        _state = state
        apiKeyCredential.loadAsync()
        jiraTokenCredential.loadAsync()
    }

    var sigridUrl: String
        get() = _state.sigridUrl
        set(value) { _state.sigridUrl = value }

    var customer: String
        get() = _state.customer
        set(value) { _state.customer = value }

    var apiKey: String
        get() = apiKeyCredential.get()
        set(value) = apiKeyCredential.set(value)

    var jiraUser: String
        get() = _state.jiraUser
        set(value) { _state.jiraUser = value }

    var jiraToken: String
        get() = jiraTokenCredential.get()
        set(value) = jiraTokenCredential.set(value)

    fun getSigridApiBaseUrl(): String {
        val base = _state.sigridUrl.trimEnd('/').ifBlank { SIGRID_DEFAULT_URL }
        return "$base$SIGRID_API_BASE_PATH"
    }
}