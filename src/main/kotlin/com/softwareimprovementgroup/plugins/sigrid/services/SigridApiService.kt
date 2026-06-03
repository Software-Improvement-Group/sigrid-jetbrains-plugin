package com.softwareimprovementgroup.plugins.sigrid.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.softwareimprovementgroup.plugins.sigrid.models.*
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service(Service.Level.APP)
class SigridApiService {
    companion object {
        fun getInstance(): SigridApiService = service()
    }

    private val gson = Gson()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .proxy(ProxySelector.getDefault())
        .build()

    private fun buildRequest(url: String, projectConfig: SigridProjectConfiguration): HttpRequest.Builder {
        val apiKey = projectConfig.effectiveApiKey
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
    }

    private fun joinUrl(base: String, vararg paths: String): String {
        val normalizedBase = base.trimEnd('/')
        val path = paths.joinToString("/") { it.trim('/') }
        return "$normalizedBase/$path"
    }

    fun getOpenSourceHealthFindings(project: Project): OpenSourceHealthResponse {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "osh-findings", projectConfig.effectiveCustomer, projectConfig.system)
        val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
        return gson.fromJson(response.body(), OpenSourceHealthResponse::class.java)
    }

    fun getSecurityFindings(project: Project): List<SecurityFindingResponse> {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "security-findings", projectConfig.effectiveCustomer, projectConfig.system)
        val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
        val type = object : TypeToken<List<SecurityFindingResponse>>() {}.type
        return gson.fromJson(response.body(), type)
    }

    fun getRefactoringCandidates(project: Project, category: RefactoringCategory): RefactoringCandidatesResponse {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        val url = joinUrl(projectConfig.effectiveSigridApiBaseUrl, "refactoring-candidates", projectConfig.effectiveCustomer, projectConfig.system, category.value)
        val response = httpClient.send(buildRequest(url, projectConfig).GET().build(), HttpResponse.BodyHandlers.ofString())
        return gson.fromJson(response.body(), RefactoringCandidatesResponse::class.java)
    }

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
        httpClient.send(request, HttpResponse.BodyHandlers.discarding())
    }
}