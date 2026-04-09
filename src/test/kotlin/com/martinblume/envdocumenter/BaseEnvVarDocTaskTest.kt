package com.martinblume.envdocumenter

import com.martinblume.envdocumenter.model.EnvVarEntry
import com.martinblume.envdocumenter.readme.ReadmeInjector
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

// Concrete subclass used exclusively for testing; Gradle's decorator provides the
// abstract property implementations at runtime. Public helpers expose the protected methods.
internal abstract class ConcreteTestTask : BaseEnvVarDocTask() {
    @TaskAction fun run() {}
    fun callRequireReadmeExists(f: File) = requireReadmeExists(f)
    fun callCollectSourceFiles(): List<File> = collectSourceFiles()
    fun callParseEntries(files: List<File>): List<EnvVarEntry> = parseEntries(files)
    fun callCreateInjector(): ReadmeInjector = createInjector()
}

class BaseEnvVarDocTaskTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var task: ConcreteTestTask

    @BeforeEach
    fun setUp() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        task = project.tasks.register("t", ConcreteTestTask::class.java).get()
        task.sectionStartMarker.set("<!-- START -->")
        task.sectionEndMarker.set("<!-- END -->")
        task.projectDir.set(project.layout.projectDirectory)
        task.sourceDirs.setFrom(emptyList<File>())
    }

    // -------------------------------------------------------------------------
    // requireReadmeExists
    // -------------------------------------------------------------------------

    @Test
    fun `requireReadmeExists does not throw when file exists`() {
        val readme = File(tempDir, "README.md").also { it.writeText("") }
        assertDoesNotThrow { task.callRequireReadmeExists(readme) }
    }

    @Test
    fun `requireReadmeExists throws GradleException when file is missing`() {
        assertThrows(GradleException::class.java) {
            task.callRequireReadmeExists(File(tempDir, "MISSING.md"))
        }
    }

    @Test
    fun `requireReadmeExists error message includes the file path`() {
        val missing = File(tempDir, "MISSING.md")
        val ex = assertThrows(GradleException::class.java) { task.callRequireReadmeExists(missing) }
        assertTrue(ex.message!!.contains(missing.absolutePath), ex.message)
    }

    @Test
    fun `requireReadmeExists error message includes start marker`() {
        val ex = assertThrows(GradleException::class.java) {
            task.callRequireReadmeExists(File(tempDir, "MISSING.md"))
        }
        assertTrue(ex.message!!.contains("<!-- START -->"), ex.message)
    }

    @Test
    fun `requireReadmeExists error message includes end marker`() {
        val ex = assertThrows(GradleException::class.java) {
            task.callRequireReadmeExists(File(tempDir, "MISSING.md"))
        }
        assertTrue(ex.message!!.contains("<!-- END -->"), ex.message)
    }

    // -------------------------------------------------------------------------
    // collectSourceFiles
    // -------------------------------------------------------------------------

    @Test
    fun `collectSourceFiles returns empty list for empty directory`() {
        val srcDir = File(tempDir, "src").also { it.mkdirs() }
        task.sourceDirs.setFrom(srcDir)
        assertTrue(task.callCollectSourceFiles().isEmpty())
    }

    @Test
    fun `collectSourceFiles returns kt files from directory`() {
        val srcDir = File(tempDir, "src").also { it.mkdirs() }
        val ktFile = File(srcDir, "App.kt").also { it.writeText("val x = 1") }
        task.sourceDirs.setFrom(srcDir)
        assertEquals(listOf(ktFile), task.callCollectSourceFiles())
    }

    @Test
    fun `collectSourceFiles returns java files from directory`() {
        val srcDir = File(tempDir, "src").also { it.mkdirs() }
        val javaFile = File(srcDir, "App.java").also { it.writeText("class App {}") }
        task.sourceDirs.setFrom(srcDir)
        assertEquals(listOf(javaFile), task.callCollectSourceFiles())
    }

    @Test
    fun `collectSourceFiles returns both kt and java files`() {
        val srcDir = File(tempDir, "src").also { it.mkdirs() }
        File(srcDir, "App.kt").writeText("val x = 1")
        File(srcDir, "Config.java").writeText("class Config {}")
        task.sourceDirs.setFrom(srcDir)
        assertEquals(2, task.callCollectSourceFiles().size)
    }

    @Test
    fun `collectSourceFiles skips non-kt and non-java files`() {
        val srcDir = File(tempDir, "src").also { it.mkdirs() }
        File(srcDir, "config.xml").writeText("<config/>")
        File(srcDir, "notes.txt").writeText("notes")
        task.sourceDirs.setFrom(srcDir)
        assertTrue(task.callCollectSourceFiles().isEmpty())
    }

    @Test
    fun `collectSourceFiles collects recursively from subdirectories`() {
        val srcDir = File(tempDir, "src").also { it.mkdirs() }
        val subDir = File(srcDir, "sub").also { it.mkdirs() }
        val ktFile = File(subDir, "Deep.kt").also { it.writeText("val x = 1") }
        task.sourceDirs.setFrom(srcDir)
        assertEquals(listOf(ktFile), task.callCollectSourceFiles())
    }

    @Test
    fun `collectSourceFiles collects from multiple source directories`() {
        val dir1 = File(tempDir, "src1").also { it.mkdirs() }
        val dir2 = File(tempDir, "src2").also { it.mkdirs() }
        File(dir1, "A.kt").writeText("val a = 1")
        File(dir2, "B.java").writeText("class B {}")
        task.sourceDirs.setFrom(dir1, dir2)
        assertEquals(2, task.callCollectSourceFiles().size)
    }

    @Test
    fun `collectSourceFiles skips entries that are not directories`() {
        val plainFile = File(tempDir, "notadir.kt").also { it.writeText("val x = 1") }
        task.sourceDirs.setFrom(plainFile)
        assertTrue(task.callCollectSourceFiles().isEmpty())
    }

    // -------------------------------------------------------------------------
    // createInjector
    // -------------------------------------------------------------------------

    @Test
    fun `createInjector returns a non-null ReadmeInjector`() {
        assertNotNull(task.callCreateInjector())
    }

    @Test
    fun `createInjector uses configured start marker`() {
        val readme = File(tempDir, "README.md").also { it.writeText("<!-- START -->\n<!-- END -->") }
        assertDoesNotThrow { task.callCreateInjector().inject(readme, emptyList()) }
        assertTrue(readme.readText().contains("<!-- START -->"))
    }

    @Test
    fun `createInjector uses configured end marker`() {
        val readme = File(tempDir, "README.md").also { it.writeText("<!-- START -->\n<!-- END -->") }
        assertDoesNotThrow { task.callCreateInjector().inject(readme, emptyList()) }
        assertTrue(readme.readText().contains("<!-- END -->"))
    }

    // -------------------------------------------------------------------------
    // parseEntries
    // -------------------------------------------------------------------------

    @Test
    fun `parseEntries returns empty list for no files`() {
        assertTrue(task.callParseEntries(emptyList()).isEmpty())
    }

    @Test
    fun `parseEntries returns entry for getenv call`() {
        val srcFile = File(tempDir, "App.kt").also {
            it.writeText("""val x = System.getenv("MY_VAR")""")
        }
        val entries = task.callParseEntries(listOf(srcFile))
        assertEquals(1, entries.size)
        assertEquals("MY_VAR", entries[0].name)
    }
}
