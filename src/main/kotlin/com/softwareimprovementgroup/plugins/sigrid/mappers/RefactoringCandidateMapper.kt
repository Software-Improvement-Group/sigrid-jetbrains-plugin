package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import com.softwareimprovementgroup.plugins.sigrid.models.*

object RefactoringCandidateMapper {
    fun map(response: Map<RefactoringCategory, RefactoringCandidatesResponse>, subsystem: String): List<RefactoringCandidate> =
        RefactoringCategory.entries.flatMap { category ->
            mapCategory(category, response[category], subsystem)
        }

    private fun mapCategory(
        category: RefactoringCategory,
        response: RefactoringCandidatesResponse?,
        subsystem: String,
    ): List<RefactoringCandidate> =
        response?.refactoringCandidates
            ?.filter { subsystem.isBlank() || it.component == subsystem }
            ?.map { create(category, it, subsystem) }
            ?: emptyList()

    private fun create(
        category: RefactoringCategory,
        r: RefactoringCandidateResponse,
        subsystem: String,
    ): RefactoringCandidate = RefactoringCandidate(
        id = r.id,
        category = category,
        severity = MaintainabilitySeverity.from(r.severity),
        status = MaintainabilityFindingStatus.from(r.status),
        statusLabel = snakeCaseToTitleCase(r.status),
        weight = r.weight,
        technology = r.technology,
        snapshotDate = r.snapshotDate,
        name = r.name ?: "",
        mcCabe = r.mcCabe,
        fanIn = r.fanIn,
        component = r.component,
        parameters = r.parameters,
        displayLocation = displayLocation(r),
        description = description(category, r),
        fileLocations = fileLocations(category, r, subsystem),
    )

    private fun displayLocation(r: RefactoringCandidateResponse, noPathPrefix: Boolean = false): String {
        val locations = r.locations ?: emptyList()
        val prefix = if (noPathPrefix) "" else ".../"
        if (locations.isEmpty()) return toDisplayFilePath(r.file, prefix)
        val first = toDisplayFilePath(locations[0].file, prefix)
        if (locations.size == 1) return first
        val second = toDisplayFilePath(locations[1].file, prefix)
        if (locations.size == 2) return SigridBundle["mapper.location.two", first, second]
        return SigridBundle["mapper.location.many", first, second, locations.size - 2]
    }

    private fun description(category: RefactoringCategory, r: RefactoringCandidateResponse): String {
        val name = r.name?.replace(",", ", ")
        return when (category) {
            RefactoringCategory.Duplication     -> SigridBundle["mapper.description.duplication", r.weight, displayLocation(r, noPathPrefix = true)]
            RefactoringCategory.UnitSize        -> SigridBundle["mapper.description.unit.size", name ?: "", r.weight]
            RefactoringCategory.UnitComplexity  -> SigridBundle["mapper.description.unit.complexity", name ?: "", r.mcCabe ?: 0]
            RefactoringCategory.UnitInterfacing -> SigridBundle["mapper.description.unit.interfacing", name ?: "", r.parameters ?: 0]
            RefactoringCategory.ModuleCoupling  -> SigridBundle["mapper.description.module.coupling", toDisplayFilePath(r.file, ""), r.fanIn ?: 0]
        }
    }

    private fun fileLocations(
        category: RefactoringCategory,
        r: RefactoringCandidateResponse,
        subsystem: String,
    ): List<FileLocation> = when (category) {
        RefactoringCategory.Duplication ->
            r.locations?.map { loc ->
                FileLocation(loc.component, normalizePath(loc.file, subsystem), loc.startLine, loc.endLine)
            } ?: emptyList()
        RefactoringCategory.ModuleCoupling ->
            listOf(FileLocation(r.component ?: "", normalizePath(r.file, subsystem), 0, r.loc ?: 0))
        RefactoringCategory.UnitSize,
        RefactoringCategory.UnitComplexity,
        RefactoringCategory.UnitInterfacing ->
            r.lineRanges?.map { range ->
                FileLocation(r.component ?: "", normalizePath(r.file, subsystem), range.startLine, range.endLine)
            } ?: emptyList()
    }

}