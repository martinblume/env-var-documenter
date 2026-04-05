package com.martinblume.envdocumenter.readme

import com.martinblume.envdocumenter.model.EnvVarEntry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReadmeInjectorTest {

    @TempDir
    lateinit var tempDir: File

    private val injector = ReadmeInjector(
        startMarker = "<!-- ENV_VARS_START -->",
        endMarker = "<!-- ENV_VARS_END -->",
    )

    private fun readme(content: String): File =
        File(tempDir, "README.md").also { it.writeText(content) }

    private fun entry(
        name: String,
        description: String? = null,
        required: Boolean = true,
        default: String? = null,
    ) = EnvVarEntry(name, description, required, default, "App.kt", 1)

    @Test
    fun `injectsTableBetweenMarkers`() {
        val file = readme("# Docs\n<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->\n")
        injector.inject(file, listOf(entry("DB_HOST", "Database host")))
        val content = file.readText()
        assertTrue(content.contains("| DB_HOST | Database host | yes | — |"))
        assertTrue(content.contains("<!-- ENV_VARS_START -->"))
        assertTrue(content.contains("<!-- ENV_VARS_END -->"))
    }

    @Test
    fun `replacesExistingTable`() {
        val file = readme("<!-- ENV_VARS_START -->\n| old | table |\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("NEW_VAR")))
        val content = file.readText()
        assertFalse(content.contains("old"))
        assertTrue(content.contains("NEW_VAR"))
    }

    @Test
    fun `isIdempotent`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        val entries = listOf(entry("DB_HOST", "host"))
        injector.inject(file, entries)
        val firstRun = file.readText()
        injector.inject(file, entries)
        val secondRun = file.readText()
        assertEquals(firstRun, secondRun)
    }

    @Test
    fun `throwsReadmeMarkerExceptionWhenStartMarkerMissing`() {
        val file = readme("# Readme\n<!-- ENV_VARS_END -->")
        val ex = assertThrows(ReadmeMarkerException::class.java) {
            injector.inject(file, emptyList())
        }
        assertTrue(ex.message!!.contains("Start marker"))
    }

    @Test
    fun `throwsReadmeMarkerExceptionWhenEndMarkerMissing`() {
        val file = readme("<!-- ENV_VARS_START -->")
        val ex = assertThrows(ReadmeMarkerException::class.java) {
            injector.inject(file, emptyList())
        }
        assertTrue(ex.message!!.contains("End marker"))
    }

    @Test
    fun `throwsReadmeMarkerExceptionWhenMarkersReversed`() {
        val file = readme("<!-- ENV_VARS_END -->\n<!-- ENV_VARS_START -->")
        val ex = assertThrows(ReadmeMarkerException::class.java) {
            injector.inject(file, emptyList())
        }
        assertTrue(ex.message!!.contains("End marker appears before"))
    }

    @Test
    fun `emptyEntriesProducesPlaceholderRow`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, emptyList())
        assertTrue(file.readText().contains("No environment variables detected"))
    }

    @Test
    fun `escapesMarkdownPipesInCells`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", "A | B description")))
        assertTrue(file.readText().contains("A \\| B description"))
    }

    @Test
    fun `noRequiredShowsNo`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("OPT_VAR", required = false, default = "fallback")))
        val content = file.readText()
        assertTrue(content.contains("| OPT_VAR | — | no | fallback |"))
    }

    @Test
    fun `extractCurrentContentReturnsTableBetweenMarkers`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("DB_HOST", "Database host")))
        val content = injector.extractCurrentContent(file)
        assertTrue(content.contains("| DB_HOST | Database host | yes | — |"))
        assertFalse(content.contains("<!-- ENV_VARS_START -->"))
        assertFalse(content.contains("<!-- ENV_VARS_END -->"))
    }

    @Test
    fun `verifyReturnsNullWhenUpToDate`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        val entries = listOf(entry("DB_HOST", "host"))
        injector.inject(file, entries)
        assertNull(injector.verify(file, entries))
    }

    @Test
    fun `verifyReturnsDiffMessageWhenStale`() {
        val file = readme("<!-- ENV_VARS_START -->\n| stale | content |\n<!-- ENV_VARS_END -->")
        val diff = injector.verify(file, listOf(entry("NEW_VAR")))
        assertNotNull(diff)
        assertTrue(diff!!.contains("NEW_VAR"))
    }

    @Test
    fun `newlineInDescriptionIsReplacedWithSpace`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", "First line.\nSecond line.")))
        val content = file.readText()
        // The table row must stay on a single line — no raw newline inside a cell
        assertTrue(content.contains("| VAR | First line. Second line. |"), content)
    }

    @Test
    fun `windowsLineEndingInDescriptionIsReplacedWithSpace`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", "First line.\r\nSecond line.")))
        val content = file.readText()
        assertTrue(content.contains("| VAR | First line. Second line. |"), content)
    }

    @Test
    fun `utf8CharactersArePreservedInDescription`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", "Höst-Überprüfung")))
        val content = file.readText(Charsets.UTF_8)
        assertTrue(content.contains("Höst-Überprüfung"), content)
    }

    @Test
    fun `utf8CharactersArePreservedInReadme`() {
        // README itself contains non-ASCII before the markers
        val file = readme("# Ünïcödé Héading\n<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR")))
        val content = file.readText(Charsets.UTF_8)
        assertTrue(content.contains("Ünïcödé Héading"), content)
    }

    @Test
    fun `extractCurrentContentThrowsWhenStartMarkerMissing`() {
        val file = readme("# Readme\n<!-- ENV_VARS_END -->")
        val ex = assertThrows(ReadmeMarkerException::class.java) {
            injector.extractCurrentContent(file)
        }
        assertTrue(ex.message!!.contains("Start marker"))
    }

    @Test
    fun `extractCurrentContentThrowsWhenEndMarkerMissing`() {
        val file = readme("<!-- ENV_VARS_START -->")
        val ex = assertThrows(ReadmeMarkerException::class.java) {
            injector.extractCurrentContent(file)
        }
        assertTrue(ex.message!!.contains("End marker"))
    }

    @Test
    fun `extractCurrentContentThrowsWhenMarkersReversed`() {
        val file = readme("<!-- ENV_VARS_END -->\n<!-- ENV_VARS_START -->")
        val ex = assertThrows(ReadmeMarkerException::class.java) {
            injector.extractCurrentContent(file)
        }
        assertTrue(ex.message!!.contains("End marker appears before"))
    }

    @Test
    fun `buildTableHeaderAlwaysPresent`() {
        val table = injector.buildTable(emptyList())
        assertTrue(table.startsWith("| Variable | Description | Required | Default |"))
    }

    @Test
    fun `nullDescriptionRendersAsDash`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("NO_DOC_VAR", description = null)))
        assertTrue(file.readText().contains("| NO_DOC_VAR | — |"))
    }

    @Test
    fun `nonNullDescriptionIsRendered`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("DOCUMENTED_VAR", description = "My description")))
        assertTrue(file.readText().contains("| DOCUMENTED_VAR | My description |"))
    }

    // ── Markdown escaping ──────────────────────────────────────────────────────

    @Test
    fun `backslashInDescriptionIsEscaped`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", description = """C:\Users\foo""")))
        // Each \ must be doubled so it renders as a literal backslash in Markdown
        assertTrue(file.readText().contains("""C:\\Users\\foo"""), file.readText())
    }

    @Test
    fun `asteriskInDescriptionIsEscaped`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", description = "*verbose*")))
        assertTrue(file.readText().contains("""\*verbose\*"""), file.readText())
    }

    @Test
    fun `underscoreInDescriptionIsEscaped`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", description = "snake_case")))
        assertTrue(file.readText().contains("""snake\_case"""), file.readText())
    }

    @Test
    fun `backtickInDescriptionIsEscaped`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", description = "`true`")))
        assertTrue(file.readText().contains("""\`true\`"""), file.readText())
    }

    @Test
    fun `openBracketInDescriptionIsEscaped`() {
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", description = "[optional]")))
        assertTrue(file.readText().contains("""\[optional]"""), file.readText())
    }

    @Test
    fun `backslashFollowedByPipeIsCorrectlyEscaped`() {
        // Input: \ followed by | (2 chars)
        // Expected in file: \\ followed by \| (4 chars: \\  +  \|)
        // In a raw string: \\\| (3 backslashes + pipe)
        val file = readme("<!-- ENV_VARS_START -->\n<!-- ENV_VARS_END -->")
        injector.inject(file, listOf(entry("VAR", description = """\|""")))
        assertTrue(file.readText().contains("""\\\|"""), file.readText())
    }
}
