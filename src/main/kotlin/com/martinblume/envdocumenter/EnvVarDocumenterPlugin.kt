package com.martinblume.envdocumenter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class EnvVarDocumenterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Create the extension and wire in conventions (defaults).
        val extension = project.extensions.create<EnvVarDocumenterExtension>("envVarDocumenter")
        extension.sourceDirs.convention(listOf("src/main/kotlin"))
        extension.readmeFile.convention("README.md")
        extension.sectionStartMarker.convention("<!-- ENV_VARS_START -->")
        extension.sectionEndMarker.convention("<!-- ENV_VARS_END -->")

        project.tasks.register<GenerateEnvVarDocsTask>("generateEnvVarDocs") {
            group = "documentation"
            description = "Scans Kotlin sources for System.getenv() calls and updates README.md."

            sourceDirs.setFrom(
                extension.sourceDirs.map { dirs -> dirs.map { project.file(it) } }
            )
            readmeFile.set(
                extension.readmeFile.map { project.layout.projectDirectory.file(it) }
            )
            sectionStartMarker.set(extension.sectionStartMarker)
            sectionEndMarker.set(extension.sectionEndMarker)
            projectDir.set(project.layout.projectDirectory)
        }

        project.tasks.register<VerifyEnvVarDocsTask>("verifyEnvVarDocs") {
            group = "documentation"
            description = "Checks that the README env-var section is up to date. Fails if generateEnvVarDocs would change anything."

            sourceDirs.setFrom(
                extension.sourceDirs.map { dirs -> dirs.map { project.file(it) } }
            )
            readmeFile.set(
                extension.readmeFile.map { project.layout.projectDirectory.file(it) }
            )
            sectionStartMarker.set(extension.sectionStartMarker)
            sectionEndMarker.set(extension.sectionEndMarker)
            projectDir.set(project.layout.projectDirectory)
        }

        project.plugins.withId("base") {
            project.tasks.getByName("check").dependsOn("verifyEnvVarDocs")
        }
    }
}
