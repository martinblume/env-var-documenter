package com.martinblume.envdocumenter.parser

import com.martinblume.envdocumenter.model.ParsedKDoc
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KDocParserTest {

    @Test
    fun `parsesSimpleDescription`() {
        val lines = listOf("/** Database hostname */", "val x = System.getenv(\"DB_HOST\")")
        val result = KDocParser.extractKDocAbove(lines, 1)
        assertNotNull(result)
        assertEquals("Database hostname", result!!.description)
        assertNull(result.defaultOverride)
        assertNull(result.requiredOverride)
    }

    @Test
    fun `parsesMultilineDescription`() {
        val lines = listOf(
            "/**",
            " * Database hostname.",
            " * Used to connect to the primary DB.",
            " */",
            "val x = System.getenv(\"DB_HOST\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 4)
        assertNotNull(result)
        assertEquals("Database hostname. Used to connect to the primary DB.", result!!.description)
    }

    @Test
    fun `parsesDefaultTag`() {
        val lines = listOf(
            "/**",
            " * The database host.",
            " * @default localhost",
            " */",
            "val x = System.getenv(\"DB_HOST\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 4)
        assertNotNull(result)
        assertEquals("localhost", result!!.defaultOverride)
    }

    @Test
    fun `parsesDefaultTagWithSpaces`() {
        val lines = listOf(
            "/**",
            " * Base URL.",
            " * @default http://localhost:8080",
            " */",
            "val x = System.getenv(\"BASE_URL\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 4)
        assertEquals("http://localhost:8080", result!!.defaultOverride)
    }

    @Test
    fun `parsesRequiredTrue`() {
        val lines = listOf(
            "/**",
            " * Secret key.",
            " * @required true",
            " */",
            "val x = System.getenv(\"SECRET\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 4)
        assertEquals(true, result!!.requiredOverride)
    }

    @Test
    fun `parsesRequiredFalse`() {
        val lines = listOf(
            "/**",
            " * Optional feature flag.",
            " * @required false",
            " */",
            "val x = System.getenv(\"FEATURE_FLAG\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 4)
        assertEquals(false, result!!.requiredOverride)
    }

    @Test
    fun `returnsNullWhenNoKDoc`() {
        val lines = listOf(
            "// just a comment",
            "val x = System.getenv(\"DB_HOST\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 1)
        assertNull(result)
    }

    @Test
    fun `returnsNullAtTopOfFile`() {
        val lines = listOf("val x = System.getenv(\"DB_HOST\")")
        val result = KDocParser.extractKDocAbove(lines, 0)
        assertNull(result)
    }

    @Test
    fun `skipsDeclarationLineBetweenKDocAndCall`() {
        val lines = listOf(
            "/**",
            " * Port number.",
            " * @default 8080",
            " */",
            "val port: String =",
            "    System.getenv(\"PORT\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 5)
        assertNotNull(result)
        assertEquals("Port number.", result!!.description)
        assertEquals("8080", result.defaultOverride)
    }

    @Test
    fun `ignoresUnknownTags`() {
        val lines = listOf(
            "/**",
            " * Some variable.",
            " * @param foo ignored",
            " * @see SomeClass",
            " */",
            "val x = System.getenv(\"VAR\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 5)
        assertNotNull(result)
        assertEquals("Some variable.", result!!.description)
        assertNull(result.defaultOverride)
        assertNull(result.requiredOverride)
    }

    @Test
    fun `parseKDocLinesInlineSingleLine`() {
        val raw = listOf("/** Simple description */")
        val result = KDocParser.parseKDocLines(raw)
        assertEquals("Simple description", result.description)
    }

    @Test
    fun `skipsOpenAndOverrideModifiers`() {
        val lines = listOf(
            "/**",
            " * Open property.",
            " */",
            "open val x: String =",
            "    System.getenv(\"OPEN_VAR\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 4)
        assertNotNull(result)
        assertEquals("Open property.", result!!.description)
    }

    @Test
    fun `skipsOverrideModifier`() {
        val lines = listOf(
            "/** Overridden value. */",
            "override val x = System.getenv(\"OVERRIDE_VAR\")",
        )
        val result = KDocParser.extractKDocAbove(lines, 1)
        assertNotNull(result)
        assertEquals("Overridden value.", result!!.description)
    }

    @Test
    fun `returnsNullWhenKDocIsBeyondWindowLimit`() {
        // KDoc is 20 lines above the call — beyond the 15-line window.
        val padding = List(20) { "// filler line $it" }
        val lines = listOf("/** Very far away KDoc */") + padding + listOf("val x = System.getenv(\"FAR_VAR\")")
        val callIndex = lines.size - 1
        val result = KDocParser.extractKDocAbove(lines, callIndex)
        assertNull(result)
    }

    @Test
    fun `findsKDocExactlyAtWindowBoundary`() {
        // KDoc is exactly 15 lines above (the window limit).
        val padding = List(14) { "" } // 14 blank lines → skippable
        val lines = listOf("/** Boundary KDoc */") + padding + listOf("val x = System.getenv(\"BOUNDARY_VAR\")")
        val callIndex = lines.size - 1
        val result = KDocParser.extractKDocAbove(lines, callIndex)
        assertNotNull(result)
        assertEquals("Boundary KDoc", result!!.description)
    }

    @Test
    fun `singleLineKDocNotAtLineZeroIsExtracted`() {
        // Regression: a single-line /** desc */ that is not the first line of the file was
        // previously corrupted by the backward scan mistakenly adding surrounding lines.
        val lines = listOf(
            "class Config {",
            "    public static final String KEY = \"DB_HOST\";",
            "    /** The database host. */",
            "    String host = System.getenv(\"DB_HOST\");",
            "}",
        )
        val result = KDocParser.extractKDocAbove(lines, 3)
        assertNotNull(result)
        assertEquals("The database host.", result!!.description)
    }
}
