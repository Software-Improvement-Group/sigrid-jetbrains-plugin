package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class ArchitectureFindingMapperTest {
    private fun element(id: String, type: String, name: String? = null, shortName: String? = null) =
        ArchitectureElementResponse(id, type, name, shortName)

    private fun dependency(
        source: String, target: String, type: String = "CODE_CALL",
        qualification: String? = null, qualifications: List<String>? = null, lines: List<List<Int>>? = null,
    ) = ArchitectureDependencyResponse(null, type, source, target, qualification, qualifications, lines)

    private fun contains(parent: String, child: String) = dependency(parent, child, "CONTAINS")

    private val elements = listOf(
        element("c1", "CODE_COMPONENT", shortName = "app"),
        element("c2", "CODE_COMPONENT", shortName = "lib"),
        element("f1", "FILE", name = "app/src/A.kt"),
        element("f2", "FILE", name = "lib/src/B.kt"),
    )
    private val structure = listOf(contains("c1", "f1"), contains("c2", "f2"))

    private fun graph(vararg calls: ArchitectureDependencyResponse) =
        ArchitectureGraphResponse(elements, structure + calls)

    @Test
    fun `undesirable dependency is high and located at source file and line`() {
        val result = ArchitectureFindingMapper.map(graph(dependency("f1", "f2", qualification = "UNDESIRABLE", lines = listOf(listOf(7)))), "")
        val finding = result.single()
        assertEquals(RiskSeverity.High, finding.severity)
        assertEquals("Undesirable dependency from app to lib.", finding.description)
        assertEquals("app/src/A.kt", finding.filePath)
        assertEquals(7, finding.line)
    }

    @Test
    fun `cyclic dependency is medium`() {
        val result = ArchitectureFindingMapper.map(graph(dependency("f1", "f2", qualifications = listOf("CYCLIC"))), "")
        assertEquals(RiskSeverity.Medium, result.single().severity)
        assertEquals(0, result.single().line)
    }

    @Test
    fun `undesirable wins over cyclic`() {
        val result = ArchitectureFindingMapper.map(graph(dependency("f1", "f2", qualifications = listOf("CYCLIC", "UNDESIRABLE"))), "")
        assertEquals(RiskSeverity.High, result.single().severity)
    }

    @Test
    fun `ignores non-call dependencies and unqualified calls`() {
        val result = ArchitectureFindingMapper.map(
            graph(dependency("f1", "f2", "IMPORT", "UNDESIRABLE"), dependency("f1", "f2")), "")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `skips dependencies that do not resolve to files with components`() {
        val result = ArchitectureFindingMapper.map(graph(dependency("f1", "missing", qualification = "CYCLIC")), "")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filters by subsystem and strips its prefix from the path`() {
        val call = dependency("f1", "f2", qualification = "CYCLIC")
        assertEquals("src/A.kt", ArchitectureFindingMapper.map(graph(call), "app").single().filePath)
        assertTrue(ArchitectureFindingMapper.map(graph(call), "lib").isEmpty())
    }

    @Test
    fun `sorts by severity descending`() {
        val result = ArchitectureFindingMapper.map(
            graph(dependency("f1", "f2", qualification = "CYCLIC"), dependency("f2", "f1", qualification = "UNDESIRABLE")), "")
        assertEquals(listOf(RiskSeverity.High, RiskSeverity.Medium), result.map { it.severity })
    }

    @Test
    fun `missing arrays give no findings`() {
        assertTrue(ArchitectureFindingMapper.map(ArchitectureGraphResponse(null, null), "").isEmpty())
    }
}
