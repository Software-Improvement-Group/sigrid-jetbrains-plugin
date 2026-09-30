package com.softwareimprovementgroup.plugins.sigrid.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.models.*
import java.net.ProxySelector
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service(Service.Level.APP)
class SigridApiService {
    companion object {
        fun getInstance(): SigridApiService = service()

        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(30)

        internal fun requireHttpsUrl(url: String) {
            val scheme = try { URI.create(url).scheme } catch (_: Exception) { null }
            if (scheme != "https") throw IllegalArgumentException("Sigrid URL must use HTTPS (got: $url)")
        }
    }

    private val gson = Gson()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .proxy(ProxySelector.getDefault())
        .connectTimeout(CONNECT_TIMEOUT)
        .build()

    // Several tabs (Maintainability/Security/OSH, and the merged Prioritized tab) fetch the same
    // data per refresh cycle; this cache avoids re-issuing identical HTTP requests for each tab.
    // Cleared via invalidateCache(), called by the manual refresh action.
    private val responseCache = java.util.concurrent.ConcurrentHashMap<String, Any>()

    // Maintainability's overall system rating (0.5-5.5 stars) only changes when Sigrid re-analyzes a
    // system, not between manual refreshes - unlike responseCache, this is intentionally NOT cleared by
    // invalidateCache(), and is kept warm by MaintainabilityRatingWarmupActivity instead of being
    // re-fetched on every refresh. ConcurrentHashMap can't store null values directly, hence the
    // CachedRating wrapper - a system genuinely having no rating is still a valid, cacheable result.
    private data class CachedRating(val value: Double?)
    private val maintainabilityRatingCache = java.util.concurrent.ConcurrentHashMap<String, CachedRating>()

    fun invalidateCache() = responseCache.clear()

    fun invalidateMaintainabilityRatingCache() = maintainabilityRatingCache.clear()

    private fun <T : Any> cached(key: String, load: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return responseCache.computeIfAbsent(key) { load() } as T
    }

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

    internal fun withDateRangeQuery(url: String, startDate: String, endDate: String): String =
        "$url?startDate=${URLEncoder.encode(startDate, "UTF-8")}&endDate=${URLEncoder.encode(endDate, "UTF-8")}"

    fun getOpenSourceHealthFindings(project: Project): OpenSourceHealthResponse {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val cacheKey = "osh-findings:${projectConfig.effectiveCustomer}:${projectConfig.system}"
        return cached(cacheKey) {
            val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "osh-findings", projectConfig.effectiveCustomer, projectConfig.system)
            val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
            checkStatus(response)
            gson.fromJson(response.body(), OpenSourceHealthResponse::class.java)
        }
    }

    fun getSecurityFindings(project: Project): List<SecurityFindingResponse> {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val cacheKey = "security-findings:${projectConfig.effectiveCustomer}:${projectConfig.system}"
        return cached(cacheKey) {
            val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "security-findings", projectConfig.effectiveCustomer, projectConfig.system)
            val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
            checkStatus(response)
            val type = object : TypeToken<List<SecurityFindingResponse>>() {}.type
            gson.fromJson<List<SecurityFindingResponse>>(response.body(), type)
        }
    }

    fun getReliabilityFindings(project: Project): List<SecurityFindingResponse> {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val cacheKey = "reliability-findings:${projectConfig.effectiveCustomer}:${projectConfig.system}"
        return cached(cacheKey) {
            val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "reliability-findings", projectConfig.effectiveCustomer, projectConfig.system)
            val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
            checkStatus(response)
            val type = object : TypeToken<List<SecurityFindingResponse>>() {}.type
            gson.fromJson<List<SecurityFindingResponse>>(response.body(), type)
        }
    }

    fun getRefactoringCandidates(project: Project, category: RefactoringCategory): RefactoringCandidatesResponse {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val cacheKey = "refactoring-candidates:${projectConfig.effectiveCustomer}:${projectConfig.system}:${category.value}"
        return cached(cacheKey) {
            val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "refactoring-candidates", projectConfig.effectiveCustomer, projectConfig.system, category.value)
            val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
            checkStatus(response)
            gson.fromJson(response.body(), RefactoringCandidatesResponse::class.java)
        }
    }

    fun getAllRefactoringCandidates(project: Project): Map<RefactoringCategory, RefactoringCandidatesResponse> {
        return RefactoringCategory.entries.associateWith { getRefactoringCandidates(project, it) }
    }

    // "raw" export of the full Architecture Quality graph - much heavier than the other end points, but it's
    // the only place Sigrid exposes per-file Git activity (CHURN) today. See ArchitectureQualityMapper.
    fun getArchitectureQualityRaw(project: Project): ArchitectureQualityRawResponse {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val cacheKey = "architecture-quality-raw:${projectConfig.effectiveCustomer}:${projectConfig.system}"
        return cached(cacheKey) {
            val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "architecture-quality", projectConfig.effectiveCustomer, projectConfig.system, "raw")
            val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
            checkStatus(response)
            gson.fromJson(response.body(), ArchitectureQualityRawResponse::class.java)
        }
    }

    // This end point is portfolio-wide (no {system} path segment), so the raw response is filtered down to
    // the current project's system here, once, rather than making every caller repeat that filter.
    fun getObjectivesEvaluation(project: Project, startDate: String, endDate: String): List<ObjectiveEvaluationResponse> {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val cacheKey = "objectives-evaluation:${projectConfig.effectiveCustomer}:${projectConfig.system}:$startDate:$endDate"
        return cached(cacheKey) {
            val base = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "objectives-evaluation", projectConfig.effectiveCustomer)
            val url = withDateRangeQuery(base, startDate, endDate)
            val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
            checkStatus(response)
            val evaluation = gson.fromJson(response.body(), ObjectivesEvaluationResponse::class.java)
            evaluation.systems.firstOrNull { it.systemName == projectConfig.system }?.objectives ?: emptyList()
        }
    }

    // "maintainability" end point (system-level rating, not the /raw or /components variants) - used only
    // by ObjectivesGate's Gate 3 market-benchmark fallback, for Maintainability findings with no explicit
    // objective configured. Cached separately (see maintainabilityRatingCache above);
    // MaintainabilityRatingWarmupActivity keeps this warm on project open and re-fetches it when Sigrid
    // settings change, so this should almost always be a cache hit rather than a live HTTP call.
    //
    // Open Source Health's equivalent rating doesn't need a call here at all - it's already present in the
    // OSH SBOM response this service fetches for the OSH tab (see OpenSourceHealthMapper.systemRating).
    fun getMaintainabilityRating(project: Project): Double? {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val cacheKey = "maintainability-rating:${projectConfig.effectiveCustomer}:${projectConfig.system}"
        return maintainabilityRatingCache.computeIfAbsent(cacheKey) { CachedRating(fetchMaintainabilityRating(projectConfig)) }.value
    }

    private fun fetchMaintainabilityRating(projectConfig: SigridProjectConfiguration): Double? {
        val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "maintainability", projectConfig.effectiveCustomer, projectConfig.system)
        val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
        checkStatusCode(response)
        if (response.body().isNullOrBlank() || response.body() == "null") return null
        return gson.fromJson(response.body(), MaintainabilityRatingResponse::class.java)?.maintainability
    }

    // "objectives config" end point - the resolved *target* per objective type, honoring Sigrid's own
    // system-over-portfolio precedence, with no date range needed (unlike objectives-evaluation). Used by
    // ObjectivesGate to compare Maintainability's live rating against its actual configured target
    // instead of trusting objectives-evaluation's date-range-dependent targetMetAtEnd for that capability.
    // Values are a mix of Double (rating/ratio types, e.g. MAINTAINABILITY: 4.0) and String (severity
    // enum types, e.g. OSH_MAX_SEVERITY: "LOW") - Gson deserializes numbers to Double when the declared
    // type is Any, so callers should check `as? Number` rather than `as? Double`.
    fun getObjectivesConfig(project: Project): Map<String, Any> {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val cacheKey = "objectives-config:${projectConfig.effectiveCustomer}:${projectConfig.system}"
        return cached(cacheKey) {
            val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "objectives", projectConfig.effectiveCustomer, projectConfig.system, "config")
            val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
            checkStatusCode(response)
            // Unlike checkStatus()'s blank/"null" body handling elsewhere in this service: a system with
            // zero objective coverage at all (a real, documented case - "orphan systems", design doc
            // section 2.7) is expected to return an empty object here, not an error. Treat a blank body
            // the same way, defensively, in case it comes back empty instead of "{}".
            if (response.body().isNullOrBlank() || response.body() == "null") return@cached emptyMap()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson<Map<String, Any>>(response.body(), type) ?: emptyMap()
        }
    }

    fun getSystemMetadata(project: Project): SystemMetadataResponse {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val cacheKey = "system-metadata:${projectConfig.effectiveCustomer}:${projectConfig.system}"
        return cached(cacheKey) {
            val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "system-metadata", projectConfig.effectiveCustomer, projectConfig.system)
            val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
            checkStatus(response)
            gson.fromJson(response.body(), SystemMetadataResponse::class.java)
        }
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
}