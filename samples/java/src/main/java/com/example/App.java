package com.example;

import static com.example.Constants.DB_HOST_KEY;
import static com.example.Constants.FEATURE_KEY;
import java.util.Map;

public class App {

    // Feature: Deduplication — DATABASE_HOST is also read in DatabaseConfig.java.
    // The plugin keeps the entry with the most metadata; DatabaseConfig.java has a KDoc
    // description so that entry wins and this bare read is silently merged into it.
    public final String altDbHost = System.getenv(DB_HOST_KEY);

    // Feature: Commented-out getenv — must NOT appear in the generated docs.
    // public final String ignoredVar = System.getenv("IGNORED_VAR");

    // Feature: "//" inside a string literal before the getenv() call.
    // A naive indexOf("//") would wrongly treat "http://docs.example.com" as a line comment
    // and skip the entire line. The parser's findLineCommentStart() handles this correctly.
    public final Map<String, String> featureConfig = Map.of(
        "docs_url", "http://docs.example.com",
        "host", System.getenv(FEATURE_KEY)
    );
}
