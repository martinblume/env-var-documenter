package com.martinblume.envdocumenter

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GenerateEnvVarDocsTaskTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var task: GenerateEnvVarDocsTask
    private lateinit var srcDir: File
    private lateinit var readme: File

    @BeforeEach
    fun setUp() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.plugins.apply("io.github.martinblume.env-var-documenter")
        task = project.tasks.getByName("generateEnvVarDocs") as GenerateEnvVarDocsTask
        srcDir = File(tempDir, "src/main/kotlin").also { it.mkdirs() }
        readme = File(tempDir, "README.md")
    }

    @Test
    fun `generate throws GradleException when README does not exist`() {
        assertThrows(GradleException::class.java) { task.generate() }
    }

    @Test
    fun `generate throws GradleException when README has no markers`() {
        readme.writeText("# Title\nNo markers here.\n")
        val ex = assertThrows(GradleException::class.java) { task.generate() }
        assertTrue(ex.message!!.contains("marker"), ex.message)
    }

    @Test
    fun `generate writes env var table into README`() {
        readme.writeText("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n")
        File(srcDir, "App.kt").writeText("""val x = System.getenv("DB_HOST")""")
        task.generate()
        assertTrue(readme.readText().contains("DB_HOST"), readme.readText())
    }

    @Test
    fun `generate produces placeholder row when no getenv calls found`() {
        readme.writeText("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n")
        File(srcDir, "App.kt").writeText("val x = 42")
        task.generate()
        assertTrue(readme.readText().contains("No environment variables detected"), readme.readText())
    }

    @Test
    fun `generate succeeds with empty source directory and produces placeholder row`() {
        readme.writeText("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n")
        // srcDir exists but contains no files
        assertDoesNotThrow { task.generate() }
        assertTrue(readme.readText().contains("No environment variables detected"), readme.readText())
    }

    @Test
    fun `generate is idempotent`() {
        readme.writeText("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n")
        File(srcDir, "App.kt").writeText("""val x = System.getenv("MY_VAR")""")
        task.generate()
        val afterFirst = readme.readText()
        task.generate()
        assertEquals(afterFirst, readme.readText())
    }

    @Test
    fun `generate preserves README content outside the markers`() {
        readme.writeText("# My Project\n\n<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n\n## License\n")
        task.generate()
        val content = readme.readText()
        assertTrue(content.contains("# My Project"), content)
        assertTrue(content.contains("## License"), content)
    }
}
