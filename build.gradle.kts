plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "io.github.martinblume"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

gradlePlugin {
    website.set("https://github.com/martinblume/env-var-documenter")
    vcsUrl.set("https://github.com/martinblume/env-var-documenter")

    plugins {
        create("envVarDocumenter") {
            id = "io.github.martinblume.env-var-documenter"
            implementationClass = "com.martinblume.envdocumenter.EnvVarDocumenterPlugin"
            displayName = "Env Var Documenter"
            description = "Scans Kotlin and Java sources for System.getenv() calls and injects a Markdown table into README.md."
            tags.set(listOf("documentation", "environment-variables", "kotlin", "java", "readme"))
        }
    }
}
