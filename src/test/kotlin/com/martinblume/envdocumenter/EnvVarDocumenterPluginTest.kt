package com.martinblume.envdocumenter

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnvVarDocumenterPluginTest {

    private val project = ProjectBuilder.builder().build()

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply("com.martinblume.env-var-documenter")
    }

    // -------------------------------------------------------------------------
    // Task registration
    // -------------------------------------------------------------------------

    @Test
    fun `generateEnvVarDocs task is registered`() {
        assertNotNull(project.tasks.findByName("generateEnvVarDocs"))
    }

    @Test
    fun `verifyEnvVarDocs task is registered`() {
        assertNotNull(project.tasks.findByName("verifyEnvVarDocs"))
    }

    @Test
    fun `generateEnvVarDocs is of type GenerateEnvVarDocsTask`() {
        assertInstanceOf(GenerateEnvVarDocsTask::class.java, project.tasks.getByName("generateEnvVarDocs"))
    }

    @Test
    fun `verifyEnvVarDocs is of type VerifyEnvVarDocsTask`() {
        assertInstanceOf(VerifyEnvVarDocsTask::class.java, project.tasks.getByName("verifyEnvVarDocs"))
    }

    // -------------------------------------------------------------------------
    // Task group and description
    // -------------------------------------------------------------------------

    @Test
    fun `generateEnvVarDocs is in documentation group`() {
        assertEquals("documentation", project.tasks.getByName("generateEnvVarDocs").group)
    }

    @Test
    fun `verifyEnvVarDocs is in documentation group`() {
        assertEquals("documentation", project.tasks.getByName("verifyEnvVarDocs").group)
    }

    @Test
    fun `generateEnvVarDocs has a description`() {
        assertNotNull(project.tasks.getByName("generateEnvVarDocs").description)
    }

    @Test
    fun `verifyEnvVarDocs has a description`() {
        assertNotNull(project.tasks.getByName("verifyEnvVarDocs").description)
    }

    // -------------------------------------------------------------------------
    // Extension registration and defaults
    // -------------------------------------------------------------------------

    @Test
    fun `envVarDocumenter extension is registered`() {
        assertNotNull(project.extensions.findByName("envVarDocumenter"))
    }

    @Test
    fun `extension is of type EnvVarDocumenterExtension`() {
        assertInstanceOf(
            EnvVarDocumenterExtension::class.java,
            project.extensions.getByName("envVarDocumenter"),
        )
    }

    @Test
    fun `extension default sourceDirs is src main kotlin and src main java`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        assertEquals(listOf("src/main/kotlin", "src/main/java"), ext.sourceDirs.get())
    }

    @Test
    fun `extension default readmeFile is README md`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        assertEquals("README.md", ext.readmeFile.get())
    }

    @Test
    fun `extension default sectionStartMarker`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        assertEquals("<!-- ENV_VARS_START -->", ext.sectionStartMarker.get())
    }

    @Test
    fun `extension default sectionEndMarker`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        assertEquals("<!-- ENV_VARS_END -->", ext.sectionEndMarker.get())
    }

    // -------------------------------------------------------------------------
    // Default property wiring to tasks
    // -------------------------------------------------------------------------

    @Test
    fun `generateEnvVarDocs receives default sectionStartMarker`() {
        val task = project.tasks.getByName("generateEnvVarDocs") as GenerateEnvVarDocsTask
        assertEquals("<!-- ENV_VARS_START -->", task.sectionStartMarker.get())
    }

    @Test
    fun `generateEnvVarDocs receives default sectionEndMarker`() {
        val task = project.tasks.getByName("generateEnvVarDocs") as GenerateEnvVarDocsTask
        assertEquals("<!-- ENV_VARS_END -->", task.sectionEndMarker.get())
    }

    @Test
    fun `verifyEnvVarDocs receives default sectionStartMarker`() {
        val task = project.tasks.getByName("verifyEnvVarDocs") as VerifyEnvVarDocsTask
        assertEquals("<!-- ENV_VARS_START -->", task.sectionStartMarker.get())
    }

    @Test
    fun `verifyEnvVarDocs receives default sectionEndMarker`() {
        val task = project.tasks.getByName("verifyEnvVarDocs") as VerifyEnvVarDocsTask
        assertEquals("<!-- ENV_VARS_END -->", task.sectionEndMarker.get())
    }

    // -------------------------------------------------------------------------
    // Custom property propagation to tasks
    // -------------------------------------------------------------------------

    @Test
    fun `custom sectionStartMarker propagates to generateEnvVarDocs`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        ext.sectionStartMarker.set("<!-- MY_START -->")
        val task = project.tasks.getByName("generateEnvVarDocs") as GenerateEnvVarDocsTask
        assertEquals("<!-- MY_START -->", task.sectionStartMarker.get())
    }

    @Test
    fun `custom sectionEndMarker propagates to generateEnvVarDocs`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        ext.sectionEndMarker.set("<!-- MY_END -->")
        val task = project.tasks.getByName("generateEnvVarDocs") as GenerateEnvVarDocsTask
        assertEquals("<!-- MY_END -->", task.sectionEndMarker.get())
    }

    @Test
    fun `custom sectionStartMarker propagates to verifyEnvVarDocs`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        ext.sectionStartMarker.set("<!-- MY_START -->")
        val task = project.tasks.getByName("verifyEnvVarDocs") as VerifyEnvVarDocsTask
        assertEquals("<!-- MY_START -->", task.sectionStartMarker.get())
    }

    @Test
    fun `custom sectionEndMarker propagates to verifyEnvVarDocs`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        ext.sectionEndMarker.set("<!-- MY_END -->")
        val task = project.tasks.getByName("verifyEnvVarDocs") as VerifyEnvVarDocsTask
        assertEquals("<!-- MY_END -->", task.sectionEndMarker.get())
    }

    @Test
    fun `custom readmeFile propagates to generateEnvVarDocs`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        ext.readmeFile.set("DOCS.md")
        val task = project.tasks.getByName("generateEnvVarDocs") as GenerateEnvVarDocsTask
        assertTrue(task.readmeFile.get().asFile.name == "DOCS.md")
    }

    @Test
    fun `custom readmeFile propagates to verifyEnvVarDocs`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        ext.readmeFile.set("DOCS.md")
        val task = project.tasks.getByName("verifyEnvVarDocs") as VerifyEnvVarDocsTask
        assertTrue(task.readmeFile.get().asFile.name == "DOCS.md")
    }

    @Test
    fun `custom sourceDirs propagate to generateEnvVarDocs`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        ext.sourceDirs.set(listOf("custom/src"))
        val task = project.tasks.getByName("generateEnvVarDocs") as GenerateEnvVarDocsTask
        assertEquals(
            listOf(project.file("custom/src")),
            task.sourceDirs.files.toList(),
        )
    }

    @Test
    fun `custom sourceDirs propagate to verifyEnvVarDocs`() {
        val ext = project.extensions.getByType(EnvVarDocumenterExtension::class.java)
        ext.sourceDirs.set(listOf("custom/src"))
        val task = project.tasks.getByName("verifyEnvVarDocs") as VerifyEnvVarDocsTask
        assertEquals(
            listOf(project.file("custom/src")),
            task.sourceDirs.files.toList(),
        )
    }

    // -------------------------------------------------------------------------
    // Lifecycle wiring
    // -------------------------------------------------------------------------

    @Test
    fun `verifyEnvVarDocs is wired into check when base plugin is applied`() {
        project.plugins.apply("base")
        val checkTask = project.tasks.getByName("check")
        val deps = checkTask.taskDependencies.getDependencies(checkTask).map { it.name }
        assertTrue("verifyEnvVarDocs" in deps)
    }

    @Test
    fun `base plugin applied before env-var-documenter still wires check`() {
        val freshProject = ProjectBuilder.builder().build()
        freshProject.plugins.apply("base")
        freshProject.plugins.apply("com.martinblume.env-var-documenter")
        val checkTask = freshProject.tasks.getByName("check")
        val deps = checkTask.taskDependencies.getDependencies(checkTask).map { it.name }
        assertTrue("verifyEnvVarDocs" in deps)
    }

    @Test
    fun `no check task exists when base plugin is absent`() {
        assertNull(project.tasks.findByName("check"))
    }

    @Test
    fun `applying plugin without base plugin does not throw`() {
        // Covered by the fact that @BeforeEach succeeds, but make it explicit.
        assertDoesNotThrow {
            ProjectBuilder.builder().build().also {
                it.plugins.apply("com.martinblume.env-var-documenter")
            }
        }
    }
}
