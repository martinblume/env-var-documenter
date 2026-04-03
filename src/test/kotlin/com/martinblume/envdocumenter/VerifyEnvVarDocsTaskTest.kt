package com.martinblume.envdocumenter

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class VerifyEnvVarDocsTaskTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var verifyTask: VerifyEnvVarDocsTask
    private lateinit var generateTask: GenerateEnvVarDocsTask
    private lateinit var srcDir: File
    private lateinit var readme: File

    @BeforeEach
    fun setUp() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.plugins.apply("com.martinblume.env-var-documenter")
        verifyTask = project.tasks.getByName("verifyEnvVarDocs") as VerifyEnvVarDocsTask
        generateTask = project.tasks.getByName("generateEnvVarDocs") as GenerateEnvVarDocsTask
        srcDir = File(tempDir, "src/main/kotlin").also { it.mkdirs() }
        readme = File(tempDir, "README.md")
    }

    @Test
    fun `verify throws GradleException when README does not exist`() {
        assertThrows(GradleException::class.java) { verifyTask.verify() }
    }

    @Test
    fun `verify throws GradleException when README has no markers`() {
        readme.writeText("# Title\nNo markers here.\n")
        val ex = assertThrows(GradleException::class.java) { verifyTask.verify() }
        assertTrue(ex.message!!.contains("marker"), ex.message)
    }

    @Test
    fun `verify throws GradleException when README is stale`() {
        readme.writeText("<!-- ENV_VARS_START -->\n| stale | content |\n<!-- ENV_VARS_END -->\n")
        File(srcDir, "App.kt").writeText("""val x = System.getenv("DB_HOST")""")
        val ex = assertThrows(GradleException::class.java) { verifyTask.verify() }
        assertTrue(ex.message!!.contains("out of date"), ex.message)
    }

    @Test
    fun `verify does not throw when README is up to date`() {
        readme.writeText("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n")
        File(srcDir, "App.kt").writeText("""val x = System.getenv("DB_HOST")""")
        generateTask.generate()
        assertDoesNotThrow { verifyTask.verify() }
    }

    @Test
    fun `verify never modifies the README`() {
        val originalContent = "<!-- ENV_VARS_START -->\n| stale | content |\n<!-- ENV_VARS_END -->\n"
        readme.writeText(originalContent)
        File(srcDir, "App.kt").writeText("""val x = System.getenv("DB_HOST")""")
        assertThrows(GradleException::class.java) { verifyTask.verify() }
        assertEquals(originalContent, readme.readText())
    }

    @Test
    fun `verify passes when source has no env vars and README has placeholder row`() {
        readme.writeText("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n")
        generateTask.generate()
        assertDoesNotThrow { verifyTask.verify() }
    }

    @Test
    fun `verify stale error message suggests generateEnvVarDocs`() {
        readme.writeText("<!-- ENV_VARS_START -->\n| stale | content |\n<!-- ENV_VARS_END -->\n")
        val ex = assertThrows(GradleException::class.java) { verifyTask.verify() }
        assertTrue(ex.message!!.contains("generateEnvVarDocs"), ex.message)
    }
}
