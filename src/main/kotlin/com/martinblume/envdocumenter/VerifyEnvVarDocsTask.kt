package com.martinblume.envdocumenter

import com.martinblume.envdocumenter.readme.ReadmeMarkerException
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Verifies README state without producing output files; always re-runs.")
abstract class VerifyEnvVarDocsTask : BaseEnvVarDocTask() {

    /** The README file to check. Read-only — this task never writes to it. */
    @get:InputFile
    abstract val readmeFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val readme = readmeFile.get().asFile

        requireReadmeExists(readme)

        val entries = parseEntries(collectKotlinFiles())

        try {
            val diff = createInjector().verify(readme, entries)
            if (diff != null) {
                throw GradleException(
                    "env-var-documenter: README is out of date. " +
                    "Run ./gradlew generateEnvVarDocs to update it.\n$diff"
                )
            }
        } catch (e: ReadmeMarkerException) {
            throw GradleException(e.message ?: "README marker configuration error", e)
        }

        logger.lifecycle("env-var-documenter: README env-var section is up to date.")
    }
}
