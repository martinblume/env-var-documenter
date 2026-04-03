package com.martinblume.envdocumenter.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EnvVarParserTest {

    @TempDir
    lateinit var tempDir: File

    private fun kt(name: String, content: String): File =
        File(tempDir, "$name.kt").also { it.writeText(content) }

    private val parser = EnvVarParser()

    // -------------------------------------------------------------------------
    // Core detection
    // -------------------------------------------------------------------------

    @Test
    fun `parsesLiteralGetenv`() {
        val file = kt("App", """val x = System.getenv("DB_HOST")""")
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertEquals("DB_HOST", entries[0].name)
        assertTrue(entries[0].required)
        assertNull(entries[0].default)
    }

    @Test
    fun `parsesElvisDefault`() {
        val file = kt("App", """val x = System.getenv("PORT") ?: "8080"""")
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertEquals("PORT", entries[0].name)
        assertFalse(entries[0].required)
        assertEquals("8080", entries[0].default)
    }

    @Test
    fun `parsesElvisThrow`() {
        val file = kt("App", """val x = System.getenv("SECRET") ?: throw IllegalStateException("missing")""")
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertEquals("SECRET", entries[0].name)
        assertTrue(entries[0].required)
        assertNull(entries[0].default)
    }

    @Test
    fun `resolvesConstantReference`() {
        val file = kt("Config", """
            const val DB_URL_KEY = "DATABASE_URL"
            val url = System.getenv(DB_URL_KEY)
        """.trimIndent())
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertEquals("DATABASE_URL", entries[0].name)
    }

    @Test
    fun `ignoresLowerCaseIdentifierReference`() {
        val file = kt("App", """val x = System.getenv(someVariable)""")
        val entries = parser.parse(listOf(file))
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `deduplicatesByName`() {
        val f1 = kt("A", """val x = System.getenv("MY_VAR")""")
        val f2 = kt("B", """val x = System.getenv("MY_VAR")""")
        val entries = parser.parse(listOf(f1, f2))
        assertEquals(1, entries.size)
        assertEquals("MY_VAR", entries[0].name)
    }

    @Test
    fun `sortsAlphabetically`() {
        val file = kt("Config", """
            val z = System.getenv("ZEBRA_VAR")
            val a = System.getenv("ALPHA_VAR")
        """.trimIndent())
        val entries = parser.parse(listOf(file))
        assertEquals(listOf("ALPHA_VAR", "ZEBRA_VAR"), entries.map { it.name })
    }

    @Test
    fun `noEntriesForEmptyFile`() {
        val file = kt("Empty", "")
        val entries = parser.parse(listOf(file))
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `constValsWithPrivateModifier`() {
        val file = kt("Config", """
            private const val KEY = "PRIVATE_VAR"
            val x = System.getenv(KEY)
        """.trimIndent())
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertEquals("PRIVATE_VAR", entries[0].name)
    }

    @Test
    fun `collectsConstantsAcrossFiles`() {
        val f1 = kt("Constants", """const val HOST_KEY = "DB_HOST"""")
        val f2 = kt("App", """val host = System.getenv(HOST_KEY)""")
        val entries = parser.parse(listOf(f1, f2))
        assertEquals(1, entries.size)
        assertEquals("DB_HOST", entries[0].name)
    }

    // -------------------------------------------------------------------------
    // KDoc integration
    // -------------------------------------------------------------------------

    @Test
    fun `kdocDescriptionIsExtracted`() {
        val file = kt("App", """
            /** The database hostname. */
            val host = System.getenv("DB_HOST")
        """.trimIndent())
        val entries = parser.parse(listOf(file))
        assertEquals("The database hostname.", entries[0].description)
    }

    @Test
    fun `kdocDescriptionIsNullWhenNoKDoc`() {
        val file = kt("App", """val host = System.getenv("DB_HOST")""")
        val entries = parser.parse(listOf(file))
        assertNull(entries[0].description)
    }

    @Test
    fun `kdocOverridesInferredDefault`() {
        val file = kt("App", """
            /**
             * Port number.
             * @default 9090
             */
            val port = System.getenv("PORT") ?: "8080"
        """.trimIndent())
        val entries = parser.parse(listOf(file))
        assertEquals("9090", entries[0].default)
    }

    @Test
    fun `kdocRequiredFalseOverridesNoElvis`() {
        val file = kt("App", """
            /**
             * Optional setting.
             * @required false
             */
            val x = System.getenv("OPTIONAL_VAR")
        """.trimIndent())
        val entries = parser.parse(listOf(file))
        assertFalse(entries[0].required)
    }

    // -------------------------------------------------------------------------
    // Comment detection (#4)
    // -------------------------------------------------------------------------

    @Test
    fun `commentedOutGetenvIsIgnored`() {
        val file = kt("App", """
            // val host = System.getenv("DB_HOST")
            val port = System.getenv("PORT")
        """.trimIndent())
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertEquals("PORT", entries[0].name)
    }

    @Test
    fun `getenvAfterInlineCommentMarkerIsIgnored`() {
        // The getenv call appears after a `//` on the same line (unusual but possible in generated code).
        val file = kt("App", """val x = 42 // System.getenv("COMMENTED_VAR")""")
        val entries = parser.parse(listOf(file))
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `getenvBeforeInlineCommentIsDetected`() {
        val file = kt("App", """val x = System.getenv("REAL_VAR") // trailing comment""")
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertEquals("REAL_VAR", entries[0].name)
    }

    // -------------------------------------------------------------------------
    // Multi-line Elvis (#5)
    // -------------------------------------------------------------------------

    @Test
    fun `multiLineElvisDefaultIsDetected`() {
        val file = kt("App", """
            val port =
                System.getenv("PORT")
                    ?: "8080"
        """.trimIndent())
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertFalse(entries[0].required)
        assertEquals("8080", entries[0].default)
    }

    @Test
    fun `multiLineElvisThrowIsDetected`() {
        val file = kt("App", """
            val secret =
                System.getenv("SECRET")
                    ?: throw IllegalStateException("SECRET is required")
        """.trimIndent())
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertTrue(entries[0].required)
    }

    // -------------------------------------------------------------------------
    // Charset handling
    // -------------------------------------------------------------------------

    @Test
    fun `utf8SourceFileIsParsedCorrectly`() {
        // File written explicitly as UTF-8 with non-ASCII KDoc content
        val file = File(tempDir, "Utf8.kt").also {
            it.writeText(
                "/** Höst-Beschreibung. */\nval x = System.getenv(\"UTF8_VAR\")",
                Charsets.UTF_8,
            )
        }
        val entries = parser.parse(listOf(file))
        assertEquals(1, entries.size)
        assertEquals("UTF8_VAR", entries[0].name)
        assertEquals("Höst-Beschreibung.", entries[0].description)
    }

    // -------------------------------------------------------------------------
    // Source file path (#8)
    // -------------------------------------------------------------------------

    @Test
    fun `sourceFileIsRelativeToBasePath`() {
        val file = kt("Config", """val x = System.getenv("VAR")""")
        val entries = parser.parse(listOf(file), basePath = tempDir)
        assertEquals("Config.kt", entries[0].sourceFile)
    }

    // -------------------------------------------------------------------------
    // Internal helpers (accessible within the same module)
    // -------------------------------------------------------------------------

    @Test
    fun `collectConstantsReadsFromPreloadedLines`() {
        val f1 = kt("Consts", """const val MY_KEY = "MY_VALUE"""")
        val fileLines = mapOf(f1 to f1.readLines())
        val map = parser.collectConstants(fileLines)
        assertEquals("MY_VALUE", map["MY_KEY"])
    }

    @Test
    fun `parseFileAcceptsPreloadedLines`() {
        val file = kt("App", """val x = System.getenv("PRELOADED_VAR")""")
        val lines = file.readLines()
        val entries = parser.parseFile(file, lines, emptyMap())
        assertEquals(1, entries.size)
        assertEquals("PRELOADED_VAR", entries[0].name)
    }
}
