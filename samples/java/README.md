# env-var-sample-java

A sample Java application demonstrating every feature of the
[env-var-documenter](https://github.com/martinblume/env-var-documenter) Gradle plugin
with pure Java source files.

## Environment Variables

<!-- ENV_VARS_START -->
| Variable | Description | Required | Default |
|---|---|---|---|
| API_BASE_URL | Base URL for the API. Example: \`http://localhost:8080/api\` — note the \*scheme\* and \_path\_ prefix. See \[RFC 3986] for the URL format. On Windows paths use C:\\inetpub\\wwwroot style. Für Umlaute: äöü werden unterstützt. | no | http://localhost:8080 |
| AUTH_SECRET | Secret key used to sign JWT tokens. Must be at least 32 characters. | yes | — |
| CACHE_TTL_SECONDS | Cache entry TTL in seconds. | no | 300 |
| DATABASE_HOST | Hostname of the primary database server. | yes | — |
| DATABASE_NAME | Name of the database schema to connect to. | yes | — |
| DATABASE_PORT | Port the database listens on. | no | 5432 |
| DB_MAX_CONNECTIONS | Maximum number of pooled database connections. | no | 10 |
| FEATURE_FLAG_HOST | — | yes | — |
| OAUTH_CLIENT_ID | OAuth2 client ID. Leave blank to disable OAuth2 login entirely. | no | — |
| OAUTH_REDIRECT_URI | OAuth2 redirect URI registered with the identity provider. | no | https://myapp.example.com/oauth/callback |
| SERVER_HOST | Bind address for the HTTP server. | yes | — |
| SERVER_PORT | Port the HTTP server listens on. Supports ports 1–65535. | no | 8080 |
| TLS_CERT_PATH | TLS certificate file path. Required for HTTPS. | yes | — |
| TOKEN_EXPIRY_SECONDS | Access token lifetime in seconds. | no | 3600 |
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
cd samples/java
./gradlew generateEnvVarDocs
```
