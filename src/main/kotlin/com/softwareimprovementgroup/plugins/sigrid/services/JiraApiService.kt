package com.softwareimprovementgroup.plugins.sigrid.services

import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import java.net.http.HttpClient
import java.net.http.HttpResponse

@Service(Service.Level.APP)
class JiraApiService {
    companion object {
        fun getInstance(): JiraApiService = service()

        internal fun normalizeUrl(url: String): String = normalizeIssueTrackerUrl(url)

        internal fun buildPreviewHtml(findings: List<IssueFinding>): String {
            val sb = StringBuilder()
            sb.append("<html><body>")
            sb.append("<h3>Code selected for refactoring</h3>")
            sb.append("<p>The following Sigrid findings have been selected for improvement:</p>")
            sb.append(buildFindingListHtml(findings) { emoji, title -> "<b>$emoji $title</b>" })
            sb.append("</body></html>")
            return sb.toString()
        }
    }

    private val gson = Gson()
    private val httpClient: HttpClient = buildIssueTrackerHttpClient()

    fun createIssue(
        jiraBaseUrl: String,
        jiraUser: String,
        jiraToken: String,
        jiraProjectKey: String,
        summary: String,
        findings: List<IssueFinding>,
    ): String {
        val baseUrl = normalizeUrl(jiraBaseUrl)
        if (!baseUrl.startsWith("https://")) {
            throw IllegalArgumentException("Jira Base URL must use the https:// protocol")
        }
        val authHeader = buildBasicAuthHeader("$jiraUser:$jiraToken")

        val v3Body = gson.toJson(buildV3RequestBody(jiraProjectKey, summary, findings))
        val v3Response = sendPost("$baseUrl/rest/api/3/issue", v3Body, authHeader)

        if (v3Response.statusCode() == 400) {
            val v2Body = gson.toJson(buildV2RequestBody(jiraProjectKey, summary, findings))
            val v2Response = sendPost("$baseUrl/rest/api/2/issue", v2Body, authHeader)
            checkResponse(v2Response)
            return parseIssueKey(v2Response.body())
        }

        checkResponse(v3Response)
        return parseIssueKey(v3Response.body())
    }

    private fun sendPost(url: String, body: String, authHeader: String): HttpResponse<String> =
        httpClient.sendPost(url, body, authHeader, "application/json")

    private fun checkResponse(response: HttpResponse<String>) {
        if (response.statusCode() !in 200..299) {
            throw Exception("HTTP ${response.statusCode()}: ${response.body()}")
        }
    }

    internal fun parseIssueKey(body: String): String {
        @Suppress("UNCHECKED_CAST")
        val parsed = gson.fromJson(body, Map::class.java) as Map<String, Any>
        return parsed["key"] as? String ?: throw Exception("No issue key in Jira response")
    }

    internal fun buildV3RequestBody(
        projectKey: String,
        summary: String,
        findings: List<IssueFinding>,
    ): Map<String, Any> {
        val bulletItems = findings.map { finding ->
            val locationItems = finding.fileLocations.map { loc ->
                val text = if (loc.startLine != null) "${loc.filePath}:${loc.startLine}" else loc.filePath
                mapOf(
                    "type" to "listItem",
                    "content" to listOf(mapOf(
                        "type" to "paragraph",
                        "content" to listOf(mapOf("type" to "text", "text" to text)),
                    )),
                )
            }

            val itemContent = mutableListOf(mapOf(
                "type" to "paragraph",
                "content" to listOf(mapOf(
                    "type" to "text",
                    "text" to "${finding.severityEmoji} ${finding.title}",
                    "marks" to listOf(mapOf("type" to "strong")),
                )),
            ))
            if (locationItems.isNotEmpty()) {
                itemContent += mapOf("type" to "bulletList", "content" to locationItems)
            }

            mapOf("type" to "listItem", "content" to itemContent)
        }

        val description = mapOf(
            "type" to "doc",
            "version" to 1,
            "content" to listOf(
                mapOf(
                    "type" to "heading",
                    "attrs" to mapOf("level" to 2),
                    "content" to listOf(mapOf("type" to "text", "text" to "Code selected for refactoring")),
                ),
                mapOf(
                    "type" to "paragraph",
                    "content" to listOf(mapOf("type" to "text", "text" to "The following Sigrid findings have been selected for improvement:")),
                ),
                mapOf("type" to "bulletList", "content" to bulletItems),
            ),
        )

        return mapOf(
            "fields" to mapOf(
                "project" to mapOf("key" to projectKey),
                "summary" to summary,
                "issuetype" to mapOf("name" to "Task"),
                "description" to description,
            ),
        )
    }

    internal fun buildV2RequestBody(
        projectKey: String,
        summary: String,
        findings: List<IssueFinding>,
    ): Map<String, Any> {
        val sb = StringBuilder()
        sb.appendLine("h2. Code selected for refactoring")
        sb.appendLine()
        sb.appendLine("The following Sigrid findings have been selected for improvement:")
        sb.appendLine()
        for (finding in findings) {
            sb.appendLine("* ${finding.severityEmoji} *${finding.title}*")
            for (loc in finding.fileLocations) {
                val text = if (loc.startLine != null) "${loc.filePath}:${loc.startLine}" else loc.filePath
                sb.appendLine("** $text")
            }
        }

        return mapOf(
            "fields" to mapOf(
                "project" to mapOf("key" to projectKey),
                "summary" to summary,
                "issuetype" to mapOf("name" to "Task"),
                "description" to sb.toString(),
            ),
        )
    }
}
