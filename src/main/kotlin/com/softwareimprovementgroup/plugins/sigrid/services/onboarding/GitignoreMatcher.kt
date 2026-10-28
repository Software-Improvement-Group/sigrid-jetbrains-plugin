package com.softwareimprovementgroup.plugins.sigrid.services.onboarding

/**
 * Minimal `.gitignore` semantics (comments, negation, directory-only rules, anchoring, `*`, `?`, `**`),
 * used when `git` is unavailable. Paths are relative and `/`-separated.
 */
internal class GitignoreMatcher(lines: List<String>) {

    private class Rule(glob: String, val negated: Boolean, val directoryOnly: Boolean) {
        val regex = Regex(if (glob.contains('/')) globToRegex(glob.trimStart('/')) else "(?:.*/)?${globToRegex(glob)}")
    }

    private val rules = lines.mapNotNull(::parseRule)

    fun isIgnored(relativePath: String, isDirectory: Boolean): Boolean {
        var ignored = false
        for (rule in rules) {
            if ((isDirectory || !rule.directoryOnly) && rule.regex.matches(relativePath)) ignored = !rule.negated
        }
        return ignored
    }

    private fun parseRule(line: String): Rule? {
        val text = line.trimEnd()
        if (text.isEmpty() || text.startsWith("#")) return null
        val glob = text.removePrefix("!").trimEnd('/')
        return if (glob.isEmpty()) null else Rule(glob, text.startsWith("!"), text.endsWith("/"))
    }
}

private fun globToRegex(glob: String): String {
    val regex = StringBuilder()
    var i = 0
    while (i < glob.length) {
        i += when {
            glob.startsWith("**/", i) -> { regex.append("(?:.*/)?"); 3 }
            glob.startsWith("**", i) -> { regex.append(".*"); 2 }
            glob[i] == '*' -> { regex.append("[^/]*"); 1 }
            glob[i] == '?' -> { regex.append("[^/]"); 1 }
            else -> { regex.append(Regex.escape(glob[i].toString())); 1 }
        }
    }
    return regex.toString()
}
