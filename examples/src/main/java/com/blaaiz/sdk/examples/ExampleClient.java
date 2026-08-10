package com.blaaiz.sdk.examples;

import com.blaaiz.sdk.Blaaiz;
import com.blaaiz.sdk.BlaaizClientOptions;

/**
 * Builds the {@link Blaaiz} instance that the other examples share.
 *
 * <p>Credentials come from the environment. Set either {@code BLAAIZ_CLIENT_ID} and
 * {@code BLAAIZ_CLIENT_SECRET} for OAuth, or {@code BLAAIZ_API_KEY} for the legacy API key.
 * Set {@code BLAAIZ_API_URL} to choose the environment; it defaults to the development API.
 */
public final class ExampleClient {

    private ExampleClient() {
    }

    public static Blaaiz create() {
        BlaaizClientOptions options = new BlaaizClientOptions();

        String clientId = System.getenv("BLAAIZ_CLIENT_ID");
        String clientSecret = System.getenv("BLAAIZ_CLIENT_SECRET");
        String apiKey = System.getenv("BLAAIZ_API_KEY");

        if (clientId != null && clientSecret != null) {
            options.clientId(clientId).clientSecret(clientSecret);
        } else if (apiKey != null) {
            options.apiKey(apiKey);
        } else {
            throw new IllegalStateException(
                    "Set BLAAIZ_CLIENT_ID and BLAAIZ_CLIENT_SECRET, or set BLAAIZ_API_KEY");
        }

        String baseUrl = System.getenv("BLAAIZ_API_URL");
        if (baseUrl != null) {
            options.baseUrl(baseUrl);
        }

        return new Blaaiz(options);
    }

    /** Reads a required environment variable, or fails with a clear message. */
    public static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Set the " + name + " environment variable");
        }
        return value;
    }
}
