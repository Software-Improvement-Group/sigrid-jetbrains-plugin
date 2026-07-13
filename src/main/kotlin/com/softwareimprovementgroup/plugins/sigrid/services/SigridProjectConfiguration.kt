package com.softwareimprovementgroup.plugins.sigrid.services

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

        internal fun computeEffectiveApiKey(urlOverride: String, projectApiKey: String, globalApiKey: String): String =
            if (urlOverride.isNotBlank()) projectApiKey
            else projectApiKey.ifBlank { globalApiKey }

        internal fun computeIsUrlOverrideWithoutKeyOverride(urlOverride: String, projectApiKey: String): Boolean =
            urlOverride.isNotBlank() && projectApiKey.isBlank()
    }

    data class State(
        var system: String = "",
        var subsystem: String = "",
        var customerOverride: String = "",
        var sigridUrlOverride: String = "",
        var jiraBaseUrl: String = "",
        var jiraUser: String = "",
        var jiraProjectKey: String = "",
        var azureDevOpsProjectName: String = "",
        var azureDevOpsOrganizationUrlOverride: String = "",
        var azureDevOpsLastWorkItemType: String = "",
        var lastIssueCreationAction: String = "",
    )

    private var _state = State()
    private val apiKeyOverrideCredential by lazy {
        PasswordSafeCredential("com.softwareimprovementgroup.plugins.sigrid/apiKey/${project.locationHash}")
    }
    private val jiraTokenCredential by lazy {
        PasswordSafeCredential("com.softwareimprovementgroup.plugins.sigrid/jiraToken/${project.locationHash}")
    }
    private val azureDevOpsPatOverrideCredential by lazy {
        PasswordSafeCredential("com.softwareimprovementgroup.plugins.sigrid/azureDevOpsPatOverride/${project.locationHash}")
    }

    init {
        apiKeyOverrideCredential.loadAsync()
        jiraTokenCredential.loadAsync()
        azureDevOpsPatOverrideCredential.loadAsync()
    }

    override fun getState(): State = _state

    override fun loadState(state: State) {
        _state = state
        apiKeyOverrideCredential.loadAsync()
        jiraTokenCredential.loadAsync()
        azureDevOpsPatOverrideCredential.loadAsync()
    }

    var apiKeyOverride: String
        get() = apiKeyOverrideCredential.get()
        set(value) = apiKeyOverrideCredential.set(value)

    val effectiveApiKey: String
        get() = computeEffectiveApiKey(_state.sigridUrlOverride, apiKeyOverrideCredential.get(), SigridConfiguration.getInstance().apiKey)

    val isUrlOverrideWithoutKeyOverride: Boolean
        get() = computeIsUrlOverrideWithoutKeyOverride(_state.sigridUrlOverride, apiKeyOverrideCredential.get())

    val effectiveCustomer: String
        get() = _state.customerOverride.ifBlank { SigridConfiguration.getInstance().customer }

    val effectiveSigridUrl: String
        get() = _state.sigridUrlOverride.trimEnd('/').ifBlank { SigridConfiguration.getInstance().sigridUrl }

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
                jiraTokenCredential.get().isNotBlank() && _state.jiraProjectKey.isNotBlank()

    val effectiveAzureDevOpsOrganizationUrl: String
        get() = _state.azureDevOpsOrganizationUrlOverride.ifBlank { SigridConfiguration.getInstance().azureDevOpsOrganizationUrl }

    val effectiveAzureDevOpsPat: String
        get() = computeEffectiveApiKey(
            _state.azureDevOpsOrganizationUrlOverride,
            azureDevOpsPatOverrideCredential.get(),
            SigridConfiguration.getInstance().azureDevOpsPat,
        )

    val isAzureDevOpsUrlOverrideWithoutPatOverride: Boolean
        get() = computeIsUrlOverrideWithoutKeyOverride(
            _state.azureDevOpsOrganizationUrlOverride,
            azureDevOpsPatOverrideCredential.get(),
        )

    val isAzureDevOpsConfigured: Boolean
        get() = effectiveAzureDevOpsOrganizationUrl.isNotBlank() &&
                effectiveAzureDevOpsPat.isNotBlank() &&
                _state.azureDevOpsProjectName.isNotBlank()

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
        get() = jiraTokenCredential.get()
        set(value) = jiraTokenCredential.set(value)

    var jiraProjectKey: String
        get() = _state.jiraProjectKey
        set(value) { _state.jiraProjectKey = value }

    var azureDevOpsProjectName: String
        get() = _state.azureDevOpsProjectName
        set(value) { _state.azureDevOpsProjectName = value }

    var azureDevOpsOrganizationUrlOverride: String
        get() = _state.azureDevOpsOrganizationUrlOverride
        set(value) { _state.azureDevOpsOrganizationUrlOverride = value }

    var azureDevOpsPatOverride: String
        get() = azureDevOpsPatOverrideCredential.get()
        set(value) = azureDevOpsPatOverrideCredential.set(value)

    var azureDevOpsLastWorkItemType: String
        get() = _state.azureDevOpsLastWorkItemType
        set(value) { _state.azureDevOpsLastWorkItemType = value }

    var lastIssueCreationAction: String
        get() = _state.lastIssueCreationAction
        set(value) { _state.lastIssueCreationAction = value }
}
