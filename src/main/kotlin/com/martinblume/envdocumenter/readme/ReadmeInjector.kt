package com.martinblume.envdocumenter.readme

import com.martinblume.envdocumenter.model.EnvVarEntry
import java.io.File

class ReadmeInjector(
    private val startMarker: String,
    private val endMarker: String,
) {

    /**
     * Reads the README file, replaces the content between [startMarker] and [endMarker] with a
     * freshly generated Markdown table, and writes the result back.
     *
     * The markers themselves are preserved, so repeated invocations are idempotent.
     *
     * @throws ReadmeMarkerException if either marker is absent or they are in the wrong order.
     */
    fun inject(readmeFile: File, entries: List<EnvVarEntry>) {
        val original = readmeFile.readText(Charsets.UTF_8)
        val (startIdx, endIdx) = resolveMarkers(original, readmeFile.name)

        val before = original.substring(0, startIdx + startMarker.length)
        val after = original.substring(endIdx)

        readmeFile.writeText("$before\n${buildTable(entries)}\n$after", Charsets.UTF_8)
    }

    /**
     * Returns `null` if the README section already matches what would be generated for [entries],
     * or a message describing the mismatch if it is out of date.
     *
     * @throws ReadmeMarkerException if markers are absent or in the wrong order.
     */
    fun verify(readmeFile: File, entries: List<EnvVarEntry>): String? {
        val current = extractCurrentContent(readmeFile)
        val expected = buildTable(entries)
        return if (current == expected) null
        else "Current content:\n$current\n\nExpected content:\n$expected"
    }

    internal fun extractCurrentContent(readmeFile: File): String {
        val original = readmeFile.readText(Charsets.UTF_8)
        val (startIdx, _) = resolveMarkers(original, readmeFile.name)

        val afterStart = original.substring(startIdx + startMarker.length)
        return afterStart.substringBefore(endMarker).trim('\n')
    }

    private fun resolveMarkers(text: String, fileName: String): Pair<Int, Int> {
        val startIdx = text.indexOf(startMarker)
        val endIdx = text.indexOf(endMarker)

        when {
            startIdx == -1 -> throw ReadmeMarkerException(
                "env-var-documenter: Start marker \"$startMarker\" not found in $fileName. " +
                "Add the marker to your README where the table should appear."
            )
            endIdx == -1 -> throw ReadmeMarkerException(
                "env-var-documenter: End marker \"$endMarker\" not found in $fileName. " +
                "Add \"$endMarker\" after \"$startMarker\" in your README."
            )
            endIdx < startIdx -> throw ReadmeMarkerException(
                "env-var-documenter: End marker appears before start marker in $fileName. " +
                "Ensure \"$startMarker\" comes before \"$endMarker\"."
            )
        }

        return Pair(startIdx, endIdx)
    }

    internal fun buildTable(entries: List<EnvVarEntry>): String {
        val header = "| Variable | Description | Required | Default |"
        val separator = "|---|---|---|---|"

        if (entries.isEmpty()) {
            return "$header\n$separator\n| — | No environment variables detected | — | — |"
        }

        val rows = entries.joinToString("\n") { entry ->
            val required = if (entry.required) "yes" else "no"
            val default = entry.default?.let { escapeMarkdown(it) } ?: "—"
            val description = escapeMarkdown(entry.description?.ifBlank { null } ?: "—")
            "| ${entry.name} | $description | $required | $default |"
        }

        return "$header\n$separator\n$rows"
    }

    private fun escapeMarkdown(text: String): String =
        text
            .replace("\r\n", " ")  // Windows line endings → space
            .replace('\n', ' ')    // Unix newlines → space
            .replace('\r', ' ')    // Bare CR → space
            .replace("|", "\\|")
}
