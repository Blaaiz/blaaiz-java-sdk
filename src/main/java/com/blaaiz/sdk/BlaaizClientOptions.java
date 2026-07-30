package com.blaaiz.sdk;

import java.util.Optional;

/**
 * Construction options for {@link BlaaizClient}.
 *
 * <p>Authenticate with either OAuth 2.0 client credentials ({@link #clientId}/{@link #clientSecret})
 * or a legacy {@link #apiKey}. When both are supplied, OAuth wins.
 */
public final class BlaaizClientOptions {

    public static final String DEFAULT_BASE_URL = "https://api-dev.blaaiz.com";
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private String apiKey;
    private String clientId;
    private String clientSecret;
    private Optional<String> oauthScope = Optional.empty();
    private String baseUrl = DEFAULT_BASE_URL;
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    public String getApiKey() {
        return apiKey;
    }

    public BlaaizClientOptions apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public BlaaizClientOptions clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public BlaaizClientOptions clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    /**
     * Space-separated OAuth scope string. Leaving this unset (the default) requests all
     * canonical scopes ({@link BlaaizClient#allScopes()} joined with spaces); an explicitly
     * supplied empty string is preserved as-is rather than falling back to the default.
     */
    public Optional<String> getOauthScope() {
        return oauthScope;
    }

    public BlaaizClientOptions oauthScope(String oauthScope) {
        this.oauthScope = Optional.ofNullable(oauthScope);
        return this;
    }

    public BlaaizClientOptions oauthScope(Optional<String> oauthScope) {
        this.oauthScope = oauthScope == null ? Optional.empty() : oauthScope;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public BlaaizClientOptions baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public BlaaizClientOptions timeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        return this;
    }
}
