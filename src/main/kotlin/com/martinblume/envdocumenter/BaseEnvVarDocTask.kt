package com.martinblume.envdocumenter

import com.martinblume.envdocumenter.model.EnvVarEntry
import com.martinblume.envdocumenter.parser.EnvVarParser
import com.martinblume.envdocumenter.readme.ReadmeInjector
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import java.io.File

abstract class BaseEnvVarDocTask : DefaultTask() {

    /** Kotlin source directories to scan. Tracked by Gradle for up-to-date checking. */
    @get:InputFiles
    abstract val sourceDirs: ConfigurableFileCollection

    @get:Input
    abstract val sectionStartMarker: Property<String>

    @get:Input
    abstract val sectionEndMarker: Property<String>

    /** Project root directory used to compute relative source-file paths. Not a task input. */
    @get:Internal
    abstract val projectDir: DirectoryProperty

    protected fun requireReadmeExists(readme: File) {
        if (!readme.exists()) {
            throw GradleException(
                "env-var-documenter: README file not found at ${readme.absolutePath}. " +
                "Create the file and add the markers \"${sectionStartMarker.get()}\" and " +
                "\"${sectionEndMarker.get()}\"."
            )
        }
    }

    protected fun collectKotlinFiles(): List<File> =
        sourceDirs
            .filter { it.isDirectory }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }

    protected fun parseEntries(kotlinFiles: List<File>): List<EnvVarEntry> =
        EnvVarParser().parse(kotlinFiles, projectDir.get().asFile)

    protected fun createInjector(): ReadmeInjector = ReadmeInjector(
        startMarker = sectionStartMarker.get(),
        endMarker = sectionEndMarker.get(),
    )
}
