package com.softwareimprovementgroup.plugins.sigrid.services.onboarding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class RepositoryArchiverTest {

    @TempDir
    lateinit var root: Path

    private fun write(path: String, content: String = "x") {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    @Test
    fun listFilesByGitignore_skipsIgnoredDirectoriesAndGitDir() {
        write(".gitignore", "node_modules/\n*.log\n")
        write("src/Main.kt")
        write("node_modules/lib/index.js")
        write("debug.log")
        write(".git/config")

        val files = RepositoryArchiver.listFilesByGitignore(root).sorted()

        assertEquals(listOf(".gitignore", "src/Main.kt"), files)
    }

    @Test
    fun listFilesByGitignore_withoutGitignore_includesEverythingButGit() {
        write("a.txt")
        write(".git/HEAD")

        assertEquals(listOf("a.txt"), RepositoryArchiver.listFilesByGitignore(root))
    }

    @Test
    fun writeZip_storesRelativePathsAndContentsAndSkipsMissingFiles() {
        write("src/Main.kt", "hello")
        val target = Files.createTempFile(root, "out", ".zip")

        RepositoryArchiver.writeZip(root, listOf("src/Main.kt", "gone.txt"), target)

        ZipFile(target.toFile()).use { zip ->
            assertEquals(listOf("src/Main.kt"), zip.entries().toList().map { it.name })
            assertEquals("hello", zip.getInputStream(zip.getEntry("src/Main.kt")).readBytes().decodeToString())
        }
    }

    @Test
    fun listFiles_inGitRepository_honoursGitignore() {
        val git = ProcessBuilder("git", "init", "-q", root.toString()).start()
        if (git.waitFor() != 0) return
        write(".gitignore", "build/\n")
        write("src/Main.kt")
        write("build/out.class")

        val files = RepositoryArchiver.listFiles(root)

        assertTrue("src/Main.kt" in files)
        assertTrue(files.none { it.startsWith("build/") })
    }
}
