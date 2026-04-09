# env-var-documenter — Claude Context

## Project Overview

A Gradle plugin that scans Kotlin and Java source files for `System.getenv()` calls and injects a Markdown table documenting all environment variables into a README file.

- **Plugin ID**: `com.martinblume.env-var-documenter`
- **Group/Version**: `com.martinblume` / `0.1.0`
- **Java target**: JVM 17
- **Published to**: Gradle Plugin Portal

## Build & Test

```bash
./gradlew test              # Run all unit + functional tests
./gradlew build             # Compile + test
./gradlew publishToMavenLocal  # Install locally for manual integration testing
```

Tests use JUnit 5. Functional tests use GradleTestKit (`GradleRunner`) and run real Gradle builds in a temp directory.

## Architecture

Two-pass parsing pipeline → README injection → Gradle task wiring.

### Source Layout

```
src/main/kotlin/com/martinblume/envdocumenter/
├── EnvVarDocumenterPlugin.kt      # Plugin entry point; registers tasks and wires lifecycle
├── EnvVarDocumenterExtension.kt   # DSL extension (sourceDirs, readmeFile, markers)
├── GenerateEnvVarDocsTask.kt      # Writes updated table into README
├── VerifyEnvVarDocsTask.kt        # CI check — fails if README is stale (never writes)
├── model/
│   ├── EnvVarEntry.kt             # Data class: name, description, required, default, sourceFile, lineNumber
│   └── ParsedKDoc.kt              # Data class: description, defaultOverride, requiredOverride
├── parser/
│   ├── EnvVarParser.kt            # Two-pass regex parser for .kt and .java files
│   └── KDocParser.kt              # Extracts KDoc block above a given line index
└── readme/
    ├── ReadmeInjector.kt          # Builds Markdown table; inject() / verify() / extractCurrentContent()
    └── ReadmeMarkerException.kt   # Exception thrown when markers are missing or reversed
```

### Parsing Pipeline (`EnvVarParser`)

1. **Pass 1 — constant collection** (`collectConstants()`): Scans ALL files and builds a constant-name → string-value lookup map. Kotlin files use `const val NAME = "VALUE"`; Java files use `public static final String NAME = "value"` (any access modifier, either `static final` / `final static` order). This enables cross-file constant resolution.
2. **Pass 2 — getenv detection** (`parseFile()`): For each line, matches `System.getenv("LITERAL")` or `System.getenv(CONST_REF)`. Skips calls that appear after `//` on the same line. Peeks at the next line for multi-line Elvis operators.

**Regex patterns** (in `EnvVarParser`):
- `constValRegex` — captures Kotlin `const val` with optional access modifiers
- `javaStaticFinalRegex` — captures Java `(public|private|protected)? (static final|final static) String NAME = "value"` (any access modifier, either modifier order)
- `getenvLiteralRegex` — literal string argument
- `getenvConstRefRegex` — `SCREAMING_SNAKE_CASE` identifier only (avoids local variable false positives)
- `elvisDefaultRegex` / `elvisThrowRegex` — same-line Elvis detection
- `nextLineElvisDefaultRegex` / `nextLineElvisThrowRegex` — next-line Elvis peek

`collectConstants()` selects the appropriate regex per file: `javaStaticFinalRegex` for `.java` files, `constValRegex` for everything else.

**KDoc/JavaDoc integration** (`KDocParser`): If a `/** ... */` block immediately precedes the line with `getenv()`, it is parsed for (both Kotlin KDoc and Java JavaDoc use the same `/** ... */` format, so no separate parser is needed):
- Plain description text
- `@default <value>` — overrides inferred default
- `@required false` — overrides inferred required flag

**Deduplication**: Entries with the same name are merged; the one with the most metadata wins (`maxBy` on description length + default presence).

### README Injection (`ReadmeInjector`)

`inject()` replaces content between `startMarker` and `endMarker` with a freshly built Markdown table. The markers themselves are preserved, making repeated invocations idempotent.

`verify()` compares `extractCurrentContent()` against `buildTable()` without writing — returns `null` if up to date, or a diff message if stale.

`escapeMarkdown()` is applied to all cell values:
- `\r\n` → space (before `\r` / `\n` individually, to avoid double-replacement)
- `\n` → space
- `\r` → space
- `|` → `\|`

All file I/O uses `Charsets.UTF_8` explicitly.

### Gradle Tasks

| Task | Class | Type | Purpose |
|---|---|---|---|
| `generateEnvVarDocs` | `GenerateEnvVarDocsTask` | `@OutputFile` on readme | Writes updated table |
| `verifyEnvVarDocs` | `VerifyEnvVarDocsTask` | `@InputFile` on readme | CI check; fails if stale |

`verifyEnvVarDocs` uses `@DisableCachingByDefault` so Gradle always re-runs it.

`verifyEnvVarDocs` is wired into `check` only when the `base` plugin is present:
```kotlin
project.plugins.withId("base") {
    project.tasks.getByName("check").dependsOn("verifyEnvVarDocs")
}
```
> **Note**: `getByName()` is used (not `tasks.named(...).configure { }`) because Kotlin SAM conversion breaks when `configure` is called inside a nested lambda (the inner lambda is inferred as `Function2` rather than `Action<Task>`). Since `withId` fires only after `base` is applied and `check` is guaranteed to exist, eager lookup is safe here.

### Extension DSL Defaults

| Property | Default |
|---|---|
| `sourceDirs` | `["src/main/kotlin", "src/main/java"]` |
| `readmeFile` | `"README.md"` |
| `sectionStartMarker` | `<!-- ENV_VARS_START -->` |
| `sectionEndMarker` | `<!-- ENV_VARS_END -->` |

## Test Requirements

**Every new piece of code must be accompanied by tests in the same PR/commit.**

- **Unit tests** for all non-trivial methods — including protected helpers on abstract classes (expose via a `ConcreteTestTask` subclass registered with `ProjectBuilder`).
- **Task action tests** via `ProjectBuilder` + `@TempDir`: call `task.generate()` / `task.verify()` directly — no Gradle daemon needed.
- **Integration/e2e tests** via `GradleTestKit` (`GradleRunner`) when the behavior spans plugin wiring, task registration, or real Gradle lifecycle (up-to-date checking, `check` dependency, etc.).
- Cover **error paths** (missing files, wrong markers, stale README) and **warning branches** (empty source dirs, no `getenv` calls), not just the happy path.

## Test Structure

```
src/test/kotlin/com/martinblume/envdocumenter/
├── BaseEnvVarDocTaskTest.kt          # Unit tests for base-class helpers (requireReadmeExists, collectSourceFiles, etc.)
├── EnvVarDocumenterPluginTest.kt     # Unit tests for plugin registration, extension defaults, property wiring
├── GenerateEnvVarDocsTaskTest.kt     # Unit tests for generate() action (ProjectBuilder, direct invocation)
├── VerifyEnvVarDocsTaskTest.kt       # Unit tests for verify() action (ProjectBuilder, direct invocation)
├── parser/
│   ├── EnvVarParserTest.kt           # Unit tests for parsing, deduplication, sorting, KDoc, UTF-8
│   └── KDocParserTest.kt             # Unit tests for KDoc extraction and tag parsing
├── readme/
│   └── ReadmeInjectorTest.kt         # Unit tests for inject(), verify(), escaping, UTF-8
└── functional/
    └── EnvVarDocumenterFunctionalTest.kt  # GradleTestKit end-to-end tests
```

Functional tests build a complete throwaway Gradle project in a `@TempDir` using `GradleRunner.withPluginClasspath()`.

## Known Gotchas

- **No environment variable name from lowercase identifiers**: `System.getenv(someVar)` is intentionally ignored — only `SCREAMING_SNAKE_CASE` constants are resolved.
- **Unresolved const references are warned and skipped**: If `System.getenv(SOME_CONST)` is used but `SOME_CONST` is not found in any source file in `sourceDirs`, the entry is dropped and a `logger.warn()` message is emitted via Gradle's logging API.
- **`check` wiring is eager**: Uses `getByName` instead of lazy `named(...).configure { }` due to SAM conversion issue — see note in plugin section above. This means the `check` task must already exist when `base` is applied.
- **Configuration cache**: The plugin is not yet verified as configuration-cache compatible.
- **Running both tasks in the same invocation**: Running `generateEnvVarDocs` and `verifyEnvVarDocs` in the same `gradle` command causes a Gradle implicit-dependency validation error, because both tasks reference the same `README.md` file without an explicit ordering declaration. Run them in separate invocations.
