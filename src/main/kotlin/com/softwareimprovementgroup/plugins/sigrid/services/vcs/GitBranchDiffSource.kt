package com.softwareimprovementgroup.plugins.sigrid.services.vcs

import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

private val DEFAULT_BRANCH_CANDIDATES = listOf("origin/main", "origin/master", "main", "master")

class GitBranchDiffSource : ChangedFilesSource {
    override fun committedPaths(project: Project): Set<String> =
        GitRepositoryManager.getInstance(project).repositories.flatMapTo(mutableSetOf()) { changedIn(project, it) }

    override fun recentlyChangedPaths(project: Project): Set<String>? {
        val repositories = GitRepositoryManager.getInstance(project).repositories
        if (repositories.isEmpty()) return null
        return repositories.flatMapTo(mutableSetOf()) { touchedIn(project, it) }
    }

    // Repositories are registered asynchronously after startup, so the first read can find none.
    override fun onRepositoriesReady(project: Project, onReady: () -> Unit) {
        project.messageBus.connect().subscribe(VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED, VcsRepositoryMappingListener { onReady() })
    }

    // An empty pretty format leaves only the file names, one per line, for every commit in the window.
    private fun touchedIn(project: Project, repository: GitRepository): List<String> {
        return outputLines(project, repository, GitCommand.LOG, "--since=${ChangedFilesSource.RECENT_HISTORY_DAYS}.days.ago", "--name-only", "--pretty=format:")
    }

    // `base...HEAD` diffs against the merge-base, so only this branch's own commits are listed. On the default
    // branch itself that leaves just the unpushed commits, which is also what the developer is working on.
    private fun changedIn(project: Project, repository: GitRepository): List<String> {
        val base = defaultBranchRef(repository) ?: return emptyList()
        return outputLines(project, repository, GitCommand.DIFF, "--name-only", "$base...HEAD")
    }

    // Empty when git fails: a branch we can't read just contributes no files.
    private fun outputLines(project: Project, repository: GitRepository, command: GitCommand, vararg parameters: String): List<String> {
        val handler = GitLineHandler(project, repository.root, command).apply {
            addParameters(*parameters)
            setSilent(true)
        }
        val result = Git.getInstance().runCommand(handler)
        return if (result.success()) result.output.filter { it.isNotBlank() } else emptyList()
    }

    private fun defaultBranchRef(repository: GitRepository): String? {
        if (repository.currentRevision == null) return null
        val branches = repository.branches
        return DEFAULT_BRANCH_CANDIDATES.firstOrNull {
            branches.findRemoteBranch(it) != null || branches.findLocalBranch(it) != null
        }
    }
}
