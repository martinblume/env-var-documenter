package com.example;

/**
 * Application-wide string constants for environment variable names.
 * These are resolved cross-file by the plugin when other classes use a
 * static import and call getenv with the constant name as the argument.
 */
public class Constants {

    // Feature: public static final String (most common Java constant form)
    // Resolved by: System.getenv(DB_HOST_KEY) in DatabaseConfig.java
    public static final String DB_HOST_KEY = "DATABASE_HOST";

    // Feature: package-private static final (no access modifier)
    // Resolved by: System.getenv().getOrDefault(DB_PORT_KEY, …) in DatabaseConfig.java
    static final String DB_PORT_KEY = "DATABASE_PORT";

    // Feature: private static final — parser resolves it cross-file regardless of visibility
    // Resolved by: System.getenv(AUTH_SECRET_KEY) in AuthConfig.java
    private static final String AUTH_SECRET_KEY = "AUTH_SECRET";

    // Feature: final static modifier order (swapped) — both orderings are supported
    // Resolved by: System.getenv().getOrDefault(CACHE_TTL_KEY, …) in ServerConfig.java
    public final static String CACHE_TTL_KEY = "CACHE_TTL_SECONDS";

    // Feature: cross-file resolution of a constant used in a map literal with "//" before getenv
    // Resolved by: System.getenv(FEATURE_KEY) in App.java
    public static final String FEATURE_KEY = "FEATURE_FLAG_HOST";
}
