package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(
    name = "SigridProjectConfiguration",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class SigridProjectConfiguration(private val project: Project) : PersistentStateComponent<SigridProjectConfiguration.State> {
    companion object {
        fun getInstance(project: Project): SigridProjectConfiguration = project.service()
    }

    data class State(
        var system: String = "",
        var subsystem: String = "",
        var customerOverride: String = "",
        var sigridUrlOverride: String = "",
        var jiraBaseUrl: String = "",
        var jiraUser: String = "",
        var jiraToken: String = "",
        var jiraProjectKey: String = "",
    )

    private var _state = State()

    override fun getState(): State = _state

    override fun loadState(state: State) {
        _state = state
    }

    // Per-project API key override stored in the OS credential store under a project-scoped key
    var apiKeyOverride: String
        get() {
            val attributes = CredentialAttributes(apiKeyCredentialKey())
            return PasswordSafe.instance.get(attributes)?.getPasswordAsString() ?: ""
        }
        set(value) {
            val attributes = CredentialAttributes(apiKeyCredentialKey())
            PasswordSafe.instance.set(attributes, if (value.isBlank()) null else Credentials(null, value))
        }

    private fun apiKeyCredentialKey() =
        "com.softwareimprovementgroup.plugins.sigrid/apiKey/${project.locationHash}"

    // Effective values — project override wins when set, otherwise falls back to global
    val effectiveApiKey: String
        get() = apiKeyOverride.ifBlank { SigridConfiguration.getInstance().apiKey }

    val effectiveCustomer: String
        get() = _state.customerOverride.ifBlank { SigridConfiguration.getInstance().customer }

    val effectiveSigridApiBaseUrl: String
        get() {
            val overrideUrl = _state.sigridUrlOverride.trimEnd('/').ifBlank { null }
            return if (overrideUrl != null)
                "$overrideUrl${SigridConfiguration.SIGRID_API_BASE_PATH}"
            else
                SigridConfiguration.getInstance().getSigridApiBaseUrl()
        }

    val isConfigurationValid: Boolean
        get() = effectiveApiKey.isNotBlank() && effectiveCustomer.isNotBlank() && _state.system.isNotBlank()

    val isJiraConfigured: Boolean
        get() = _state.jiraBaseUrl.isNotBlank() && _state.jiraUser.isNotBlank() &&
                _state.jiraToken.isNotBlank() && _state.jiraProjectKey.isNotBlank()

    var system: String
        get() = _state.system
        set(value) { _state.system = value }

    var subsystem: String
        get() = _state.subsystem.trim()
        set(value) { _state.subsystem = value }

    var customerOverride: String
        get() = _state.customerOverride
        set(value) { _state.customerOverride = value }

    var sigridUrlOverride: String
        get() = _state.sigridUrlOverride
        set(value) { _state.sigridUrlOverride = value }

    var jiraBaseUrl: String
        get() = _state.jiraBaseUrl
        set(value) { _state.jiraBaseUrl = value }

    var jiraUser: String
        get() = _state.jiraUser
        set(value) { _state.jiraUser = value }

    var jiraToken: String
        get() = _state.jiraToken
        set(value) { _state.jiraToken = value }

    var jiraProjectKey: String
        get() = _state.jiraProjectKey
        set(value) { _state.jiraProjectKey = value }
}