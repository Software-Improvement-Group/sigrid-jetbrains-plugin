package com.softwareimprovementgroup.plugins.sigrid.services

import com.google.common.html.HtmlEscapers
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import java.net.ProxySelector
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

internal val ISSUE_TRACKER_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)
internal val ISSUE_TRACKER_REQUEST_TIMEOUT: Duration = Duration.ofSeconds(30)

internal fun buildIssueTrackerHttpClient(): HttpClient = HttpClient.newBuilder()
    .proxy(ProxySelector.getDefault())
    .connectTimeout(ISSUE_TRACKER_CONNECT_TIMEOUT)
    .build()

internal fun normalizeIssueTrackerUrl(url: String): String {
    val trimmed = url.trim().trimEnd('/')
    return if (trimmed.contains("://")) trimmed else "https://$trimmed"
}

internal fun buildBasicAuthHeader(credentials: String): String =
    "Basic " + Base64.getEncoder().encodeToString(credentials.toByteArray())

internal fun encodeUrlPathSegment(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

internal fun buildFindingListHtml(
    findings: List<IssueFinding>,
    renderTitle: (emoji: String, escapedTitle: String) -> String,
): String {
    val escaper = HtmlEscapers.htmlEscaper()
    val sb = StringBuilder()
    sb.append("<ul>")
    for (finding in findings) {
        sb.append("<li>${renderTitle(escaper.escape(finding.severityEmoji), escaper.escape(finding.title))}")
        if (finding.fileLocations.isNotEmpty()) {
            sb.append("<ul>")
            for (loc in finding.fileLocations) {
                val text = if (loc.startLine != null) "${loc.filePath}:${loc.startLine}" else loc.filePath
                sb.append("<li>${escaper.escape(text)}</li>")
            }
            sb.append("</ul>")
        }
        sb.append("</li>")
    }
    sb.append("</ul>")
    return sb.toString()
}

internal fun HttpClient.sendPost(
    url: String,
    body: String,
    authHeader: String,
    contentType: String,
): HttpResponse<String> {
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(ISSUE_TRACKER_REQUEST_TIMEOUT)
        .header("Content-Type", contentType)
        .header("Authorization", authHeader)
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
    return send(request, HttpResponse.BodyHandlers.ofString())
}
