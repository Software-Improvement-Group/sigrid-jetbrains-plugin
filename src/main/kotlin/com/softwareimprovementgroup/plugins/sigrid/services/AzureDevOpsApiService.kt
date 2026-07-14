package com.softwareimprovementgroup.plugins.sigrid.services

import com.google.common.html.HtmlEscapers
import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.components.service
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentHashMap

data class AzureDevOpsWorkItemResult(val id: Int, val browserUrl: String?)

@Service(Service.Level.APP)
class AzureDevOpsApiService {
    companion object {
        private val EXCLUDED_CATEGORY_REFS = setOf("microsoft.testcasecategory", "microsoft.epiccategory")
        private const val HIDDEN_CATEGORY_REF = "microsoft.hiddencategory"

        fun getInstance(): AzureDevOpsApiService = service()

        internal fun normalizeUrl(url: String): String = normalizeIssueTrackerUrl(url)

        internal fun buildAuthHeader(pat: String): String = buildBasicAuthHeader(":$pat")

        internal fun buildHtmlDescription(findings: List<IssueFinding>, sigridUrl: String): String {
            val escaper = HtmlEscapers.htmlEscaper()
            val sb = StringBuilder()
            sb.append("<h2>Code selected for refactoring</h2>")
            sb.append("<p>The following Sigrid findings have been selected for improvement:</p>")
            sb.append(buildFindingListHtml(findings) { emoji, title -> "$emoji <strong>$title</strong>" })
            sb.append("<p>You can find more information in <a href=\"${escaper.escape(sigridUrl)}\">Sigrid</a>.</p>")
            return sb.toString()
        }

        internal fun buildPreviewHtml(findings: List<IssueFinding>, sigridUrl: String): String =
            "<html><body>${buildHtmlDescription(findings, sigridUrl)}</body></html>"

        private const val REPRO_STEPS_FIELD = "Microsoft.VSTS.TCM.ReproSteps"
        private const val DEFAULT_DESCRIPTION_FIELD = "System.Description"

        @Suppress("UNCHECKED_CAST")
        internal fun resolveDescriptionField(workItemTypeJson: String): String {
            val root = Gson().fromJson(workItemTypeJson, Map::class.java) as Map<String, Any>
            val fields = root["fields"] as? List<Map<String, Any>> ?: return DEFAULT_DESCRIPTION_FIELD
            val hasReproSteps = fields.any { (it["referenceName"] as? String) == REPRO_STEPS_FIELD }
            return if (hasReproSteps) REPRO_STEPS_FIELD else DEFAULT_DESCRIPTION_FIELD
        }
    }

    private val gson = Gson()
    private val httpClient: HttpClient = buildIssueTrackerHttpClient()

    @Volatile private var cachedTypes: Pair<String, List<String>>? = null
    private val descriptionFieldCache = ConcurrentHashMap<String, String>()

    fun getWorkItemTypes(organizationUrl: String, projectName: String, pat: String): List<String> {
        val url = normalizeUrl(organizationUrl)
        if (!url.startsWith("https://")) {
            throw IllegalArgumentException("Organization URL must use the https:// protocol")
        }
        val cacheKey = "$url|$projectName|$pat"
        cachedTypes?.takeIf { it.first == cacheKey }?.let { return it.second }

        val authHeader = buildAuthHeader(pat)
        val encodedProject = encodePathSegment(projectName)
        val categoriesJson = sendGet("$url/$encodedProject/_apis/wit/workitemtypecategories?api-version=7.1", authHeader)
        val typesJson = sendGet("$url/$encodedProject/_apis/wit/workitemtypes?api-version=7.1", authHeader)
        val types = filterWorkItemTypes(categoriesJson, typesJson)

        cachedTypes = cacheKey to types
        return types
    }

    fun createWorkItem(
        organizationUrl: String,
        projectName: String,
        pat: String,
        workItemType: String,
        title: String,
        findings: List<IssueFinding>,
        sigridUrl: String,
    ): AzureDevOpsWorkItemResult {
        val url = normalizeUrl(organizationUrl)
        if (!url.startsWith("https://")) {
            throw IllegalArgumentException("Organization URL must use the https:// protocol")
        }
        val authHeader = buildAuthHeader(pat)
        val encodedProject = encodePathSegment(projectName)
        val encodedType = encodePathSegment(workItemType)
        val apiUrl = "$url/$encodedProject/_apis/wit/workitems/\$$encodedType?api-version=7.1"
        val descField = getDescriptionField(url, projectName, pat, workItemType)
        val body = gson.toJson(buildRequestBody(title, buildHtmlDescription(findings, sigridUrl), descField))
        val response = sendPost(apiUrl, body, authHeader)
        checkResponse(response)
        return parseWorkItemResult(response.body())
    }

    private fun filterWorkItemTypes(categoriesJson: String, typesJson: String): List<String> {
        val hiddenTypeNames = extractHiddenCategoryTypeNames(categoriesJson)
        val excludedDefaultTypeNames = extractExcludedDefaultTypeNames(categoriesJson)
        return extractEnabledTypeNames(typesJson)
            .filter { it !in hiddenTypeNames }
            .filter { it !in excludedDefaultTypeNames }
            .sorted()
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractHiddenCategoryTypeNames(categoriesJson: String): Set<String> {
        val root = gson.fromJson(categoriesJson, Map::class.java) as Map<String, Any>
        val categories = root["value"] as? List<Map<String, Any>> ?: run {
            thisLogger().warn("Unexpected Azure DevOps categories response shape; skipping hidden-type filter")
            return emptySet()
        }
        return categories
            .filter { (it["referenceName"] as? String)?.lowercase() == HIDDEN_CATEGORY_REF }
            .flatMap { cat -> (cat["workItemTypes"] as? List<Map<String, Any>>)?.mapNotNull { it["name"] as? String } ?: emptyList() }
            .toSet()
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractExcludedDefaultTypeNames(categoriesJson: String): Set<String> {
        val root = gson.fromJson(categoriesJson, Map::class.java) as Map<String, Any>
        val categories = root["value"] as? List<Map<String, Any>> ?: run {
            thisLogger().warn("Unexpected Azure DevOps categories response shape; skipping excluded-type filter")
            return emptySet()
        }
        return categories
            .filter { (it["referenceName"] as? String)?.lowercase() in EXCLUDED_CATEGORY_REFS }
            .mapNotNull { (it["defaultWorkItemType"] as? Map<String, Any>)?.get("name") as? String }
            .toSet()
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractEnabledTypeNames(typesJson: String): List<String> {
        val root = gson.fromJson(typesJson, Map::class.java) as Map<String, Any>
        val types = root["value"] as? List<Map<String, Any>> ?: return emptyList()
        return types
            .filter { it["isDisabled"] != true }
            .mapNotNull { it["name"] as? String }
    }

    private fun encodePathSegment(value: String): String = encodeUrlPathSegment(value)

    private fun getDescriptionField(url: String, project: String, pat: String, workItemType: String): String {
        val cacheKey = "$url|$project|${workItemType.lowercase()}"
        return descriptionFieldCache.computeIfAbsent(cacheKey) {
            val encodedProject = encodePathSegment(project)
            val encodedType = encodePathSegment(workItemType)
            val json = sendGet("$url/$encodedProject/_apis/wit/workitemtypes/$encodedType?api-version=7.1", buildAuthHeader(pat))
            resolveDescriptionField(json)
        }
    }

    private fun buildRequestBody(title: String, description: String, descriptionField: String): List<Map<String, String>> = listOf(
        mapOf("op" to "add", "path" to "/fields/System.Title", "value" to title),
        mapOf("op" to "add", "path" to "/fields/$descriptionField", "value" to description),
    )

    private fun sendGet(url: String, authHeader: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(ISSUE_TRACKER_REQUEST_TIMEOUT)
            .header("Authorization", authHeader)
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        checkResponse(response)
        return response.body()
    }

    private fun sendPost(url: String, body: String, authHeader: String): HttpResponse<String> =
        httpClient.sendPost(url, body, authHeader, "application/json-patch+json")

    private fun checkResponse(response: HttpResponse<String>) {
        if (response.statusCode() !in 200..299) {
            throw Exception("HTTP ${response.statusCode()}: ${extractErrorMessage(response.body())}")
        }
    }

    private fun extractErrorMessage(body: String): String {
        if (body.trimStart().startsWith("<")) return "Unexpected response from server"
        return try {
            @Suppress("UNCHECKED_CAST")
            val parsed = gson.fromJson(body, Map::class.java) as Map<String, Any>
            parsed["message"] as? String ?: "Unexpected error"
        } catch (e: Exception) {
            thisLogger().warn("Failed to parse Azure DevOps error response", e)
            "Unexpected error"
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun parseWorkItemResult(body: String): AzureDevOpsWorkItemResult {
        val parsed = gson.fromJson(body, Map::class.java) as Map<String, Any>
        val id = (parsed["id"] as? Double)?.toInt() ?: throw Exception("No work item id in Azure DevOps response")
        val links = parsed["_links"] as? Map<String, Any>
        val html = links?.get("html") as? Map<String, Any>
        val browserUrl = html?.get("href") as? String
        return AzureDevOpsWorkItemResult(id, browserUrl)
    }
}
