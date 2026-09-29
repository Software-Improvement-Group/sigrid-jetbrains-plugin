package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.FileLocation
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityCapability
import com.softwareimprovementgroup.plugins.sigrid.models.PriorityRank
import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PriorityExclusionFilterTest {

    private fun makeFinding(
        capability: PriorityCapability = PriorityCapability.Maintainability,
        paths: List<String> = listOf("svc/src/main/Foo.kt"),
    ) = PrioritizedFinding(
        id = "id",
        capability = capability,
        priorityRank = PriorityRank.High,
        displayLocation = "Foo.kt",
        description = "",
        statusLabel = "",
        remark = "",
        fileLocations = paths.map { FileLocation("svc", it) },
        href = null,
        editable = false,
        statusOptions = emptyList(),
        currentStatusValue = "",
    )

    // isVendoredOrGenerated

    @Test
    fun isVendoredOrGenerated_nodeModules_isTrue() {
        assertTrue(PriorityExclusionFilter.isVendoredOrGenerated("svc/node_modules/lib/index.js"))
    }

    @Test
    fun isVendoredOrGenerated_buildOutputDir_isTrue() {
        assertTrue(PriorityExclusionFilter.isVendoredOrGenerated("svc/build/classes/Foo.class"))
    }

    @Test
    fun isVendoredOrGenerated_minifiedFile_isTrue() {
        assertTrue(PriorityExclusionFilter.isVendoredOrGenerated("svc/static/app.min.js"))
    }

    @Test
    fun isVendoredOrGenerated_normalSourceFile_isFalse() {
        assertFalse(PriorityExclusionFilter.isVendoredOrGenerated("svc/src/main/Foo.kt"))
    }

    // isConfigFile

    @Test
    fun isConfigFile_yaml_isTrue() {
        assertTrue(PriorityExclusionFilter.isConfigFile("svc/application.yml"))
    }

    @Test
    fun isConfigFile_dockerfile_isTrue() {
        assertTrue(PriorityExclusionFilter.isConfigFile("svc/Dockerfile"))
    }

    @Test
    fun isConfigFile_javaSource_isFalse() {
        assertFalse(PriorityExclusionFilter.isConfigFile("svc/src/main/Foo.java"))
    }

    // isTestCodePath

    @Test
    fun isTestCodePath_testDirectory_isTrue() {
        assertTrue(PriorityExclusionFilter.isTestCodePath("svc/src/test/java/FooTest.java"))
    }

    @Test
    fun isTestCodePath_testSuffixFileName_isTrue() {
        assertTrue(PriorityExclusionFilter.isTestCodePath("svc/src/main/FooTest.kt"))
    }

    @Test
    fun isTestCodePath_testPrefixFileName_isTrue() {
        assertTrue(PriorityExclusionFilter.isTestCodePath("svc/src/main/test_foo.py"))
    }

    @Test
    fun isTestCodePath_looseSubstringMatch_isFalse() {
        // Regression guard: a naive "contains(test.)" check would misclassify this.
        assertFalse(PriorityExclusionFilter.isTestCodePath("svc/src/main/latest.json"))
    }

    @Test
    fun isTestCodePath_normalSourceFile_isFalse() {
        assertFalse(PriorityExclusionFilter.isTestCodePath("svc/src/main/Foo.kt"))
    }

    // exclude (capability-aware)

    @Test
    fun exclude_vendoredPath_anyCapability_isTrue() {
        assertTrue(PriorityExclusionFilter.exclude(makeFinding(capability = PriorityCapability.Security, paths = listOf("svc/node_modules/lib/index.js"))))
    }

    @Test
    fun exclude_configFile_maintainability_isTrue() {
        assertTrue(PriorityExclusionFilter.exclude(makeFinding(capability = PriorityCapability.Maintainability, paths = listOf("svc/application.yml"))))
    }

    @Test
    fun exclude_configFile_security_isFalse() {
        assertFalse(PriorityExclusionFilter.exclude(makeFinding(capability = PriorityCapability.Security, paths = listOf("svc/application.yml"))))
    }

    @Test
    fun exclude_normalFile_isFalse() {
        assertFalse(PriorityExclusionFilter.exclude(makeFinding()))
    }

    @Test
    fun exclude_noFileLocations_isFalse() {
        assertFalse(PriorityExclusionFilter.exclude(makeFinding(paths = emptyList())))
    }

    // isTestCode (finding-level)

    @Test
    fun isTestCode_testPath_isTrue() {
        assertTrue(PriorityExclusionFilter.isTestCode(makeFinding(paths = listOf("svc/src/test/FooTest.kt"))))
    }

    @Test
    fun isTestCode_mainPath_isFalse() {
        assertFalse(PriorityExclusionFilter.isTestCode(makeFinding(paths = listOf("svc/src/main/Foo.kt"))))
    }
}
