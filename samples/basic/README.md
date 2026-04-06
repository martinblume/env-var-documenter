# env-var-sample

A sample application demonstrating every feature of the
[env-var-documenter](https://github.com/martinblume/env-var-documenter) Gradle plugin.

## Environment Variables

<!-- ENV_VARS_START -->
<!-- ENV_VARS_END -->

## Running locally

1. Copy `.env.example` to `.env` and fill in the required values.
2. Run the application with your preferred method.

## Regenerating the docs table

```bash
# From the plugin project root — publish the plugin locally first
cd ../..
./gradlew publishToMavenLocal

# Then regenerate the table in this README
cd samples/basic
./gradlew generateEnvVarDocs
```
