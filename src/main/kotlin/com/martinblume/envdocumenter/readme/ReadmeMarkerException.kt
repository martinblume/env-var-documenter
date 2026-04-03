package com.martinblume.envdocumenter.readme

/**
 * Thrown when the README file is missing one or both section markers, or they are in the wrong order.
 * The Gradle task catches this and re-throws it as a [org.gradle.api.GradleException].
 */
class ReadmeMarkerException(message: String) : IllegalStateException(message)
