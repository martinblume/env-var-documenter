plugins {
    id("com.martinblume.env-var-documenter") version "0.1.0"
}

repositories {
    mavenLocal()               // needed so Gradle can resolve the plugin's runtime JAR
    mavenCentral()
}
