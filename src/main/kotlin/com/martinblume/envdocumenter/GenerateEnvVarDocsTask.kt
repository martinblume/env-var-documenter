package com.martinblume.envdocumenter

import com.martinblume.envdocumenter.readme.ReadmeMarkerException
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateEnvVarDocsTask : BaseEnvVarDocTask() {

    /** The README file that will be updated. Tracked by Gradle as an output. */
    @get:OutputFile
    abstract val readmeFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val readme = readmeFile.get().asFile

        requireReadmeExists(readme)

        val sourceFiles = collectSourceFiles()
        if (sourceFiles.isEmpty()) {
            logger.warn("env-var-documenter: No source files found in configured sourceDirs.")
        }

        val entries = parseEntries(sourceFiles)
        if (entries.isEmpty()) {
            logger.warn("env-var-documenter: No System.getenv() calls found.")
        }

        try {
            createInjector().inject(readme, entries)
        } catch (e: ReadmeMarkerException) {
            throw GradleException(e.message ?: "README marker configuration error", e)
        }

        logger.lifecycle("env-var-documenter: Documented ${entries.size} environment variable(s) in ${readme.name}.")
    }
}
