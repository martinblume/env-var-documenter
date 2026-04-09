plugins {
    id("io.github.martinblume.env-var-documenter") version "0.1.0"
}

repositories {
    mavenLocal()               // needed so Gradle can resolve the plugin's runtime JAR
    mavenCentral()
}

// Explicit Java-only configuration — the plugin default also includes src/main/kotlin,
// but since that directory doesn't exist here it would simply be skipped.
// Listing only src/main/java makes the intent clear for a pure-Java project.
envVarDocumenter {
    sourceDirs.set(listOf("src/main/java"))
}
