package com.softwareimprovementgroup.plugins.sigrid.services.onboarding

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GitignoreMatcherTest {

    @Test
    fun unanchoredDirectoryRule_matchesAtAnyDepth() {
        val matcher = GitignoreMatcher(listOf("node_modules/"))
        assertTrue(matcher.isIgnored("node_modules", true))
        assertTrue(matcher.isIgnored("web/node_modules", true))
        assertFalse(matcher.isIgnored("node_modules", false))
    }

    @Test
    fun anchoredRule_matchesOnlyFromRoot() {
        val matcher = GitignoreMatcher(listOf("/build"))
        assertTrue(matcher.isIgnored("build", true))
        assertFalse(matcher.isIgnored("src/build", true))
    }

    @Test
    fun globRules_matchWildcards() {
        val matcher = GitignoreMatcher(listOf("*.log", "docs/**/tmp"))
        assertTrue(matcher.isIgnored("a/b/debug.log", false))
        assertTrue(matcher.isIgnored("docs/x/y/tmp", true))
        assertFalse(matcher.isIgnored("debug.txt", false))
    }

    @Test
    fun negation_reincludesLaterMatch() {
        val matcher = GitignoreMatcher(listOf("*.log", "!keep.log"))
        assertTrue(matcher.isIgnored("a.log", false))
        assertFalse(matcher.isIgnored("keep.log", false))
    }

    @Test
    fun commentsAndBlankLines_areIgnored() {
        val matcher = GitignoreMatcher(listOf("# comment", "", "   "))
        assertFalse(matcher.isIgnored("comment", false))
    }
}
