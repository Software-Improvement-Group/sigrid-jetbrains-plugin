package com.softwareimprovementgroup.plugins.sigrid.toolWindow.panels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CreateIssueButtonTest {

    // resolveActiveTracker

    @Test
    fun resolveActiveTracker_noHistory_jiraConfigured_returnsJira() =
        assertEquals("jira", CreateIssueButton.resolveActiveTracker("", true, false))

    @Test
    fun resolveActiveTracker_noHistory_azureConfigured_returnsAzure() =
        assertEquals("azuredevops", CreateIssueButton.resolveActiveTracker("", false, true))

    @Test
    fun resolveActiveTracker_noHistory_bothConfigured_prefersJira() =
        assertEquals("jira", CreateIssueButton.resolveActiveTracker("", true, true))

    @Test
    fun resolveActiveTracker_noHistory_neitherConfigured_returnsNull() =
        assertNull(CreateIssueButton.resolveActiveTracker("", false, false))

    @Test
    fun resolveActiveTracker_lastJira_jiraConfigured_returnsJira() =
        assertEquals("jira", CreateIssueButton.resolveActiveTracker("jira", true, false))

    @Test
    fun resolveActiveTracker_lastJira_jiraNotConfigured_azureConfigured_returnsAzure() =
        assertEquals("azuredevops", CreateIssueButton.resolveActiveTracker("jira", false, true))

    @Test
    fun resolveActiveTracker_lastJira_neitherConfigured_returnsNull() =
        assertNull(CreateIssueButton.resolveActiveTracker("jira", false, false))

    @Test
    fun resolveActiveTracker_lastAzure_azureConfigured_returnsAzure() =
        assertEquals("azuredevops", CreateIssueButton.resolveActiveTracker("azuredevops", false, true))

    @Test
    fun resolveActiveTracker_lastAzure_azureNotConfigured_jiraConfigured_returnsJira() =
        assertEquals("jira", CreateIssueButton.resolveActiveTracker("azuredevops", true, false))

    @Test
    fun resolveActiveTracker_lastAzure_neitherConfigured_returnsNull() =
        assertNull(CreateIssueButton.resolveActiveTracker("azuredevops", false, false))

    @Test
    fun resolveActiveTracker_lastJira_bothConfigured_returnsJira() =
        assertEquals("jira", CreateIssueButton.resolveActiveTracker("jira", true, true))

    @Test
    fun resolveActiveTracker_lastAzure_bothConfigured_returnsAzure() =
        assertEquals("azuredevops", CreateIssueButton.resolveActiveTracker("azuredevops", true, true))

}
