package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
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
        private const val PASSWORD_SAFE_KEY = "com.softwareimprovementgroup.plugins.sigrid/apiKey"

        fun getInstance(): SigridConfiguration = service()
    }

    data class State(
        var sigridUrl: String = SIGRID_DEFAULT_URL,
        var customer: String = "",
    )

    private var _state = State()

    override fun getState(): State = _state

    override fun loadState(state: State) {
        _state = state
    }

    var sigridUrl: String
        get() = _state.sigridUrl
        set(value) { _state.sigridUrl = value }

    var customer: String
        get() = _state.customer
        set(value) { _state.customer = value }

    // Stored in the OS credential store, never written to disk in plain text
    var apiKey: String
        get() {
            val attributes = CredentialAttributes(PASSWORD_SAFE_KEY)
            return PasswordSafe.instance.get(attributes)?.getPasswordAsString() ?: ""
        }
        set(value) {
            val attributes = CredentialAttributes(PASSWORD_SAFE_KEY)
            PasswordSafe.instance.set(attributes, if (value.isBlank()) null else Credentials(null, value))
        }

    fun getSigridApiBaseUrl(): String {
        val base = _state.sigridUrl.trimEnd('/').ifBlank { SIGRID_DEFAULT_URL }
        return "$base$SIGRID_API_BASE_PATH"
    }
}