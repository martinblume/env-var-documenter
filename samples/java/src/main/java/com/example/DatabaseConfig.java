package com.example;

import static com.example.Constants.DB_HOST_KEY;
import static com.example.Constants.DB_PORT_KEY;

/**
 * Database connection configuration resolved from environment variables.
 */
public class DatabaseConfig {

    // Feature: System.getenv(CONST_REF) — cross-file constant reference
    /** Hostname of the primary database server. */
    public final String host = System.getenv(DB_HOST_KEY);

    // Feature: System.getenv().getOrDefault(CONST_REF, "default") — const ref in getOrDefault
    /** Port the database listens on. */
    public final String port = System.getenv().getOrDefault(DB_PORT_KEY, "5432");

    // Feature: System.getenv("LITERAL") — literal key, required (no default available)
    /** Name of the database schema to connect to. */
    public final String name = System.getenv("DATABASE_NAME");

    // Feature: System.getenv().getOrDefault("LITERAL", "default") — literal key with fallback
    /** Maximum number of pooled database connections. */
    public final String maxConnections = System.getenv().getOrDefault("DB_MAX_CONNECTIONS", "10");
}
