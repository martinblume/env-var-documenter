package com.example

// Cross-file constant resolution — these identifiers are referenced in other files via
// System.getenv(CONST_REF) and System.getenv().getOrDefault(CONST_REF, …).
// The plugin resolves them back to their string values across files in a single pass.

const val DB_HOST_KEY = "DATABASE_HOST"
const val DB_PORT_KEY = "DATABASE_PORT"

// Access-modifier variants — the parser must handle private/internal/public/no-modifier.
private const val AUTH_SECRET_KEY = "AUTH_SECRET"
internal const val CACHE_TTL_KEY = "CACHE_TTL_SECONDS"
