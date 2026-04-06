package com.example

// Feature: Deduplication — DATABASE_HOST is also read in DatabaseConfig.kt.
// The plugin keeps the entry with the most metadata; DatabaseConfig.kt has a KDoc description
// so that entry wins and this bare read is silently merged into it.
val altDbHost = System.getenv(DB_HOST_KEY)

// Feature: Commented-out getenv — must NOT appear in the generated docs.
// val ignoredVar = System.getenv("IGNORED_VAR")

// Feature: "//" inside a string literal before the getenv() call.
// indexOf("//") would wrongly treat the "//" in "http://docs.example.com" as a line comment
// and skip the entire line. The parser's findLineCommentStart() handles this correctly.
val featureFlagHost = mapOf(
    "docs_url" to "http://docs.example.com",
    "host" to System.getenv("FEATURE_FLAG_HOST"),
)
