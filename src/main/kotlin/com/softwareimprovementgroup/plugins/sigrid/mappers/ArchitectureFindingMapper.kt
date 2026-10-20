package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*

/**
 * Derives architecture findings from the raw architecture graph: every call dependency between two
 * files that Sigrid qualified as undesirable or cyclic, expressed on the level of their components.
 */
object ArchitectureFindingMapper {
    private class QualificationRule(
        val name: String,
        val severity: RiskSeverity,
        val describe: (source: String, target: String) -> String,
    )

    /** Order matters: a dependency that is both undesirable and cyclic is reported as undesirable. */
    private val QUALIFICATION_RULES = listOf(
        QualificationRule("UNDESIRABLE", RiskSeverity.High) { source, target -> "Undesirable dependency from $source to $target." },
        QualificationRule("CYCLIC", RiskSeverity.Medium) { source, target -> "Cyclic dependency between $source and $target." },
    )

    private val CALL_DEPENDENCY_TYPES = setOf("CODE_CALL", "DATA_ACCESS", "INTERFACE_CALL")

    private class GraphIndex(
        val files: Map<String?, ArchitectureElementResponse>,
        val components: Map<String?, ArchitectureElementResponse>,
        val parentIds: Map<String?, String?>,
    )

    private class DependencyLink(
        val sourceFile: ArchitectureElementResponse,
        val source: ArchitectureElementResponse,
        val target: ArchitectureElementResponse,
    )

    fun map(response: ArchitectureGraphResponse, subsystem: String): List<ArchitectureFinding> {
        val elements = response.systemElements ?: return emptyList()
        val dependencies = response.dependencies ?: return emptyList()
        val index = buildIndex(elements, dependencies)

        return dependencies
            .filter { it.type in CALL_DEPENDENCY_TYPES }
            .mapNotNull { createFinding(it, index) }
            .filter { subsystem.isBlank() || it.sourceComponent == subsystem }
            .map { it.withPathRelativeTo(subsystem) }
            .sortedWith(compareByDescending<ArchitectureFinding> { it.severity }.thenBy { it.description })
    }

    private fun buildIndex(elements: List<ArchitectureElementResponse>, dependencies: List<ArchitectureDependencyResponse>) =
        GraphIndex(
            files = elements.filter { it.type == "FILE" }.associateBy { it.id },
            components = elements.filter { it.type == "CODE_COMPONENT" }.associateBy { it.id },
            parentIds = dependencies.filter { it.type == "CONTAINS" }.associate { it.targetElementId to it.sourceElementId },
        )

    private fun createFinding(dependency: ArchitectureDependencyResponse, index: GraphIndex): ArchitectureFinding? {
        val rule = findRule(dependency) ?: return null
        val link = resolveLink(dependency, index) ?: return null
        return buildFinding(dependency, rule, link)
    }

    private fun resolveLink(dependency: ArchitectureDependencyResponse, index: GraphIndex): DependencyLink? {
        val sourceFile = index.files[dependency.sourceElementId] ?: return null
        if (dependency.targetElementId !in index.files) return null
        val source = componentOf(dependency.sourceElementId, index) ?: return null
        val target = componentOf(dependency.targetElementId, index) ?: return null
        return DependencyLink(sourceFile, source, target)
    }

    private fun componentOf(fileId: String?, index: GraphIndex) = index.components[index.parentIds[fileId]]

    private fun buildFinding(dependency: ArchitectureDependencyResponse, rule: QualificationRule, link: DependencyLink): ArchitectureFinding {
        val sourceName = displayName(link.source)
        val targetName = displayName(link.target)
        val line = dependency.lines?.firstOrNull()?.firstOrNull() ?: 0
        return locatedAt(
            ArchitectureFinding(
                id = dependency.id ?: "${dependency.sourceElementId}->${dependency.targetElementId}",
                severity = rule.severity,
                description = rule.describe(sourceName, targetName),
                sourceComponent = sourceName,
                targetComponent = targetName,
                filePath = "",
                displayFilePath = "",
                line = line,
                fileLocations = emptyList(),
            ),
            link.sourceFile.name ?: link.sourceFile.shortName,
        )
    }

    private fun ArchitectureFinding.withPathRelativeTo(subsystem: String) = locatedAt(this, normalizePath(filePath, subsystem))

    private fun locatedAt(finding: ArchitectureFinding, path: String?): ArchitectureFinding {
        val filePath = path.orEmpty()
        return finding.copy(
            filePath = filePath,
            displayFilePath = toDisplayFilePath(filePath),
            fileLocations = listOf(FileLocation(finding.sourceComponent, filePath, finding.line, finding.line)),
        )
    }

    /** Sigrid writes the qualification as a list in the graph export and as a single value in its docs. */
    private fun findRule(dependency: ArchitectureDependencyResponse): QualificationRule? {
        val qualifications = (dependency.qualifications ?: emptyList()) + listOfNotNull(dependency.qualification)
        return QUALIFICATION_RULES.firstOrNull { it.name in qualifications }
    }

    private fun displayName(element: ArchitectureElementResponse) = element.shortName ?: element.name ?: element.id.orEmpty()
}
