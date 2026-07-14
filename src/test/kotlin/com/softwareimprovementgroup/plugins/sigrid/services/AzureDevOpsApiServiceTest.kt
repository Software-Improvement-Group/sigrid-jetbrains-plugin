package com.softwareimprovementgroup.plugins.sigrid.services

import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.IssueFinding
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AzureDevOpsApiServiceTest {

    private val service = AzureDevOpsApiService()

    // region normalizeUrl

    @Test
    fun normalizeUrl_bareHostname_prependsHttps() {
        assertEquals("https://dev.azure.com/myorg", AzureDevOpsApiService.normalizeUrl("dev.azure.com/myorg"))
    }

    @Test
    fun normalizeUrl_trailingSlash_stripped() {
        assertEquals("https://dev.azure.com/myorg", AzureDevOpsApiService.normalizeUrl("dev.azure.com/myorg/"))
    }

    @Test
    fun normalizeUrl_httpsUrl_unchanged() {
        assertEquals("https://dev.azure.com/myorg", AzureDevOpsApiService.normalizeUrl("https://dev.azure.com/myorg"))
    }

    @Test
    fun normalizeUrl_visualStudioUrl_unchanged() {
        assertEquals("https://myorg.visualstudio.com", AzureDevOpsApiService.normalizeUrl("https://myorg.visualstudio.com"))
    }

    @Test
    fun normalizeUrl_leadingAndTrailingWhitespace_trimmed() {
        assertEquals("https://dev.azure.com/myorg", AzureDevOpsApiService.normalizeUrl("  dev.azure.com/myorg  "))
    }

    // endregion

    // region buildAuthHeader

    @Test
    fun buildAuthHeader_encodesPATWithColonPrefix() {
        val header = AzureDevOpsApiService.buildAuthHeader("mytoken")
        assertTrue(header.startsWith("Basic "), "Should start with 'Basic '")
        val decoded = String(java.util.Base64.getDecoder().decode(header.removePrefix("Basic ")))
        assertEquals(":mytoken", decoded, "Should encode ':pat' (no username)")
    }

    // endregion

    // region buildHtmlDescription

    @Test
    fun buildHtmlDescription_containsH2HeadingAndIntro() {
        val html = AzureDevOpsApiService.buildHtmlDescription(emptyList(), "https://sigrid.example.com")
        assertTrue(html.contains("<h2>Code selected for refactoring</h2>"))
        assertTrue(html.contains("The following Sigrid findings have been selected for improvement:"))
    }

    @Test
    fun buildHtmlDescription_containsSigridLink() {
        val html = AzureDevOpsApiService.buildHtmlDescription(emptyList(), "https://sigrid.example.com/cust/sys")
        assertTrue(html.contains("<a href=\"https://sigrid.example.com/cust/sys\">Sigrid</a>"))
    }

    @Test
    fun buildHtmlDescription_singleFinding_titleBoldWithEmoji() {
        val finding = IssueFinding("SQL Injection", "🔴", emptyList())
        val html = AzureDevOpsApiService.buildHtmlDescription(listOf(finding), "https://sigrid.example.com")
        assertTrue(html.contains("🔴 <strong>SQL Injection</strong>"))
    }

    @Test
    fun buildHtmlDescription_fileLocationWithLine_formattedWithColon() {
        val loc = FileLocation(component = "svc", filePath = "src/Foo.kt", startLine = 42)
        val finding = IssueFinding("Issue", "🟠", listOf(loc))
        val html = AzureDevOpsApiService.buildHtmlDescription(listOf(finding), "https://sigrid.example.com")
        assertTrue(html.contains("<li>src/Foo.kt:42</li>"))
    }

    @Test
    fun buildHtmlDescription_fileLocationWithoutLine_noColon() {
        val loc = FileLocation(component = "svc", filePath = "src/Bar.kt", startLine = null)
        val finding = IssueFinding("Issue", "🟠", listOf(loc))
        val html = AzureDevOpsApiService.buildHtmlDescription(listOf(finding), "https://sigrid.example.com")
        assertTrue(html.contains("<li>src/Bar.kt</li>"))
        assertFalse(html.contains("<li>src/Bar.kt:"))
    }

    @Test
    fun buildHtmlDescription_specialCharsInTitle_escaped() {
        val finding = IssueFinding("A < B & C > D", "🔴", emptyList())
        val html = AzureDevOpsApiService.buildHtmlDescription(listOf(finding), "https://sigrid.example.com")
        assertTrue(html.contains("A &lt; B &amp; C &gt; D"))
        assertFalse(html.contains("A < B"))
    }

    @Test
    fun buildHtmlDescription_specialCharsInSigridUrl_escaped() {
        val html = AzureDevOpsApiService.buildHtmlDescription(emptyList(), "https://sigrid.example.com/cust&special/sys")
        assertTrue(html.contains("href=\"https://sigrid.example.com/cust&amp;special/sys\""))
    }

    @Test
    fun buildHtmlDescription_findingWithNoLocations_noNestedList() {
        val finding = IssueFinding("Issue", "🟠", emptyList())
        val html = AzureDevOpsApiService.buildHtmlDescription(listOf(finding), "https://sigrid.example.com")
        assertFalse(html.contains("<ul><ul>"), "No nested list expected for finding with no locations")
    }

    // endregion

    // region parseWorkItemResult

    @Test
    fun parseWorkItemResult_validJson_returnsIdAndBrowserUrl() {
        val json = """{"id":131489,"_links":{"html":{"href":"https://dev.azure.com/fabrikam/web/wi.aspx?id=131489"}}}"""
        val result = service.parseWorkItemResult(json)
        assertEquals(131489, result.id)
        assertEquals("https://dev.azure.com/fabrikam/web/wi.aspx?id=131489", result.browserUrl)
    }

    @Test
    fun parseWorkItemResult_missingLinks_browserUrlIsNull() {
        val json = """{"id":42}"""
        val result = service.parseWorkItemResult(json)
        assertEquals(42, result.id)
        assertNull(result.browserUrl)
    }

    @Test
    fun parseWorkItemResult_missingId_throwsException() {
        assertThrows(Exception::class.java) {
            service.parseWorkItemResult("""{"_links":{}}""")
        }
    }

    // endregion

    // region resolveDescriptionField

    @Test
    fun resolveDescriptionField_hasReproSteps_returnsReproStepsReference() {
        val json = """{"fields":[{"referenceName":"System.Title"},{"referenceName":"Microsoft.VSTS.TCM.ReproSteps"},{"referenceName":"System.Description"}]}"""
        assertEquals("Microsoft.VSTS.TCM.ReproSteps", AzureDevOpsApiService.resolveDescriptionField(json))
    }

    @Test
    fun resolveDescriptionField_noReproSteps_returnsSystemDescription() {
        val json = """{"fields":[{"referenceName":"System.Title"},{"referenceName":"System.Description"}]}"""
        assertEquals("System.Description", AzureDevOpsApiService.resolveDescriptionField(json))
    }

    @Test
    fun resolveDescriptionField_emptyFieldsArray_returnsSystemDescription() {
        val json = """{"fields":[]}"""
        assertEquals("System.Description", AzureDevOpsApiService.resolveDescriptionField(json))
    }

    @Test
    fun resolveDescriptionField_missingFieldsKey_returnsSystemDescription() {
        val json = """{"name":"Custom"}"""
        assertEquals("System.Description", AzureDevOpsApiService.resolveDescriptionField(json))
    }

    // endregion

    // region HTTPS enforcement in createWorkItem and getWorkItemTypes

    @Test
    fun createWorkItem_httpUrl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            service.createWorkItem(
                organizationUrl = "http://dev.azure.com/myorg",
                projectName = "MyProject",
                pat = "token",
                workItemType = "Task",
                title = "Fix this",
                findings = emptyList(),
                sigridUrl = "https://sigrid-says.com",
            )
        }
    }

    @Test
    fun getWorkItemTypes_httpUrl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            service.getWorkItemTypes(
                organizationUrl = "http://dev.azure.com/myorg",
                projectName = "MyProject",
                pat = "token",
            )
        }
    }

    // endregion
}
