package com.softwareimprovementgroup.plugins.sigrid.services.onboarding

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Zips a repository for upload, leaving out whatever `.gitignore` excludes (and `.git` itself). */
internal object RepositoryArchiver {

    private const val GIT_TIMEOUT_MS = 60_000

    /** The caller owns the returned temp file and must delete it. */
    fun createArchive(root: Path): Path {
        val archive = Files.createTempFile("sigrid-onboarding", ".zip")
        try {
            writeZip(root, listFiles(root), archive)
        } catch (e: Exception) {
            Files.deleteIfExists(archive)
            throw e
        }
        return archive
    }

    fun listFiles(root: Path): List<String> = listGitFiles(root) ?: listFilesByGitignore(root)

    private fun listGitFiles(root: Path): List<String>? {
        val command = GeneralCommandLine("git", "ls-files", "--cached", "--others", "--exclude-standard", "-z")
            .withWorkDirectory(root.toFile())
            .withCharset(StandardCharsets.UTF_8)
        val output = try {
            CapturingProcessHandler(command).runProcess(GIT_TIMEOUT_MS)
        } catch (_: ExecutionException) {
            return null
        }
        if (output.exitCode != 0) return null
        return output.stdout.split('\u0000').filter { it.isNotEmpty() }.distinct()
    }

    internal fun listFilesByGitignore(root: Path): List<String> {
        val visitor = GitignoreVisitor(root, GitignoreMatcher(readGitignore(root)))
        Files.walkFileTree(root, visitor)
        return visitor.files
    }

    private fun readGitignore(root: Path): List<String> {
        val gitignore = root.resolve(".gitignore")
        return if (Files.isRegularFile(gitignore)) Files.readAllLines(gitignore, StandardCharsets.UTF_8) else emptyList()
    }

    internal fun writeZip(root: Path, files: List<String>, target: Path) {
        ZipOutputStream(Files.newOutputStream(target).buffered()).use { zip ->
            val writer = ZipEntryWriter(root, zip)
            files.forEach(writer::add)
        }
    }
}

/** Collects the files below [root] that [matcher] does not ignore, never descending into `.git`. */
private class GitignoreVisitor(private val root: Path, private val matcher: GitignoreMatcher) : SimpleFileVisitor<Path>() {
    val files = mutableListOf<String>()

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (dir == root) return FileVisitResult.CONTINUE
        val skip = dir.fileName.toString() == ".git" || matcher.isIgnored(relative(dir), true)
        return if (skip) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        val rel = relative(file)
        if (attrs.isRegularFile && !matcher.isIgnored(rel, false)) files.add(rel)
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE

    private fun relative(path: Path): String = root.relativize(path).joinToString("/")
}

private class ZipEntryWriter(private val root: Path, private val zip: ZipOutputStream) {
    /** Skips anything that is not a regular file: deleted-but-tracked files, symlinks, submodule directories. */
    fun add(relativePath: String) {
        val file = root.resolve(relativePath)
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return
        zip.putNextEntry(ZipEntry(relativePath))
        Files.copy(file, zip)
        zip.closeEntry()
    }
}
