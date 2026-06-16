package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ProxySelector
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val USAGE_STATISTICS_URL = "https://sigrid-says.com/usage/matomo.php?idsite=5&rec=1&ca=1&e_c=jetbrains&e_a="

fun buildTrackingUrl(customer: String): String =
    "$USAGE_STATISTICS_URL${URLEncoder.encode(customer, "UTF-8")}"

class UsageStatisticsActivity : ProjectActivity {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .proxy(ProxySelector.getDefault())
        .build()

    override suspend fun execute(project: Project) {
        val projectConfig = SigridProjectConfiguration.getInstance(project)
        if (!projectConfig.isConfigurationValid) return

        val url = buildTrackingUrl(projectConfig.effectiveCustomer)

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build()
            withContext(Dispatchers.IO) {
                httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            }
        } catch (e: Exception) {
            thisLogger().error("Failed to send usage statistics", e)
        }
    }
}