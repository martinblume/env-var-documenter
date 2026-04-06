package com.example

// Feature: System.getenv(CONST_REF) — cross-file constant reference + Elvis default
/** Hostname of the primary database server. */
val dbHost = System.getenv(DB_HOST_KEY) ?: "localhost"

// Feature: System.getenv().getOrDefault(CONST_REF, "default") — cross-file const in getOrDefault
/** Port the database listens on. */
val dbPort = System.getenv().getOrDefault(DB_PORT_KEY, "5432")

// Feature: System.getenv("LITERAL") + Elvis default
/** Maximum number of pooled database connections. */
val dbMaxConnections = System.getenv("DB_MAX_CONNECTIONS") ?: "10"

// Feature: System.getenv("LITERAL") + Elvis throw → required=true, default=null
/** Name of the database schema to connect to. */
val dbName = System.getenv("DATABASE_NAME") ?: throw IllegalStateException("DATABASE_NAME is required")
