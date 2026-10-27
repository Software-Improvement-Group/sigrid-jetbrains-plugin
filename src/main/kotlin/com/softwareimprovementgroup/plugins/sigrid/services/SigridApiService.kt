package com.softwareimprovementgroup.plugins.sigrid.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.models.*
import java.lang.reflect.Type
import java.net.ProxySelector
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration

@Service(Service.Level.APP)
class SigridApiService {
    companion object {
        fun getInstance(): SigridApiService = service()

        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(30)
        private val UPLOAD_TIMEOUT: Duration = Duration.ofMinutes(10)
        private val ONBOARDING_CAPABILITIES = listOf("MAINTAINABILITY", "SECURITY", "OPEN_SOURCE_HEALTH")

        internal fun requireHttpsUrl(url: String) {
            val scheme = try { URI.create(url).scheme } catch (_: Exception) { null }
            if (scheme != "https") throw IllegalArgumentException("Sigrid URL must use HTTPS (got: $url)")
        }

        /** 2xx means the system exists, 404 means it does not; anything else is an error. */
        internal fun isOnboardedStatus(statusCode: Int, uri: Any?): Boolean = when (statusCode) {
            in 200..299 -> true
            404 -> false
            else -> throw Exception("HTTP $statusCode from $uri")
        }

        internal fun licensedCapabilities(licenses: List<String>?): List<String> =
            licenses.orEmpty().filter { it in ONBOARDING_CAPABILITIES }
    }

    private val gson = Gson()
    private val nullSerializingGson = GsonBuilder().serializeNulls().create()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .proxy(ProxySelector.getDefault())
        .connectTimeout(CONNECT_TIMEOUT)
        .build()

    private fun buildRequest(url: String, projectConfig: SigridProjectConfiguration): HttpRequest.Builder {
        requireHttpsUrl(url)
        val apiKey = projectConfig.effectiveApiKey
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
    }

    private fun checkStatusCode(response: HttpResponse<String>) {
        if (response.statusCode() !in 200..299) {
            throw Exception("HTTP ${response.statusCode()} from ${response.uri()}")
        }
    }

    private fun checkStatus(response: HttpResponse<String>) {
        checkStatusCode(response)
        if (response.body().isNullOrBlank() || response.body() == "null") {
            throw Exception("HTTP 404 from ${response.uri()}")
        }
    }

    internal fun joinUrl(base: String, vararg paths: String): String {
        val normalizedBase = base.trimEnd('/')
        val path = paths.joinToString("/") { URLEncoder.encode(it.trim('/'), "UTF-8").replace("+", "%20") }
        return "$normalizedBase/$path"
    }

    private fun <T> getJson(project: Project, type: Type, vararg path: String): T {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, path.first(), projectConfig.effectiveCustomer, projectConfig.system, *path.drop(1).toTypedArray())
        val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
        checkStatus(response)
        return gson.fromJson(response.body(), type)
    }

    fun getOpenSourceHealthFindings(project: Project): OpenSourceHealthResponse =
        getJson(project, OpenSourceHealthResponse::class.java, "osh-findings")

    fun getSecurityFindings(project: Project): List<SecurityFindingResponse> =
        getJson(project, object : TypeToken<List<SecurityFindingResponse>>() {}.type, "security-findings")

    fun getArchitectureGraph(project: Project): ArchitectureGraphResponse =
        getJson(project, ArchitectureGraphResponse::class.java, "architecture-quality", "raw")

    fun getRefactoringCandidates(project: Project, category: RefactoringCategory): RefactoringCandidatesResponse =
        getJson(project, RefactoringCandidatesResponse::class.java, "refactoring-candidates", category.value)

    fun getAllRefactoringCandidates(project: Project): Map<RefactoringCategory, RefactoringCandidatesResponse> {
        return RefactoringCategory.entries.associateWith { getRefactoringCandidates(project, it) }
    }

    fun editFinding(project: Project, findingId: String, findingRequest: FindingRequest) {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "findings", projectConfig.effectiveCustomer, projectConfig.system, findingId)
        val body = gson.toJson(findingRequest)
        val request = buildRequest(url, projectConfig)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json")
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        checkStatusCode(response)
    }

    private fun onboardingUrl(projectConfig: SigridProjectConfiguration, vararg path: String): String =
        joinUrl(projectConfig.effectiveSigridUrl, *path)

    fun isSystemOnboarded(project: Project): Boolean {
        val config = SigridProjectConfiguration.getInstance(project)
        val url = onboardingUrl(config, "rest", "analysis-results", "sigridci", config.effectiveCustomer, config.system, "v1", "ci")
        val response = httpClient.send(buildRequest(url, config).GET().build(), HttpResponse.BodyHandlers.ofString())
        return isOnboardedStatus(response.statusCode(), response.uri())
    }

    fun getLicensedCapabilities(project: Project): List<String> {
        val config = SigridProjectConfiguration.getInstance(project)
        val url = joinUrl(config.effectiveSigridApiBaseUrl, "licenses", config.effectiveCustomer)
        val response = httpClient.send(buildRequest(url, config).GET().build(), HttpResponse.BodyHandlers.ofString())
        checkStatusCode(response)
        return licensedCapabilities(gson.fromJson(response.body(), LicensesResponse::class.java)?.licenses)
    }

    /** Asks Sigrid for a presigned URL to which the repository archive is uploaded. */
    fun requestOnboardingUpload(project: Project, capabilities: List<String>): String {
        val config = SigridProjectConfiguration.getInstance(project)
        val url = onboardingUrl(config, "rest", "inboundresults", "sig", config.effectiveCustomer, config.system, "ci", "uploads", "v1")
        val body = nullSerializingGson.toJson(OnboardingRequest(capabilities = capabilities, subsystem = config.subsystem.ifBlank { null }))
        val request = buildRequest(url, config)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json")
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        checkStatusCode(response)
        return gson.fromJson(response.body(), OnboardingUploadResponse::class.java)?.uploadUrl
            ?: throw Exception("Sigrid did not return an upload URL")
    }

    /** The upload URL is presigned, so the Sigrid API key must not be sent along with it. */
    fun uploadArchive(uploadUrl: String, archive: Path) {
        requireHttpsUrl(uploadUrl)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uploadUrl))
            .timeout(UPLOAD_TIMEOUT)
            .header("Content-Type", "application/zip")
            .header("x-amz-server-side-encryption", "AES256")
            .PUT(HttpRequest.BodyPublishers.ofFile(archive))
            .build()
        checkStatusCode(httpClient.send(request, HttpResponse.BodyHandlers.ofString()))
    }
}
