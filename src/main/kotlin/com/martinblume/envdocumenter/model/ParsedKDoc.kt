package com.martinblume.envdocumenter.model

/**
 * Parsed result from a KDoc block found above a System.getenv() call.
 *
 * @property description    The human-readable description extracted from the KDoc body.
 * @property defaultOverride The value of the `@default` tag, if present.
 * @property requiredOverride The value of the `@required` tag, if present.
 */
data class ParsedKDoc(
    val description: String,
    val defaultOverride: String?,
    val requiredOverride: Boolean?,
)
