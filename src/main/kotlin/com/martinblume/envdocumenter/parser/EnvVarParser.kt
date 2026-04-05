package com.martinblume.envdocumenter.parser

import com.martinblume.envdocumenter.model.EnvVarEntry
import org.gradle.api.logging.Logging
import java.io.File

class EnvVarParser {

    private val logger = Logging.getLogger(EnvVarParser::class.java)

    // -------------------------------------------------------------------------
    // Regexes
    // -------------------------------------------------------------------------

    private val constValRegex = Regex(
        """(?:(?:private|internal|public|protected)\s+)*const\s+val\s+(\w+)\s*=\s*"([^"]*)""""
    )

    private val getenvLiteralRegex = Regex(
        """System\.getenv\(\s*"([^"]+)"\s*\)"""
    )

    // Only matches SCREAMING_SNAKE_CASE to avoid matching local variables
    private val getenvConstRefRegex = Regex(
        """System\.getenv\(\s*([A-Z_][A-Z0-9_]*)\s*\)"""
    )

    private val elvisDefaultRegex = Regex(
        """System\.getenv\([^)]+\)\s*\?:\s*"([^"]*)""""
    )

    private val elvisThrowRegex = Regex(
        """System\.getenv\([^)]+\)\s*\?:\s*throw\b"""
    )

    // Matches a continuation Elvis on the *next* line, e.g. `    ?: "default"`
    private val nextLineElvisDefaultRegex = Regex("""^\s*\?:\s*"([^"]*)"""")
    private val nextLineElvisThrowRegex = Regex("""^\s*\?:\s*throw\b""")

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parse all given Kotlin files and return a deduplicated, alphabetically sorted list of
     * [EnvVarEntry] values.
     *
     * @param files    Kotlin source files to scan.
     * @param basePath Root directory used to compute relative [EnvVarEntry.sourceFile] paths.
     *                 Defaults to the current working directory.
     */
    fun parse(files: List<File>, basePath: File = File(".")): List<EnvVarEntry> {
        // Read every file exactly once and share the lines for both passes.
        val fileLines: Map<File, List<String>> = files.associateWith { it.readLines(Charsets.UTF_8) }

        val constantMap = collectConstants(fileLines)

        return fileLines
            .flatMap { (file, lines) -> parseFile(file, lines, constantMap, basePath) }
            .groupBy { it.name }
            // Prefer the entry with the most metadata (longest description, or has a default).
            .map { (_, group) ->
                group.maxBy { (it.description?.length ?: 0) + (if (it.default != null) 1 else 0) }
            }
            .sortedBy { it.name }
    }

    // -------------------------------------------------------------------------
    // Internal helpers (visible to tests in the same module)
    // -------------------------------------------------------------------------

    /** Pass 1: collect all `const val NAME = "VALUE"` definitions across all files. */
    internal fun collectConstants(fileLines: Map<File, List<String>>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for ((_, lines) in fileLines) {
            for (line in lines) {
                constValRegex.find(line)?.let { match ->
                    map[match.groupValues[1]] = match.groupValues[2]
                }
            }
        }
        return map
    }

    /**
     * Returns the index of the first `//` on [line] that is not inside a double-quoted string
     * literal, or -1 if no such comment marker exists.
     *
     * Handles escaped characters (`\"`, `\\`) so that a quote preceded by a backslash does not
     * toggle the "inside string" state.
     */
    internal fun findLineCommentStart(line: String): Int {
        var inString = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inString) {
                if (c == '\\') i++ // skip the escaped character
                else if (c == '"') inString = false
            } else {
                if (c == '"') inString = true
                else if (c == '/' && i + 1 < line.length && line[i + 1] == '/') return i
            }
            i++
        }
        return -1
    }

    /** Pass 2: find all System.getenv() calls in a single file's pre-read lines. */
    internal fun parseFile(
        file: File,
        lines: List<String>,
        constantMap: Map<String, String>,
        basePath: File = File("."),
    ): List<EnvVarEntry> {
        val relativeSourcePath = runCatching { file.relativeTo(basePath).path }.getOrElse { file.name }
        val entries = mutableListOf<EnvVarEntry>()

        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1

            // Match the getenv() call — literal string arg takes priority over const ref.
            val literalMatch = getenvLiteralRegex.find(line)
            val constRefMatch = if (literalMatch == null) getenvConstRefRegex.find(line) else null

            val activeMatch = literalMatch ?: constRefMatch ?: continue

            // Skip calls that appear after a `//` line comment on the same line.
            // Uses findLineCommentStart() rather than indexOf("//") to avoid false positives
            // when "//" appears inside a string literal before the getenv() call.
            val commentStart = findLineCommentStart(line)
            if (commentStart != -1 && commentStart < activeMatch.range.first) continue

            val varName: String? = when {
                literalMatch != null -> literalMatch.groupValues[1]
                else -> {
                    val constName = constRefMatch!!.groupValues[1]
                    val resolved = constantMap[constName]
                    if (resolved == null) {
                        logger.warn(
                            "env-var-documenter: Unresolved constant reference '$constName' " +
                                "at $relativeSourcePath:$lineNumber — entry will be skipped."
                        )
                    }
                    resolved
                }
            }
            if (varName == null) continue

            // --- Infer required/default from Elvis on the same line ---
            val elvisDefaultMatch = elvisDefaultRegex.find(line)
            val hasElvisThrow = elvisThrowRegex.containsMatchIn(line)

            // If no Elvis on this line, peek at the next line (multi-line Elvis pattern).
            val inferredDefault: String?
            val inferredRequired: Boolean
            if (elvisDefaultMatch == null && !hasElvisThrow && index + 1 < lines.size) {
                val nextLine = lines[index + 1]
                val nextDefault = nextLineElvisDefaultRegex.find(nextLine)
                val nextThrow = nextLineElvisThrowRegex.containsMatchIn(nextLine)
                inferredDefault = nextDefault?.groupValues?.get(1)
                inferredRequired = when {
                    nextThrow        -> true
                    nextDefault != null -> false
                    else             -> true
                }
            } else {
                inferredDefault = elvisDefaultMatch?.groupValues?.get(1)
                inferredRequired = when {
                    hasElvisThrow           -> true
                    elvisDefaultMatch != null -> false
                    else                    -> true
                }
            }

            val kdoc = KDocParser.extractKDocAbove(lines, index)

            entries += EnvVarEntry(
                name = varName,
                description = kdoc?.description?.takeIf { it.isNotBlank() },
                required = kdoc?.requiredOverride ?: inferredRequired,
                default = kdoc?.defaultOverride ?: inferredDefault,
                sourceFile = relativeSourcePath,
                lineNumber = lineNumber,
            )
        }

        return entries
    }
}
