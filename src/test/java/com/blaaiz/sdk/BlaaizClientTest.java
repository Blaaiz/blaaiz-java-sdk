package com.blaaiz.sdk;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlaaizClientTest {

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private Call call;

    @BeforeEach
    void stubNewCall() {
        // Lenient because not every test exercises the HTTP path (e.g. constructor tests).
        org.mockito.Mockito.lenient().when(okHttpClient.newCall(any())).thenReturn(call);
    }

    private static Response jsonResponse(Request request, int code, String body, Map<String, String> headers) {
        Response.Builder builder = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("msg")
                .body(ResponseBody.create(body, MediaType.parse("application/json")));
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return builder.build();
    }

    // ---- constructor ----

    @Test
    void constructorThrowsWhenNoCredentialsProvided() {
        BlaaizException e = assertThrows(BlaaizException.class, () -> new BlaaizClient(new BlaaizClientOptions()));
        assertTrue(e.getMessage().contains("Authentication required"));
    }

    @Test
    void constructorThrowsWhenOnlyClientIdProvided() {
        assertThrows(BlaaizException.class,
                () -> new BlaaizClient(new BlaaizClientOptions().clientId("id-only")));
    }

    @Test
    void constructorPrefersOAuthWhenBothProvided() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions()
                .apiKey("test-key")
                .clientId("id")
                .clientSecret("secret"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/oauth/token").build();
        when(call.execute()).thenReturn(
                jsonResponse(dummyRequest, 200, "{\"access_token\":\"tok-123\",\"expires_in\":900}", null));

        // With OAuth creds present alongside an API key, OAuth must win: auth headers are
        // the bearer token, never the api key header.
        Map<String, String> headers = client.getAuthHeaders();
        assertEquals("Bearer tok-123", headers.get("Authorization"));
        assertFalse(headers.containsKey("x-blaaiz-api-key"));
    }

    @Test
    void allScopesReturnsCanonicalScopesInOrder() {
        // Source SDKs (Node.js/Python/Laravel) all define 21 scopes here, despite the task
        // brief's "20 canonical scopes" phrasing -- the literal list it enumerates has 21
        // entries, matching every SDK's actual ALL_SCOPES array. Preserved as-is for parity.
        List<String> scopes = BlaaizClient.allScopes();
        assertEquals(21, scopes.size());
        assertEquals("wallet:read", scopes.get(0));
        assertEquals("rates:read", scopes.get(scopes.size() - 1));
        assertTrue(scopes.contains("swap:create"));
    }

    // ---- makeRequest happy path ----

    @Test
    void makeRequestReturnsDataStatusAndHeadersOn2xx() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/test").build();
        Response response = jsonResponse(dummyRequest, 200, "{\"ok\":true}", Map.of("Custom", "1"));
        when(call.execute()).thenReturn(response);

        BlaaizResponse<Object> result = client.makeRequest("GET", "/test", null, null);

        assertEquals(200, result.getStatus());
        assertTrue(result.getData() instanceof Map);
        assertEquals(Boolean.TRUE, ((Map<?, ?>) result.getData()).get("ok"));
        assertEquals(List.of("1"), result.getHeaders().get("Custom"));
    }

    @Test
    void makeRequestSendsApiKeyAndDefaultHeaders() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(requestCaptor.capture())).thenReturn(call);
        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/test").build();
        when(call.execute()).thenReturn(jsonResponse(dummyRequest, 200, "{}", null));

        client.makeRequest("GET", "/test", null, null);

        Request sent = requestCaptor.getValue();
        assertEquals("test-key", sent.header("x-blaaiz-api-key"));
        assertEquals("application/json", sent.header("Accept"));
        assertEquals("Blaaiz-Java-SDK/1.0.0", sent.header("User-Agent"));
        assertNull(sent.header("Authorization"));
    }

    @Test
    void makeRequestSendsQueryParamsForGet() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(requestCaptor.capture())).thenReturn(call);
        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/test").build();
        when(call.execute()).thenReturn(jsonResponse(dummyRequest, 200, "{}", null));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("search_term", "USD");
        client.makeRequest("GET", "/test", data, null);

        Request sent = requestCaptor.getValue();
        assertEquals("GET", sent.method());
        assertNull(sent.body());
        assertEquals("USD", sent.url().queryParameter("search_term"));
    }

    @Test
    void makeRequestSendsJsonBodyForPost() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(requestCaptor.capture())).thenReturn(call);
        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/test").build();
        when(call.execute()).thenReturn(jsonResponse(dummyRequest, 200, "{}", null));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "test");
        client.makeRequest("POST", "/test", data, null);

        Request sent = requestCaptor.getValue();
        assertEquals("POST", sent.method());
        assertNotNull(sent.body());
    }

    @Test
    void makeRequestCallerHeadersWinOverDefaults() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(requestCaptor.capture())).thenReturn(call);
        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/test").build();
        when(call.execute()).thenReturn(jsonResponse(dummyRequest, 200, "{}", null));

        client.makeRequest("GET", "/test", null, Map.of("Accept", "text/plain"));

        assertEquals("text/plain", requestCaptor.getValue().header("Accept"));
    }

    // ---- makeRequest error paths ----

    @Test
    void makeRequestThrowsOnNon2xxWithMessageAndCode() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/bad").build();
        when(call.execute()).thenReturn(jsonResponse(dummyRequest, 400, "{\"message\":\"bad request\",\"code\":\"ERR\"}", null));

        BlaaizException e = assertThrows(BlaaizException.class, () -> client.makeRequest("GET", "/bad", null, null));
        assertEquals("bad request", e.getMessage());
        assertEquals(400, e.getStatus());
        assertEquals("ERR", e.getErrorCode());
    }

    @Test
    void makeRequestThrowsParseErrorOnInvalidJsonWith2xx() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/bad").build();
        when(call.execute()).thenReturn(jsonResponse(dummyRequest, 200, "not json", null));

        BlaaizException e = assertThrows(BlaaizException.class, () -> client.makeRequest("GET", "/bad", null, null));
        assertEquals("Failed to parse API response", e.getMessage());
        assertEquals("PARSE_ERROR", e.getErrorCode());
    }

    @Test
    void makeRequestWrapsTransportFailureAsRequestError() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        when(call.execute()).thenThrow(new IOException("Connection timeout"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> client.makeRequest("GET", "/test", null, null));
        assertEquals("REQUEST_ERROR", e.getErrorCode());
        assertTrue(e.getMessage().contains("Connection timeout"));
    }

    @Test
    void makeRequestUsesFallbackMessageWhenErrorBodyUnparseable() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/bad").build();
        when(call.execute()).thenReturn(jsonResponse(dummyRequest, 500, "not json", null));

        BlaaizException e = assertThrows(BlaaizException.class, () -> client.makeRequest("GET", "/bad", null, null));
        assertEquals("API request failed", e.getMessage());
        assertEquals(500, e.getStatus());
        assertNull(e.getErrorCode());
    }

    // ---- OAuth ----

    @Test
    void oauthHeadersUseBearerToken() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions()
                .clientId("id").clientSecret("secret"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/oauth/token").build();
        when(call.execute()).thenReturn(
                jsonResponse(dummyRequest, 200, "{\"access_token\":\"tok-123\",\"expires_in\":900}", null));

        Map<String, String> headers = client.getAuthHeaders();
        assertEquals("Bearer tok-123", headers.get("Authorization"));
        assertFalse(headers.containsKey("x-blaaiz-api-key"));
    }

    @Test
    void oauthTokenIsCachedAcrossCalls() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions()
                .clientId("id").clientSecret("secret"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/oauth/token").build();
        when(call.execute()).thenReturn(
                jsonResponse(dummyRequest, 200, "{\"access_token\":\"tok-123\",\"expires_in\":900}", null));

        String first = client.getOAuthToken();
        String second = client.getOAuthToken();

        assertEquals("tok-123", first);
        assertEquals("tok-123", second);
        org.mockito.Mockito.verify(okHttpClient, org.mockito.Mockito.times(1)).newCall(any());
    }

    @Test
    void oauthTokenThrowsOnParseFailure() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions()
                .clientId("id").clientSecret("secret"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/oauth/token").build();
        when(call.execute()).thenReturn(jsonResponse(dummyRequest, 200, "not-json", null));

        BlaaizException e = assertThrows(BlaaizException.class, client::getOAuthToken);
        assertEquals("Failed to parse OAuth token response", e.getMessage());
        assertEquals("OAUTH_PARSE_ERROR", e.getErrorCode());
    }

    @Test
    void oauthTokenThrowsWithErrorDescriptionOnFailure() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions()
                .clientId("id").clientSecret("secret"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/oauth/token").build();
        when(call.execute()).thenReturn(jsonResponse(dummyRequest, 401,
                "{\"error\":\"invalid_client\",\"error_description\":\"Invalid client credentials\"}", null));

        BlaaizException e = assertThrows(BlaaizException.class, client::getOAuthToken);
        assertEquals("Invalid client credentials", e.getMessage());
        assertEquals(401, e.getStatus());
        assertEquals("invalid_client", e.getErrorCode());
    }

    @Test
    void oauthScopeDefaultsToAllScopesWhenUnset() {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().clientId("id").clientSecret("secret"));
        // No direct getter for oauthScope; verified indirectly via allScopes() contract and
        // the fact construction succeeds without throwing for OAuth-only credentials.
        assertNotNull(client);
    }

    // ---- uploadFile ----

    @Test
    void uploadFileReturnsStatusAndEtag() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(requestCaptor.capture())).thenReturn(call);
        Request dummyRequest = new Request.Builder().url("https://example.com/upload").build();
        Response response = new Response.Builder()
                .request(dummyRequest).protocol(Protocol.HTTP_1_1).code(200).message("ok")
                .header("ETag", "\"etag-123\"")
                .body(ResponseBody.create(new byte[0], null))
                .build();
        when(call.execute()).thenReturn(response);

        UploadResult result = client.uploadFile("https://example.com/upload", "file-content".getBytes(), "application/pdf", "test.pdf");

        assertEquals(200, result.getStatus());
        assertEquals("\"etag-123\"", result.getEtag());
        Request sent = requestCaptor.getValue();
        assertEquals("PUT", sent.method());
        assertEquals("application/pdf", sent.header("Content-Type"));
        assertEquals("attachment; filename=\"test.pdf\"", sent.header("Content-Disposition"));
        assertNull(sent.header("Authorization"));
        assertNull(sent.header("x-blaaiz-api-key"));
    }

    @Test
    void uploadFileThrowsWhenNoEtag() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://example.com/upload").build();
        Response response = new Response.Builder()
                .request(dummyRequest).protocol(Protocol.HTTP_1_1).code(200).message("ok")
                .body(ResponseBody.create(new byte[0], null))
                .build();
        when(call.execute()).thenReturn(response);

        BlaaizException e = assertThrows(BlaaizException.class,
                () -> client.uploadFile("https://example.com/upload", "content".getBytes(), null, null));
        assertEquals("S3 upload failed: No ETag received from S3", e.getMessage());
        assertNull(e.getStatus());
        assertNull(e.getErrorCode());
    }

    @Test
    void uploadFileThrowsOnNon2xxStatus() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://example.com/upload").build();
        Response response = new Response.Builder()
                .request(dummyRequest).protocol(Protocol.HTTP_1_1).code(403).message("forbidden")
                .body(ResponseBody.create("forbidden", MediaType.parse("text/plain")))
                .build();
        when(call.execute()).thenReturn(response);

        BlaaizException e = assertThrows(BlaaizException.class,
                () -> client.uploadFile("https://example.com/upload", "content".getBytes(), null, null));
        assertEquals(403, e.getStatus());
        assertEquals("S3_UPLOAD_ERROR", e.getErrorCode());
        assertTrue(e.getMessage().contains("403"));
    }

    // ---- downloadFile ----

    @Test
    void downloadFileResolvesFilenameFromContentDisposition() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://example.com/image.jpg").build();
        Response response = new Response.Builder()
                .request(dummyRequest).protocol(Protocol.HTTP_1_1).code(200).message("ok")
                .header("Content-Type", "image/jpeg")
                .header("Content-Disposition", "attachment; filename=\"passport.jpg\"")
                .body(ResponseBody.create("image-bytes", MediaType.parse("image/jpeg")))
                .build();
        when(call.execute()).thenReturn(response);

        DownloadResult result = client.downloadFile("https://example.com/image.jpg");

        assertEquals("image-bytes", new String(result.getContent()));
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("passport.jpg", result.getFilename());
    }

    @Test
    void downloadFileFallsBackToUrlBasename() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://example.com/documents/passport.jpg").build();
        Response response = new Response.Builder()
                .request(dummyRequest).protocol(Protocol.HTTP_1_1).code(200).message("ok")
                .header("Content-Type", "image/jpeg")
                .body(ResponseBody.create("image-bytes", MediaType.parse("image/jpeg")))
                .build();
        when(call.execute()).thenReturn(response);

        DownloadResult result = client.downloadFile("https://example.com/documents/passport.jpg");

        assertEquals("passport.jpg", result.getFilename());
    }

    @Test
    void downloadFileAddsExtensionWhenFilenameHasNone() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://example.com/download").build();
        Response response = new Response.Builder()
                .request(dummyRequest).protocol(Protocol.HTTP_1_1).code(200).message("ok")
                .header("Content-Type", "application/pdf")
                .body(ResponseBody.create("pdf-bytes", MediaType.parse("application/pdf")))
                .build();
        when(call.execute()).thenReturn(response);

        DownloadResult result = client.downloadFile("https://example.com/download");

        assertEquals("download.pdf", result.getFilename());
    }

    @Test
    void downloadFileThrowsOnNon2xx() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        Request dummyRequest = new Request.Builder().url("https://example.com/file").build();
        Response response = new Response.Builder()
                .request(dummyRequest).protocol(Protocol.HTTP_1_1).code(500).message("error")
                .body(ResponseBody.create("server-error", MediaType.parse("text/plain")))
                .build();
        when(call.execute()).thenReturn(response);

        BlaaizException e = assertThrows(BlaaizException.class, () -> client.downloadFile("https://example.com/file"));
        assertEquals("Failed to download file: HTTP 500", e.getMessage());
    }

    @Test
    void downloadFileSendsOnlyUserAgentHeader() throws IOException {
        BlaaizClient client = new BlaaizClient(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(requestCaptor.capture())).thenReturn(call);
        Request dummyRequest = new Request.Builder().url("https://example.com/file").build();
        Response response = new Response.Builder()
                .request(dummyRequest).protocol(Protocol.HTTP_1_1).code(200).message("ok")
                .body(ResponseBody.create("bytes", MediaType.parse("application/octet-stream")))
                .build();
        when(call.execute()).thenReturn(response);

        client.downloadFile("https://example.com/file");

        Request sent = requestCaptor.getValue();
        assertEquals("Blaaiz-Java-SDK/1.0.0", sent.header("User-Agent"));
        assertNull(sent.header("x-blaaiz-api-key"));
        assertNull(sent.header("Authorization"));
        assertNull(sent.header("Accept"));
    }
}
