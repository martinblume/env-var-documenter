package com.martinblume.envdocumenter.functional

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EnvVarDocumenterFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private fun setup(
        sourceContent: String,
        readmeContent: String = "# Readme\n\n<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n",
        extraConfig: String = "",
    ) {
        File(testProjectDir, "settings.gradle.kts").writeText(
            """rootProject.name = "test-project""""
        )
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.martinblume.env-var-documenter")
            }
            $extraConfig
            """.trimIndent()
        )
        File(testProjectDir, "README.md").writeText(readmeContent)
        val srcDir = File(testProjectDir, "src/main/kotlin").also { it.mkdirs() }
        File(srcDir, "App.kt").writeText(sourceContent)
    }

    private fun run(vararg args: String) = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments(*args, "--stacktrace")
        .withPluginClasspath()
        .build()

    private fun runAndFail(vararg args: String) = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments(*args, "--stacktrace")
        .withPluginClasspath()
        .buildAndFail()

    @Test
    fun `task succeeds and injects table for literal getenv`() {
        setup("""
            /** The database hostname. */
            val host = System.getenv("DB_HOST") ?: "localhost"
        """.trimIndent())

        val result = run("generateEnvVarDocs")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateEnvVarDocs")?.outcome)

        val readme = File(testProjectDir, "README.md").readText()
        assertTrue(readme.contains("| DB_HOST | The database hostname. | no | localhost |"), readme)
    }

    @Test
    fun `task succeeds and resolves constant reference`() {
        setup("""
            const val DB_URL_KEY = "DATABASE_URL"
            /** Connection string. */
            val url = System.getenv(DB_URL_KEY) ?: throw IllegalStateException("missing")
        """.trimIndent())

        val result = run("generateEnvVarDocs")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateEnvVarDocs")?.outcome)

        val readme = File(testProjectDir, "README.md").readText()
        assertTrue(readme.contains("DATABASE_URL"), readme)
        assertTrue(readme.contains("yes"), readme)
    }

    @Test
    fun `warns when constant reference cannot be resolved`() {
        setup("""val x = System.getenv(UNDEFINED_CONST)""")

        val result = run("generateEnvVarDocs", "--warning-mode=all")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateEnvVarDocs")?.outcome)
        assertTrue(
            result.output.contains("Unresolved constant reference 'UNDEFINED_CONST'"),
            "Expected unresolved-constant warning in output:\n${result.output}"
        )
    }

    @Test
    fun `does not warn about unresolved constant when all references resolve`() {
        setup("""
            const val DB_HOST_KEY = "DB_HOST"
            val host = System.getenv(DB_HOST_KEY) ?: "localhost"
        """.trimIndent())

        val result = run("generateEnvVarDocs", "--warning-mode=all")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateEnvVarDocs")?.outcome)
        assertFalse(
            result.output.contains("Unresolved constant reference"),
            "Expected no unresolved-constant warning in output:\n${result.output}"
        )
    }

    @Test
    fun `task is up-to-date on second run`() {
        setup("""val x = System.getenv("MY_VAR")""")
        run("generateEnvVarDocs")
        val result = run("generateEnvVarDocs")
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateEnvVarDocs")?.outcome)
    }

    @Test
    fun `task fails gracefully when README has no markers`() {
        setup(
            sourceContent = """val x = System.getenv("MY_VAR")""",
            readmeContent = "# Readme without markers\n",
        )
        val result = runAndFail("generateEnvVarDocs")
        assertTrue(result.output.contains("Start marker"))
    }

    @Test
    fun `task reports warning when no kotlin files found`() {
        setup(sourceContent = "")
        // Remove the kotlin source dir entirely
        File(testProjectDir, "src").deleteRecursively()

        val result = run("generateEnvVarDocs")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateEnvVarDocs")?.outcome)
    }

    @Test
    fun `custom sourceDirs configuration is respected`() {
        File(testProjectDir, "settings.gradle.kts").writeText(
            """rootProject.name = "test-project""""
        )
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins { id("com.martinblume.env-var-documenter") }
            envVarDocumenter {
                sourceDirs.set(listOf("custom/src"))
            }
            """.trimIndent()
        )
        File(testProjectDir, "README.md").writeText("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n")
        val customSrc = File(testProjectDir, "custom/src").also { it.mkdirs() }
        File(customSrc, "Config.kt").writeText("""val x = System.getenv("CUSTOM_VAR")""")

        val result = run("generateEnvVarDocs")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateEnvVarDocs")?.outcome)
        assertTrue(File(testProjectDir, "README.md").readText().contains("CUSTOM_VAR"))
    }

    @Test
    fun `task is in documentation group`() {
        setup(sourceContent = "")
        val result = run("tasks", "--group=documentation")
        assertTrue(result.output.contains("generateEnvVarDocs"))
    }

    @Test
    fun `verifyEnvVarDocs passes when readme is up to date`() {
        setup("""val host = System.getenv("DB_HOST")""")
        run("generateEnvVarDocs")
        val result = run("verifyEnvVarDocs")
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifyEnvVarDocs")?.outcome)
    }

    @Test
    fun `verifyEnvVarDocs fails when readme is stale`() {
        setup(
            sourceContent = """val host = System.getenv("DB_HOST")""",
            readmeContent = "# Readme\n\n<!-- ENV_VARS_START -->\n| stale | content |\n<!-- ENV_VARS_END -->\n",
        )
        val result = runAndFail("verifyEnvVarDocs")
        assertTrue(result.output.contains("out of date"), result.output)
    }

    @Test
    fun `verifyEnvVarDocs does not modify readme`() {
        setup(
            sourceContent = """val host = System.getenv("DB_HOST")""",
            readmeContent = "# Readme\n\n<!-- ENV_VARS_START -->\n| stale | content |\n<!-- ENV_VARS_END -->\n",
        )
        val before = File(testProjectDir, "README.md").readText()
        runAndFail("verifyEnvVarDocs")
        val after = File(testProjectDir, "README.md").readText()
        assertEquals(before, after)
    }

    @Test
    fun `verifyEnvVarDocs is in documentation group`() {
        setup(sourceContent = "")
        val result = run("tasks", "--group=documentation")
        assertTrue(result.output.contains("verifyEnvVarDocs"))
    }
}
