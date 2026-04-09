package com.martinblume.envdocumenter.parser

import com.martinblume.envdocumenter.model.ParsedKDoc

object KDocParser {

    private val tagRegex = Regex("""^\s*\*?\s*@(\w+)\s+(.+)$""")

    /**
     * Maximum number of lines to walk back from a System.getenv() call when searching for a KDoc
     * block. Prevents runaway scanning in large files with no KDoc above the call.
     */
    private const val MAX_LOOKBACK_LINES = 15

    /**
     * Kotlin and Java keyword/modifier prefixes that may legally appear between a KDoc/JavaDoc
     * block and the property or expression it documents. Lines starting with any of these are
     * skipped during the backward walk.
     */
    private val SKIPPABLE_PREFIXES = setOf(
        // Declarations
        "val ", "var ", "fun ",
        // All visibility modifiers
        "private ", "internal ", "public ", "protected ",
        // All other modifiers (comprehensive list per Kotlin spec)
        "open ", "abstract ", "final ", "override ", "sealed ",
        "lateinit ", "const ", "inline ", "noinline ", "crossinline ",
        "external ", "expect ", "actual ", "suspend ", "tailrec ",
        "operator ", "infix ", "data ", "enum ", "companion ",
        "inner ", "value ", "annotation ", "object ",
        // Java-specific
        "static ",
    )

    /**
     * Given the full list of source lines and the 0-based index of the line containing the
     * System.getenv() call, walks backwards to find the immediately preceding KDoc block.
     *
     * Skips blank lines, annotation lines (`@…`), and lines beginning with any Kotlin
     * modifier or declaration keyword, up to [MAX_LOOKBACK_LINES] lines above the call.
     *
     * Returns null if no KDoc block is found within the window.
     */
    fun extractKDocAbove(lines: List<String>, callLineIndex: Int): ParsedKDoc? {
        val windowStart = maxOf(0, callLineIndex - MAX_LOOKBACK_LINES)
        var i = callLineIndex - 1

        while (i >= windowStart && isSkippableLine(lines[i])) {
            i--
        }

        if (i < windowStart || !lines[i].trim().endsWith("*/")) return null

        val kdocLines = mutableListOf(lines[i])

        // If this line is both the opening and closing of the comment (single-line /** desc */),
        // no backward scan is needed — it is already a complete doc comment.
        if (!lines[i].trim().startsWith("/**")) {
            i--
            while (i >= 0) {
                kdocLines.add(0, lines[i])
                if (lines[i].trim().startsWith("/**")) break
                i--
            }
        }

        if (!kdocLines.first().trim().startsWith("/**")) return null

        return parseKDocLines(kdocLines)
    }

    private fun isSkippableLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.isBlank()
            || trimmed.startsWith("@")
            || SKIPPABLE_PREFIXES.any { trimmed.startsWith(it) }
    }

    internal fun parseKDocLines(rawLines: List<String>): ParsedKDoc {
        val cleaned = rawLines
            .map { it.trim() }
            .map { line ->
                when {
                    line == "/**" || line == "*/" -> ""
                    line.startsWith("/**") -> line.removePrefix("/**").trimEnd().removeSuffix("*/").trim()
                    line.startsWith("*/") -> ""
                    line.startsWith("* ") -> line.removePrefix("* ")
                    line.startsWith("*") -> line.removePrefix("*").trim()
                    else -> line
                }
            }
            .dropWhile { it.isBlank() }
            .dropLastWhile { it.isBlank() }

        val descriptionLines = mutableListOf<String>()
        var defaultOverride: String? = null
        var requiredOverride: Boolean? = null
        var tagsStarted = false

        for (line in cleaned) {
            val tagMatch = tagRegex.matchEntire(line)
            if (tagMatch != null) {
                tagsStarted = true
                val tag = tagMatch.groupValues[1]
                val value = tagMatch.groupValues[2].trim()
                when (tag) {
                    "default" -> defaultOverride = value
                    "required" -> requiredOverride = value.lowercase() == "true"
                }
            } else if (!tagsStarted) {
                descriptionLines += line
            }
        }

        val description = descriptionLines
            .dropWhile { it.isBlank() }
            .dropLastWhile { it.isBlank() }
            .joinToString(" ")
            .trim()

        return ParsedKDoc(
            description = description,
            defaultOverride = defaultOverride,
            requiredOverride = requiredOverride,
        )
    }
}
