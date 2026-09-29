package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding

// PROVISIONAL path heuristics, standing in for the SAT's own production/test/generated classification,
// which Sigrid does not yet expose via the public API (see the Epic 432 design doc, section 2.8 and open
// item #13 - the classification already exists inside the SAT, it's just not surfaced on findings today).
// Deliberately conservative: only common, unambiguous markers, so real findings on plausible-looking
// application code aren't hidden by a false positive. Revisit once Sigrid exposes the real classification.
object PriorityExclusionFilter {

    private val VENDORED_OR_GENERATED_DIRECTORY_MARKERS = listOf(
        "/node_modules/", "/vendor/", "/third_party/", "/dist/", "/build/", "/target/",
        "/out/", "/bin/", "/obj/", "/packages/", "/generated/", "/__generated__/",
    )

    private val GENERATED_FILE_SUFFIXES = listOf(".min.js", ".min.css", ".g.dart", ".pb.go")

    // Config-file exclusion only applies to Maintainability - Security/Reliability tools like KICS exist
    // specifically to scan config/IaC files, so excluding them there would delete the category those tools
    // are for (Epic 432 design doc, section 3).
    private val CONFIG_FILE_EXTENSIONS = listOf(
        ".yml", ".yaml", ".json", ".xml", ".properties", ".toml", ".ini", ".conf", ".tf", ".tfvars",
    )
    private val CONFIG_FILE_NAMES = listOf("dockerfile", "makefile")

    private val TEST_DIRECTORY_MARKERS = listOf("/test/", "/tests/", "/__tests__/", "/spec/", "/specs/")
    private val TEST_FILE_SUFFIXES = listOf(
        "test.java", "test.kt", "tests.java", "tests.kt", "test.py", "test.go", "test.ts", "test.js",
        "test.tsx", "test.jsx", "spec.ts", "spec.js", "_test.py", "_test.go",
    )
    private val TEST_FILE_PREFIXES = listOf("test_")

    // Vendored/generated code is dropped entirely for every capability; config files are only dropped for
    // Maintainability. Fails open (returns false) when a finding has no file location to judge at all.
    fun exclude(finding: PrioritizedFinding): Boolean {
        val paths = finding.fileLocations.map { it.filePath }.filter { it.isNotBlank() }
        if (paths.isEmpty()) return false
        if (paths.any { isVendoredOrGenerated(it) }) return true
        return finding.capability == PriorityCapability.Maintainability && paths.all { isConfigFile(it) }
    }

    fun isTestCode(finding: PrioritizedFinding): Boolean {
        val paths = finding.fileLocations.map { it.filePath }.filter { it.isNotBlank() }
        return paths.any { isTestCodePath(it) }
    }

    internal fun isVendoredOrGenerated(path: String): Boolean {
        val normalized = "/${path.lowercase()}/"
        if (VENDORED_OR_GENERATED_DIRECTORY_MARKERS.any { normalized.contains(it) }) return true
        return GENERATED_FILE_SUFFIXES.any { path.endsWith(it, ignoreCase = true) }
    }

    internal fun isConfigFile(path: String): Boolean {
        val fileName = path.substringAfterLast("/").lowercase()
        if (CONFIG_FILE_EXTENSIONS.any { fileName.endsWith(it) }) return true
        return CONFIG_FILE_NAMES.any { fileName == it || fileName.startsWith("$it.") }
    }

    // Suffix/prefix checks only (never a loose `contains`) - a naive "test." substring check would
    // misclassify a file like "latest.json" as test code.
    internal fun isTestCodePath(path: String): Boolean {
        val lower = path.lowercase()
        val normalizedForDirCheck = "/$lower/"
        if (TEST_DIRECTORY_MARKERS.any { normalizedForDirCheck.contains(it) }) return true
        val fileName = lower.substringAfterLast("/")
        return TEST_FILE_SUFFIXES.any { fileName.endsWith(it) } || TEST_FILE_PREFIXES.any { fileName.startsWith(it) }
    }
}
