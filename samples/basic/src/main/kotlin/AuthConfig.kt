package com.example

// Feature: KDoc @required true — overrides the inferred required=false
//          (no Elvis operator, so without KDoc the plugin would infer required=true anyway;
//           here we test the explicit override path using a private cross-file const)
/**
 * Secret key used to sign JWT tokens. Must be at least 32 characters.
 * @required true
 */
val jwtSecret = System.getenv(AUTH_SECRET_KEY)

// Feature: KDoc @required false — overrides the inferred required=true
//          (no Elvis operator, so without KDoc the plugin infers required=true)
/**
 * OAuth2 client ID. Leave blank to disable OAuth2 login entirely.
 * @required false
 */
val oauthClientId = System.getenv("OAUTH_CLIENT_ID")

// Feature: KDoc @default overrides the second argument of getOrDefault()
/**
 * OAuth2 redirect URI registered with the identity provider.
 * @default https://myapp.example.com/oauth/callback
 */
val oauthRedirectUri = System.getenv().getOrDefault("OAUTH_REDIRECT_URI", "http://localhost:8080/callback")

// Feature: KDoc @default overrides the Elvis fallback value
/**
 * Access token lifetime in seconds.
 * @default 3600
 */
val tokenExpiry = System.getenv("TOKEN_EXPIRY_SECONDS") ?: "7200"
