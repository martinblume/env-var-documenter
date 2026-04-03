package com.martinblume.envdocumenter.model

/**
 * Represents a single discovered environment variable and all its metadata.
 *
 * @property name        The variable name, e.g. "DB_HOST".
 * @property description Human-readable description from KDoc, or null if no KDoc was found.
 *                       An empty string means KDoc was present but contained no description text.
 * @property required    Whether the variable is required.
 * @property default     The default value, or null if none.
 * @property sourceFile  Path of the source file relative to the project root.
 * @property lineNumber  1-based line number of the getenv() call.
 */
data class EnvVarEntry(
    val name: String,
    val description: String?,
    val required: Boolean,
    val default: String?,
    val sourceFile: String,
    val lineNumber: Int,
)
