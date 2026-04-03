package com.martinblume.envdocumenter

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Configuration DSL for the `envVarDocumenter` extension.
 *
 * Example usage in a consumer project:
 * ```kotlin
 * envVarDocumenter {
 *     sourceDirs.set(listOf("src/main/kotlin"))
 *     readmeFile.set("README.md")
 *     sectionStartMarker.set("<!-- ENV_VARS_START -->")
 *     sectionEndMarker.set("<!-- ENV_VARS_END -->")
 * }
 * ```
 *
 * All properties have sensible defaults and need only be set when deviating from them.
 */
abstract class EnvVarDocumenterExtension {

    /** Source directories to scan for Kotlin files (relative to the project root). */
    abstract val sourceDirs: ListProperty<String>

    /** Path to the README file to inject the table into (relative to the project root). */
    abstract val readmeFile: Property<String>

    /** HTML comment marker that denotes the start of the generated section. */
    abstract val sectionStartMarker: Property<String>

    /** HTML comment marker that denotes the end of the generated section. */
    abstract val sectionEndMarker: Property<String>
}
