package com.blaaiz.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Low-level transport/auth layer shared by the {@code Blaaiz} facade and all services.
 *
 * <p>Resolves OAuth-vs-API-key authentication at construction, performs the OAuth 2.0
 * client-credentials flow with in-memory token caching, builds default headers, executes
 * generic JSON HTTP calls, and provides the raw presigned-URL S3 upload / arbitrary-URL
 * download primitives used by {@code CustomerService.uploadFileComplete}.
 *
 * <p>This client makes a single attempt per call: there is no retry/backoff logic and no
 * automatic pagination anywhere in this class.
 */
public class BlaaizClient {

    private static final String USER_AGENT = "Blaaiz-Java-SDK/1.0.0";

    private static final List<String> ALL_SCOPES = Collections.unmodifiableList(List.of(
            "wallet:read", "currency:read", "bank:read", "customer:read", "customer:write",
            "beneficiary:read", "virtual-account:read", "virtual-account:create", "virtual-account:close",
            "collection:create", "collection:crypto:create", "collection:interac:accept",
            "payout:create", "swap:create", "transaction:read", "fees:read", "file:upload",
            "webhook:read", "webhook:write", "webhook:replay", "rates:read"
    ));

    private static final Map<String, String> MIME_TO_EXTENSION;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("image/jpeg", ".jpg");
        m.put("image/jpg", ".jpg");
        m.put("image/png", ".png");
        m.put("image/gif", ".gif");
        m.put("image/webp", ".webp");
        m.put("image/bmp", ".bmp");
        m.put("image/tiff", ".tiff");
        m.put("application/pdf", ".pdf");
        m.put("text/plain", ".txt");
        m.put("application/msword", ".doc");
        m.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
        MIME_TO_EXTENSION = Collections.unmodifiableMap(m);
    }

    // Mirrors the [\'"]).*?\2|[^;\n]* Content-Disposition filename pattern used by the
    // Laravel/Node.js SDKs; the captured group may include surrounding quote characters,
    // which are stripped afterwards.
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)");

    private final String apiKey;
    private final String clientId;
    private final String clientSecret;
    private final String oauthScope;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final boolean useOAuth;
    private final Map<String, String> defaultHeaders;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private Long tokenExpiresAt;

    public BlaaizClient(BlaaizClientOptions options) {
        this(options, null);
    }

    /** Package-private constructor allowing tests to inject a mock/fake transport. */
    BlaaizClient(BlaaizClientOptions options, OkHttpClient httpClient) {
        BlaaizClientOptions opts = options != null ? options : new BlaaizClientOptions();

        this.clientId = nullToEmpty(opts.getClientId());
        this.clientSecret = nullToEmpty(opts.getClientSecret());
        this.apiKey = nullToEmpty(opts.getApiKey());
        this.oauthScope = opts.getOauthScope().orElseGet(() -> String.join(" ", ALL_SCOPES));

        String configuredBaseUrl = opts.getBaseUrl() != null ? opts.getBaseUrl() : BlaaizClientOptions.DEFAULT_BASE_URL;
        this.baseUrl = stripTrailingSlash(configuredBaseUrl);
        this.timeoutSeconds = opts.getTimeoutSeconds();

        this.useOAuth = isPresent(this.clientId) && isPresent(this.clientSecret);
        if (!this.useOAuth && !isPresent(this.apiKey)) {
            throw new BlaaizException(
                    "Authentication required: provide either client_id and client_secret for OAuth, "
                            + "or api_key for legacy authentication");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("User-Agent", USER_AGENT);
        if (!this.useOAuth) {
            headers.put("x-blaaiz-api-key", this.apiKey);
        }
        this.defaultHeaders = Collections.unmodifiableMap(headers);

        this.httpClient = httpClient != null ? httpClient : new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .readTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .build();
    }

    /** The 21 canonical OAuth scopes, in fixed order. */
    public static List<String> allScopes() {
        return ALL_SCOPES;
    }

    /**
     * Executes a generic JSON HTTP call against the Blaaiz API.
     *
     * <p>{@code GET} sends {@code data} as query parameters; every other method sends it as a
     * JSON request body. Headers are merged as {@code defaultHeaders -> fresh per-call auth
     * headers -> headers} (later wins). A 2xx response is parsed and returned; anything else,
     * or a request that fails outright, is raised as a {@link BlaaizException}.
     */
    public BlaaizResponse makeRequest(String method, String endpoint, Object data, Map<String, String> headers) {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        String upperMethod = method.toUpperCase(Locale.ROOT);

        // Resolved before the request try/catch so that OAuth failures surface as-is,
        // not wrapped as an "unexpected" transport error.
        Map<String, String> authHeaders = getAuthHeaders();

        try {
            HttpUrl parsedUrl = HttpUrl.parse(baseUrl + endpoint);
            if (parsedUrl == null) {
                throw new BlaaizException("Invalid request URL: " + baseUrl + endpoint, null, "REQUEST_ERROR");
            }
            HttpUrl.Builder urlBuilder = parsedUrl.newBuilder();
            if ("GET".equals(upperMethod) && data != null) {
                for (Map.Entry<String, Object> entry : toParamMap(data).entrySet()) {
                    if (entry.getValue() == null) {
                        continue;
                    }
                    urlBuilder.addQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            RequestBody body = null;
            if (!"GET".equals(upperMethod) && !"HEAD".equals(upperMethod)) {
                byte[] json = data != null ? objectMapper.writeValueAsBytes(data) : new byte[0];
                // No MediaType here on purpose: the Content-Type header below (from
                // defaultHeaders, or overridden by a caller header) is the single source of
                // truth, rather than letting OkHttp derive/overwrite it from the body.
                body = RequestBody.create(json, null);
            }

            Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build()).method(upperMethod, body);
            applyHeaders(requestBuilder, defaultHeaders);
            applyHeaders(requestBuilder, authHeaders);
            applyHeaders(requestBuilder, headers);

            try (Response response = this.httpClient.newCall(requestBuilder.build()).execute()) {
                int status = response.code();
                String bodyStr = response.body() != null ? response.body().string() : "";
                Map<String, List<String>> responseHeaders = response.headers().toMultimap();

                if (status >= 200 && status < 300) {
                    Object parsed;
                    try {
                        parsed = objectMapper.readValue(bodyStr, Object.class);
                    } catch (IOException e) {
                        throw new BlaaizException("Failed to parse API response", status, "PARSE_ERROR");
                    }
                    return new BlaaizResponse(parsed, status, responseHeaders);
                }

                Map<String, Object> errorData = tryParseJsonObject(bodyStr);
                String message = errorData != null && errorData.get("message") != null
                        ? String.valueOf(errorData.get("message"))
                        : "API request failed";
                String code = errorData != null && errorData.get("code") != null
                        ? String.valueOf(errorData.get("code"))
                        : null;
                throw new BlaaizException(message, status, code);
            }
        } catch (BlaaizException e) {
            throw e;
        } catch (InterruptedIOException e) {
            // SocketTimeoutException (connect/read/write timeout) extends InterruptedIOException,
            // so this branch must precede the general IOException one below. Mirrors the Node.js
            // SDK, which raises TIMEOUT_ERROR rather than REQUEST_ERROR when a request times out.
            throw new BlaaizException("Request timeout", null, "TIMEOUT_ERROR");
        } catch (IOException e) {
            throw new BlaaizException("Request failed: " + e.getMessage(), null, "REQUEST_ERROR");
        } catch (RuntimeException e) {
            throw new BlaaizException("Unexpected error: " + e.getMessage(), null, "UNEXPECTED_ERROR");
        }
    }

    /**
     * Bare PUT of {@code fileContent} to a presigned S3 URL: no auth headers, only the
     * optional {@code Content-Type}/{@code Content-Disposition} headers set below.
     */
    public UploadResult uploadFile(String presignedUrl, byte[] fileContent, String contentType, String filename) {
        Objects.requireNonNull(presignedUrl, "presignedUrl must not be null");
        byte[] content = fileContent != null ? fileContent : new byte[0];

        try {
            RequestBody body = RequestBody.create(content, null);
            Request.Builder requestBuilder = new Request.Builder().url(presignedUrl).put(body);
            if (contentType != null) {
                requestBuilder.header("Content-Type", contentType);
            }
            if (filename != null) {
                requestBuilder.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            }

            try (Response response = this.httpClient.newCall(requestBuilder.build()).execute()) {
                int status = response.code();
                if (status < 200 || status >= 300) {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    throw new BlaaizException(
                            "S3 upload failed with status " + status + ": " + bodyStr, status, "S3_UPLOAD_ERROR");
                }

                String etag = response.header("ETag");
                if (etag == null) {
                    throw new BlaaizException("S3 upload failed: No ETag received from S3");
                }
                return new UploadResult(status, etag);
            }
        } catch (BlaaizException e) {
            throw e;
        } catch (IOException e) {
            throw new BlaaizException("S3 upload request failed: " + e.getMessage(), null, "S3_REQUEST_ERROR");
        }
    }

    /** Bare GET of an arbitrary URL, sending only a {@code User-Agent} header (no auth). */
    public DownloadResult downloadFile(String url) {
        Objects.requireNonNull(url, "url must not be null");

        try {
            Request request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build();

            try (Response response = this.httpClient.newCall(request).execute()) {
                int status = response.code();
                byte[] content = response.body() != null ? response.body().bytes() : new byte[0];

                if (status < 200 || status >= 300) {
                    throw new BlaaizException("Failed to download file: HTTP " + status);
                }

                String contentType = response.header("Content-Type");
                String filename = extractFilenameFromContentDisposition(response.header("Content-Disposition"));

                if (filename == null) {
                    filename = basenameFromUrl(url);
                    if (filename == null || filename.isEmpty()) {
                        filename = "download";
                    }
                    if (!hasExtension(filename) && contentType != null) {
                        String extension = extensionFromContentType(contentType);
                        if (extension != null) {
                            filename += extension;
                        }
                    }
                }

                return new DownloadResult(content, contentType, filename);
            }
        } catch (BlaaizException e) {
            throw e;
        } catch (IOException e) {
            throw new BlaaizException("File download failed: " + e.getMessage());
        }
    }

    /**
     * Returns the cached OAuth access token if it has not yet crossed the 60-second refresh
     * buffer before expiry, otherwise fetches a new one via the client-credentials grant.
     */
    protected synchronized String getOAuthToken() {
        long now = Instant.now().getEpochSecond();
        if (accessToken != null && tokenExpiresAt != null && now < tokenExpiresAt) {
            return accessToken;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("scope", oauthScope)
                .build();

        Request request = new Request.Builder().url(baseUrl + "/oauth/token").post(formBody).build();

        try (Response response = this.httpClient.newCall(request).execute()) {
            int status = response.code();
            String bodyStr = response.body() != null ? response.body().string() : "";

            if (status < 200 || status >= 300) {
                Map<String, Object> errorData = tryParseJsonObject(bodyStr);
                String message = errorData != null && stringOrNull(errorData.get("error_description")) != null
                        ? stringOrNull(errorData.get("error_description"))
                        : errorData != null && stringOrNull(errorData.get("message")) != null
                                ? stringOrNull(errorData.get("message"))
                                : "OAuth token request failed: HTTP " + status;
                String code = errorData != null && stringOrNull(errorData.get("error")) != null
                        ? stringOrNull(errorData.get("error"))
                        : "OAUTH_ERROR";
                throw new BlaaizException(message, status, code);
            }

            Map<String, Object> parsed = tryParseJsonObject(bodyStr);
            if (parsed == null || parsed.get("access_token") == null) {
                throw new BlaaizException("Failed to parse OAuth token response", status, "OAUTH_PARSE_ERROR");
            }

            long expiresIn = 900L;
            Object expiresInRaw = parsed.get("expires_in");
            if (expiresInRaw instanceof Number) {
                expiresIn = ((Number) expiresInRaw).longValue();
            } else if (expiresInRaw != null) {
                try {
                    expiresIn = Long.parseLong(String.valueOf(expiresInRaw));
                } catch (NumberFormatException ignored) {
                    expiresIn = 900L;
                }
            }

            accessToken = String.valueOf(parsed.get("access_token"));
            tokenExpiresAt = Instant.now().getEpochSecond() + expiresIn - 60;
            return accessToken;
        } catch (BlaaizException e) {
            throw e;
        } catch (IOException e) {
            throw new BlaaizException("OAuth token request failed: " + e.getMessage(), null, "OAUTH_ERROR");
        }
    }

    /**
     * Resolves the authentication header(s) for a single request: OAuth returns a bearer
     * {@code Authorization} header (recomputed/refreshed every call), API-key mode returns
     * {@code x-blaaiz-api-key}.
     */
    protected Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        if (useOAuth) {
            headers.put("Authorization", "Bearer " + getOAuthToken());
        } else {
            headers.put("x-blaaiz-api-key", apiKey);
        }
        return headers;
    }

    private static void applyHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers == null) {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            builder.header(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toParamMap(Object data) {
        if (data instanceof Map) {
            Map<Object, Object> raw = (Map<Object, Object>) data;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> entry : raw.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tryParseJsonObject(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            return parsed instanceof Map ? (Map<String, Object>) parsed : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String extractFilenameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }
        Matcher matcher = FILENAME_PATTERN.matcher(contentDisposition);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        if (value == null) {
            return null;
        }
        value = value.replace("'", "").replace("\"", "").trim();
        return value.isEmpty() ? null : value;
    }

    private static String basenameFromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                return null;
            }
            List<String> segments = new ArrayList<>(List.of(path.split("/")));
            for (int i = segments.size() - 1; i >= 0; i--) {
                if (!segments.get(i).isEmpty()) {
                    return segments.get(i);
                }
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean hasExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 && dot < filename.length() - 1;
    }

    private static String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String base = contentType.split(";")[0].trim();
        return MIME_TO_EXTENSION.get(base);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * A value is "present" for auth-resolution purposes if it is non-null, non-empty, and
     * not the literal string {@code "0"} -- matching the Node.js and Laravel SDKs, which both
     * treat {@code "0"} as an absent credential (PHP's {@code empty("0")} is {@code true}).
     */
    private static boolean isPresent(String value) {
        return value != null && !value.isEmpty() && !"0".equals(value);
    }
}
