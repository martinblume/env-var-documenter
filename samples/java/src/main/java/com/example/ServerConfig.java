package com.example;

import static com.example.Constants.CACHE_TTL_KEY;

/**
 * HTTP server and API configuration resolved from environment variables.
 */
public class ServerConfig {

    // Feature: System.getenv().getOrDefault("LITERAL", "default")
    /**
     * Port the HTTP server listens on.
     * Supports ports 1–65535.
     */
    public final String serverPort = System.getenv().getOrDefault("SERVER_PORT", "8080");

    // Feature: System.getenv("LITERAL") — required, no default
    /** Bind address for the HTTP server. */
    public final String serverHost = System.getenv("SERVER_HOST");

    // Feature: Markdown special characters in JavaDoc that must be escaped in the table:
    //   backtick (`), asterisk (*), underscore (_), open bracket ([), backslash (\)
    //   Also tests UTF-8 characters in the description.
    /**
     * Base URL for the API.
     * Example: `http://localhost:8080/api` — note the *scheme* and _path_ prefix.
     * See [RFC 3986] for the URL format. On Windows paths use C:\inetpub\wwwroot style.
     * Für Umlaute: äöü werden unterstützt.
     */
    public final String apiBaseUrl = System.getenv().getOrDefault("API_BASE_URL", "http://localhost:8080");

    // Feature: System.getenv("LITERAL") — required, no default
    /** TLS certificate file path. Required for HTTPS. */
    public final String tlsCertPath = System.getenv("TLS_CERT_PATH");

    // Feature: System.getenv().getOrDefault(CONST_REF, "default") — cross-file const in getOrDefault
    /** Cache entry TTL in seconds. */
    public final String cacheTtl = System.getenv().getOrDefault(CACHE_TTL_KEY, "300");
}
