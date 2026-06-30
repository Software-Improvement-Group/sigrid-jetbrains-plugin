package com.softwareimprovementgroup.plugins.sigrid.services

import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.JiraFinding
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JiraApiServiceTest {

    private val service = JiraApiService()

    // region normalizeUrl

    @Test
    fun normalizeUrl_bareHostname_prependsHttps() {
        assertEquals("https://jira.example.com", JiraApiService.normalizeUrl("jira.example.com"))
    }

    @Test
    fun normalizeUrl_trailingSlash_stripped() {
        assertEquals("https://jira.example.com", JiraApiService.normalizeUrl("jira.example.com/"))
    }

    @Test
    fun normalizeUrl_httpsUrl_unchanged() {
        assertEquals("https://jira.example.com", JiraApiService.normalizeUrl("https://jira.example.com"))
    }

    @Test
    fun normalizeUrl_httpsUrlWithPath_preserved() {
        assertEquals("https://jira.example.com/path", JiraApiService.normalizeUrl("https://jira.example.com/path"))
    }

    @Test
    fun normalizeUrl_httpUrl_notConverted() {
        assertEquals("http://jira.example.com", JiraApiService.normalizeUrl("http://jira.example.com"))
    }

    @Test
    fun normalizeUrl_leadingAndTrailingWhitespace_trimmed() {
        assertEquals("https://jira.example.com", JiraApiService.normalizeUrl("  jira.example.com  "))
    }

    // endregion

    // region HTTPS enforcement in createIssue

    @Test
    fun createIssue_httpUrl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            service.createIssue(
                jiraBaseUrl = "http://jira.example.com",
                jiraUser = "user",
                jiraToken = "token",
                jiraProjectKey = "PROJ",
                summary = "Fix this",
                findings = emptyList(),
            )
        }
    }

    @Test
    fun createIssue_ftpUrl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            service.createIssue(
                jiraBaseUrl = "ftp://jira.example.com",
                jiraUser = "user",
                jiraToken = "token",
                jiraProjectKey = "PROJ",
                summary = "Fix this",
                findings = emptyList(),
            )
        }
    }

    // endregion

    // region buildV3RequestBody

    @Test
    fun buildV3RequestBody_projectKey_setInFields() {
        val result = fields(service.buildV3RequestBody("MYPROJ", "summary", emptyList()))
        assertEquals(mapOf("key" to "MYPROJ"), result["project"])
    }

    @Test
    fun buildV3RequestBody_summary_setInFields() {
        val result = fields(service.buildV3RequestBody("PROJ", "My Issue Title", emptyList()))
        assertEquals("My Issue Title", result["summary"])
    }

    @Test
    fun buildV3RequestBody_issuetype_isTask() {
        val result = fields(service.buildV3RequestBody("PROJ", "summary", emptyList()))
        assertEquals(mapOf("name" to "Task"), result["issuetype"])
    }

    @Test
    fun buildV3RequestBody_description_isAdfDoc() {
        val description = description(service.buildV3RequestBody("PROJ", "summary", emptyList()))
        assertEquals("doc", description["type"])
        assertEquals(1, description["version"])
    }

    @Test
    fun buildV3RequestBody_description_hasHeadingAndIntro() {
        val description = description(service.buildV3RequestBody("PROJ", "summary", emptyList()))
        @Suppress("UNCHECKED_CAST")
        val content = description["content"] as List<Map<String, Any>>
        val types = content.map { it["type"] }
        assertTrue(types.contains("heading"), "Expected a heading node")
        assertTrue(types.contains("paragraph"), "Expected a paragraph node")
    }

    @Test
    fun buildV3RequestBody_findingTitle_appearsInBullet() {
        val finding = JiraFinding("SQL Injection", "🔴", emptyList())
        val body = service.buildV3RequestBody("PROJ", "summary", listOf(finding))
        val bodyJson = body.toString()
        assertTrue(bodyJson.contains("SQL Injection"), "Finding title should appear in body")
    }

    @Test
    fun buildV3RequestBody_fileLocationWithLine_formattedWithColon() {
        val loc = FileLocation(component = "svc", filePath = "src/Foo.kt", startLine = 42)
        val finding = JiraFinding("Issue", "🟠", listOf(loc))
        val body = service.buildV3RequestBody("PROJ", "summary", listOf(finding))
        val bodyJson = body.toString()
        assertTrue(bodyJson.contains("src/Foo.kt:42"), "Location with line should use 'path:line' format")
    }

    @Test
    fun buildV3RequestBody_fileLocationWithoutLine_formattedWithoutColon() {
        val loc = FileLocation(component = "svc", filePath = "src/Bar.kt", startLine = null)
        val finding = JiraFinding("Issue", "🟠", listOf(loc))
        val body = service.buildV3RequestBody("PROJ", "summary", listOf(finding))
        val bodyJson = body.toString()
        assertTrue(bodyJson.contains("src/Bar.kt"), "Location without line should just use path")
        assertFalse(bodyJson.contains("src/Bar.kt:"), "Location without line should not have colon")
    }

    @Test
    fun buildV3RequestBody_findingWithoutLocations_noNestedBulletList() {
        val finding = JiraFinding("Issue", "🟠", emptyList())
        val description = description(service.buildV3RequestBody("PROJ", "summary", listOf(finding)))
        @Suppress("UNCHECKED_CAST")
        val topContent = description["content"] as List<Map<String, Any>>
        val bulletList = topContent.first { it["type"] == "bulletList" }
        @Suppress("UNCHECKED_CAST")
        val listItems = bulletList["content"] as List<Map<String, Any>>
        val firstItem = listItems.first()
        @Suppress("UNCHECKED_CAST")
        val itemContent = firstItem["content"] as List<Map<String, Any>>
        val hasNestedBullet = itemContent.any { it["type"] == "bulletList" }
        assertFalse(hasNestedBullet, "Finding with no locations should have no nested bullet list")
    }

    @Test
    fun buildV3RequestBody_multipleFindings_allAppearAsBullets() {
        val findings = listOf(
            JiraFinding("Finding A", "🔴", emptyList()),
            JiraFinding("Finding B", "🟡", emptyList()),
        )
        val description = description(service.buildV3RequestBody("PROJ", "summary", findings))
        @Suppress("UNCHECKED_CAST")
        val topContent = description["content"] as List<Map<String, Any>>
        val bulletList = topContent.first { it["type"] == "bulletList" }
        @Suppress("UNCHECKED_CAST")
        val items = bulletList["content"] as List<*>
        assertEquals(2, items.size, "Two findings should produce two bullet items")
    }

    // endregion

    // region buildV2RequestBody

    @Test
    fun buildV2RequestBody_projectKey_setInFields() {
        val result = fields(service.buildV2RequestBody("MYPROJ", "summary", emptyList()))
        assertEquals(mapOf("key" to "MYPROJ"), result["project"])
    }

    @Test
    fun buildV2RequestBody_summary_setInFields() {
        val result = fields(service.buildV2RequestBody("PROJ", "My Issue Title", emptyList()))
        assertEquals("My Issue Title", result["summary"])
    }

    @Test
    fun buildV2RequestBody_issuetype_isTask() {
        val result = fields(service.buildV2RequestBody("PROJ", "summary", emptyList()))
        assertEquals(mapOf("name" to "Task"), result["issuetype"])
    }

    @Test
    fun buildV2RequestBody_description_containsH2Heading() {
        val desc = descriptionText(service.buildV2RequestBody("PROJ", "summary", emptyList()))
        assertTrue(desc.contains("h2."), "Description should have an h2 heading")
    }

    @Test
    fun buildV2RequestBody_findingTitle_inBoldBullet() {
        val finding = JiraFinding("SQL Injection", "🔴", emptyList())
        val desc = descriptionText(service.buildV2RequestBody("PROJ", "summary", listOf(finding)))
        assertTrue(desc.contains("* 🔴 *SQL Injection*"), "Finding should appear as bold bullet")
    }

    @Test
    fun buildV2RequestBody_fileLocationWithLine_nestedBulletWithColon() {
        val loc = FileLocation(component = "svc", filePath = "src/Foo.kt", startLine = 10)
        val finding = JiraFinding("Issue", "🟠", listOf(loc))
        val desc = descriptionText(service.buildV2RequestBody("PROJ", "summary", listOf(finding)))
        assertTrue(desc.contains("** src/Foo.kt:10"), "Location with line should use '** path:line'")
    }

    @Test
    fun buildV2RequestBody_fileLocationWithoutLine_nestedBulletWithoutColon() {
        val loc = FileLocation(component = "svc", filePath = "src/Bar.kt", startLine = null)
        val finding = JiraFinding("Issue", "🟠", listOf(loc))
        val desc = descriptionText(service.buildV2RequestBody("PROJ", "summary", listOf(finding)))
        assertTrue(desc.contains("** src/Bar.kt"), "Location without line should appear as nested bullet")
        assertFalse(desc.contains("** src/Bar.kt:"), "Location without line should not have colon")
    }

    @Test
    fun buildV2RequestBody_findingWithoutLocations_noNestedBullets() {
        val finding = JiraFinding("Issue", "🟠", emptyList())
        val desc = descriptionText(service.buildV2RequestBody("PROJ", "summary", listOf(finding)))
        assertFalse(desc.contains("**"), "Finding with no locations should have no nested bullets")
    }

    @Test
    fun buildV2RequestBody_multipleFindings_allAppear() {
        val findings = listOf(
            JiraFinding("Finding A", "🔴", emptyList()),
            JiraFinding("Finding B", "🟡", emptyList()),
        )
        val desc = descriptionText(service.buildV2RequestBody("PROJ", "summary", findings))
        assertTrue(desc.contains("Finding A") && desc.contains("Finding B"), "Both findings should appear")
    }

    // endregion

    // region buildPreviewHtml

    @Test
    fun buildPreviewHtml_emptyFindings_containsHeadingAndIntroOnly() {
        val html = JiraApiService.buildPreviewHtml(emptyList())
        assertTrue(html.contains("<h3>Code selected for refactoring</h3>"))
        assertTrue(html.contains("The following Sigrid findings have been selected for improvement:"))
        assertFalse(html.contains("<li>"), "No list items expected for empty findings")
    }

    @Test
    fun buildPreviewHtml_singleFinding_titleBoldWithEmoji() {
        val finding = JiraFinding("SQL Injection", "🔴", emptyList())
        val html = JiraApiService.buildPreviewHtml(listOf(finding))
        assertTrue(html.contains("<b>🔴 SQL Injection</b>"))
    }

    @Test
    fun buildPreviewHtml_fileLocationWithLine_formattedWithColon() {
        val loc = FileLocation(component = "svc", filePath = "src/Foo.kt", startLine = 42)
        val finding = JiraFinding("Issue", "🟠", listOf(loc))
        val html = JiraApiService.buildPreviewHtml(listOf(finding))
        assertTrue(html.contains("<li>src/Foo.kt:42</li>"))
    }

    @Test
    fun buildPreviewHtml_fileLocationWithoutLine_noColon() {
        val loc = FileLocation(component = "svc", filePath = "src/Bar.kt", startLine = null)
        val finding = JiraFinding("Issue", "🟠", listOf(loc))
        val html = JiraApiService.buildPreviewHtml(listOf(finding))
        assertTrue(html.contains("<li>src/Bar.kt</li>"))
        assertFalse(html.contains("<li>src/Bar.kt:"), "Location without line should not have colon")
    }

    @Test
    fun buildPreviewHtml_multipleFindings_allTitlesAppear() {
        val findings = listOf(
            JiraFinding("Finding A", "🔴", emptyList()),
            JiraFinding("Finding B", "🟡", emptyList()),
        )
        val html = JiraApiService.buildPreviewHtml(findings)
        assertTrue(html.contains("Finding A") && html.contains("Finding B"))
    }

    @Test
    fun buildPreviewHtml_findingWithNoLocations_noNestedList() {
        val finding = JiraFinding("Issue", "🟠", emptyList())
        val html = JiraApiService.buildPreviewHtml(listOf(finding))
        assertFalse(html.contains("<ul><ul>"), "No nested list expected for finding with no locations")
    }

    @Test
    fun buildPreviewHtml_specialCharsInTitle_escaped() {
        val finding = JiraFinding("A < B & C > D", "🔴", emptyList())
        val html = JiraApiService.buildPreviewHtml(listOf(finding))
        assertTrue(html.contains("A &lt; B &amp; C &gt; D"), "Special HTML chars should be escaped")
        assertFalse(html.contains("A < B"), "Raw < should not appear in output")
    }

    @Test
    fun buildPreviewHtml_specialCharsInPath_escaped() {
        val loc = FileLocation(component = "svc", filePath = "src/A&B.kt", startLine = null)
        val finding = JiraFinding("Issue", "🟠", listOf(loc))
        val html = JiraApiService.buildPreviewHtml(listOf(finding))
        assertTrue(html.contains("src/A&amp;B.kt"))
    }

    // endregion

    // region parseIssueKey

    @Test
    fun parseIssueKey_validJson_returnsKey() {
        assertEquals("PROJ-42", service.parseIssueKey("""{"key":"PROJ-42","id":"10001"}"""))
    }

    @Test
    fun parseIssueKey_missingKey_throwsException() {
        assertThrows(Exception::class.java) {
            service.parseIssueKey("""{"id":"10001"}""")
        }
    }

    // endregion

    // region helpers

    @Suppress("UNCHECKED_CAST")
    private fun fields(body: Map<String, Any>): Map<String, Any> =
        body["fields"] as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun description(body: Map<String, Any>): Map<String, Any> =
        fields(body)["description"] as Map<String, Any>

    private fun descriptionText(body: Map<String, Any>): String =
        fields(body)["description"] as String

    // endregion
}
