package com.example;

import static com.example.Constants.AUTH_SECRET_KEY;

/**
 * Authentication and OAuth2 configuration.
 */
public class AuthConfig {

    // Feature: JavaDoc @required true — explicit required override via private cross-file const
    /**
     * Secret key used to sign JWT tokens. Must be at least 32 characters.
     * @required true
     */
    public final String jwtSecret = System.getenv(AUTH_SECRET_KEY);

    // Feature: JavaDoc @required false — overrides the inferred required=true
    //          (no default, so without the tag the plugin would infer required=true)
    /**
     * OAuth2 client ID. Leave blank to disable OAuth2 login entirely.
     * @required false
     */
    public final String oauthClientId = System.getenv("OAUTH_CLIENT_ID");

    // Feature: JavaDoc @default overrides the second argument of getOrDefault()
    /**
     * OAuth2 redirect URI registered with the identity provider.
     * @default https://myapp.example.com/oauth/callback
     */
    public final String oauthRedirectUri = System.getenv().getOrDefault("OAUTH_REDIRECT_URI", "http://localhost:8080/callback");

    // Feature: JavaDoc @default overrides the inferred default from getOrDefault()
    /**
     * Access token lifetime in seconds.
     * @default 3600
     */
    public final String tokenExpiry = System.getenv().getOrDefault("TOKEN_EXPIRY_SECONDS", "7200");
}
