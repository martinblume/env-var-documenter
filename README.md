# env-var-documenter

A Gradle plugin that scans your Kotlin source files for `System.getenv()` calls and automatically injects a Markdown table documenting all environment variables into your README.

## Features

- Detects `System.getenv("LITERAL")`, `System.getenv(CONST_REF)`, and `System.getenv().getOrDefault("KEY", "default")` calls across all configured source directories
- Resolves `const val` references across files (cross-file constant resolution)
- Reads KDoc comments above `getenv()` calls for descriptions, defaults, and required flags
- Infers `required` and `default` from Elvis operators (`?: "fallback"`, `?: throw ...`)
- Deduplicates entries when the same variable is read in multiple places
- Escapes Markdown special characters (`\`, `|`, `*`, `_`, `` ` ``, `[`) in descriptions and default values so they render as literal text
- `generateEnvVarDocs` task writes the table; `verifyEnvVarDocs` task fails CI if the table is stale

## Setup

### 1. Apply the plugin

```kotlin
// build.gradle.kts
plugins {
    id("com.martinblume.env-var-documenter") version "0.1.0"
}
```

### 2. Add markers to your README

Place these HTML comments where the generated table should appear:

```markdown
<!-- ENV_VARS_START -->
<!-- ENV_VARS_END -->
```

### 3. Generate the docs

```bash
./gradlew generateEnvVarDocs
```

The plugin will scan your sources and inject a table between the markers:

```markdown
<!-- ENV_VARS_START -->
| Variable | Description | Required | Default |
|---|---|---|---|
| DB_HOST | Hostname of the database server. | yes | — |
| DB_PORT | Port the database listens on. | no | 5432 |
<!-- ENV_VARS_END -->
```

## Configuration

All settings are optional — the defaults work for a standard Kotlin or Java project layout.

```kotlin
envVarDocumenter {
    sourceDirs.set(listOf("src/main/kotlin", "src/main/java"))   // directories to scan
    readmeFile.set("README.md")                                  // file to inject into
    sectionStartMarker.set("<!-- ENV_VARS_START -->")
    sectionEndMarker.set("<!-- ENV_VARS_END -->")
}
```

| Property | Default | Description |
|---|---|---|
| `sourceDirs` | `["src/main/kotlin", "src/main/java"]` | Source directories to scan for `.kt` and `.java` files |
| `readmeFile` | `"README.md"` | Path to the README file (relative to project root) |
| `sectionStartMarker` | `<!-- ENV_VARS_START -->` | Marker that begins the generated section |
| `sectionEndMarker` | `<!-- ENV_VARS_END -->` | Marker that ends the generated section |

## KDoc annotations

Add a KDoc comment directly above a `getenv()` call to provide metadata:

```kotlin
/**
 * Hostname of the primary database server.
 * @required true
 * @default localhost
 */
val dbHost = System.getenv("DB_HOST") ?: "localhost"
```

| Tag | Effect |
|---|---|
| *(plain text)* | Used as the variable's description in the table |
| `@required true/false` | Overrides the inferred required flag |
| `@default <value>` | Overrides the inferred default value |

Without a KDoc comment the plugin infers `required` and `default` from Elvis operators on the same line or the line immediately following the call, or from the second argument of `getOrDefault()`:

```kotlin
val port = System.getenv("PORT") ?: "8080"               // → required: no, default: 8080
val key  = System.getenv("API_KEY")
    ?: throw IllegalStateException("API_KEY is required") // → required: yes
val host = System.getenv().getOrDefault("DB_HOST", "localhost") // → required: no, default: localhost
```

## CI verification

When the `base` plugin is applied, `verifyEnvVarDocs` is automatically wired into the `check` lifecycle task. It reads your README and fails the build if the generated table would differ from what is already there — without modifying any files.

```bash
./gradlew check   # fails if README is out of date
```

You can also run the task directly:

```bash
./gradlew verifyEnvVarDocs
```

## Tasks

| Task | Group | Description |
|---|---|---|
| `generateEnvVarDocs` | documentation | Scans sources and writes the env-var table into the README |
| `verifyEnvVarDocs` | documentation | Fails if the README table is stale; never writes to disk |

## License

MIT
