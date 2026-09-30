package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.PrioritizedFinding
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCandidate
import com.softwareimprovementgroup.plugins.sigrid.models.RefactoringCategory

// GROUP stage of the Epic 432 prioritization pipeline (design doc section 3): a per-file lookup, plus
// Pascal's "duplication-first" sequencing - fixing Duplication findings first often clears other
// Maintainability findings in the same files too, but only on systems where that pattern actually holds
// (design doc section 2.8: confirmed 76.2%-95.6% on hand-written backends, down to 0% on Terraform).
object PriorityGrouper {

    // Below this, promoting Duplication findings ahead of severity-ranked ones isn't backed by enough
    // evidence that fixing them actually clears other findings too. Deliberately not gated on
    // technologyCategory alone - the design doc found the frontend cluster's confidence went *down*
    // (29.8%-80%, wide unexplained variance) after adding a real SIG system, so this must be computed
    // live per system, not assumed from a category label.
    private const val DUPLICATION_FIRST_THRESHOLD = 0.5

    fun groupByFile(findings: List<PrioritizedFinding>): Map<String, List<PrioritizedFinding>> =
        findings
            .flatMap { finding -> finding.fileLocations.map { it.filePath to finding } }
            .filter { (path, _) -> path.isNotBlank() }
            .groupBy({ it.first }, { it.second })

    // Of the files that have a Duplication finding, what fraction also have another Maintainability
    // finding in them? High enough, and fixing Duplication there is likely to clear those other findings
    // for free (Pascal's rule) - low, and Duplication findings should just be ranked by severity like
    // everything else (even though that severity is itself uninformative, per section 2.3).
    fun duplicationFirstEligible(maintainability: List<RefactoringCandidate>): Boolean {
        val duplicationFiles = filePathsFor(maintainability) { it.category == RefactoringCategory.Duplication }
        if (duplicationFiles.isEmpty()) return false
        val otherFiles = filePathsFor(maintainability) { it.category != RefactoringCategory.Duplication }

        val overlap = duplicationFiles.count { it in otherFiles }
        return overlap.toDouble() / duplicationFiles.size >= DUPLICATION_FIRST_THRESHOLD
    }

    // Ranks Duplication candidates by magnitude (weight - Sigrid's own "size of problem" number, since
    // Duplication's severity is always VERY_HIGH and has zero discriminating power - section 2.3), but
    // round-robins across components so one directory's cluster can't monopolize the top of the list.
    // Confirmed real on Airflow: a straight size-first sort put ~12 entries from one GCP provider
    // directory at the very top (section 3).
    fun sequenceDuplicationFirst(candidates: List<RefactoringCandidate>): List<RefactoringCandidate> {
        val queues = candidates
            .filter { it.category == RefactoringCategory.Duplication }
            .sortedByDescending { it.weight }
            .groupBy { it.component.orEmpty() }
            .values
            .sortedByDescending { it.first().weight }
            .map { ArrayDeque(it) }

        return roundRobin(queues)
    }

    private fun roundRobin(queues: List<ArrayDeque<RefactoringCandidate>>): List<RefactoringCandidate> {
        val result = mutableListOf<RefactoringCandidate>()
        while (queues.any { it.isNotEmpty() }) {
            for (queue in queues) {
                if (queue.isNotEmpty()) result.add(queue.removeFirst())
            }
        }
        return result
    }

    private fun filePathsFor(candidates: List<RefactoringCandidate>, predicate: (RefactoringCandidate) -> Boolean): Set<String> =
        candidates.filter(predicate).flatMap { it.fileLocations.map { loc -> loc.filePath } }.filter { it.isNotBlank() }.toSet()
}
