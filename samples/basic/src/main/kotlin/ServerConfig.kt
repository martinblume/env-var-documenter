package com.example

// Feature: System.getenv().getOrDefault("LITERAL", "default")
/**
 * Port the HTTP server listens on.
 * Supports ports 1–65535.
 */
val serverPort = System.getenv().getOrDefault("SERVER_PORT", "8080")

// Feature: System.getenv("LITERAL") + Elvis default
/** Bind address for the HTTP server. */
val serverHost = System.getenv("SERVER_HOST") ?: "0.0.0.0"

// Feature: Markdown special characters in KDoc description that must be escaped:
//   backtick (`), asterisk (*), underscore (_), open bracket ([), backslash (\)
// Also tests UTF-8 characters in the description.
/**
 * Base URL for the API.
 * Example: `http://localhost:8080/api` — note the *scheme* and _path_ prefix.
 * See [RFC 3986] for the URL format. On Windows paths use C:\inetpub\wwwroot style.
 * Für Umlaute: äöü werden unterstützt.
 */
val apiBaseUrl = System.getenv("API_BASE_URL") ?: "http://localhost:8080"

// Feature: Multi-line Elvis with a string default
// The ?: operator is on the line immediately following the getenv() call.
val logLevel =
    System.getenv("LOG_LEVEL")
        ?: "INFO"

// Feature: Multi-line Elvis with throw → required=true
val tlsCertPath =
    System.getenv("TLS_CERT_PATH")
        ?: throw IllegalStateException("TLS_CERT_PATH is required for HTTPS")

// Feature: System.getenv().getOrDefault(CONST_REF, "default") — internal const from Constants.kt
/** Cache entry TTL in seconds. */
val cacheTtl = System.getenv().getOrDefault(CACHE_TTL_KEY, "300")
