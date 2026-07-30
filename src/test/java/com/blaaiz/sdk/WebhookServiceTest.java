package com.blaaiz.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private BlaaizClient client;

    private WebhookService webhooks;

    @BeforeEach
    void setUp() {
        webhooks = new WebhookService(client);
    }

    private static String hmacHex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- register ----

    @Test
    void registerSendsBothUrlsToWebhookEndpoint() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("collection_url", "https://example.com/collect");
        data.put("payout_url", "https://example.com/payout");
        when(client.makeRequest(eq("POST"), eq("/api/external/webhook"), eq(data), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("id", "wh_1"), 200, null));

        BlaaizResponse result = webhooks.register(data);

        assertEquals(200, result.getStatus());
        verify(client).makeRequest("POST", "/api/external/webhook", data, null);
    }

    @Test
    void registerThrowsWhenCollectionUrlMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("payout_url", "https://example.com/payout");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> webhooks.register(data));
        assertEquals("collection_url is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void registerThrowsWhenPayoutUrlMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("collection_url", "https://example.com/collect");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> webhooks.register(data));
        assertEquals("payout_url is required", e.getMessage());
        verifyNoInteractions(client);
    }

    // ---- get ----

    @Test
    void getSendsNoBody() {
        when(client.makeRequest(eq("GET"), eq("/api/external/webhook"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("collection_url", "x"), 200, null));

        BlaaizResponse result = webhooks.get();

        assertEquals(200, result.getStatus());
        verify(client).makeRequest("GET", "/api/external/webhook", null, null);
    }

    // ---- update ----

    @Test
    void updateForwardsDataVerbatimWithNoValidation() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("collection_url", "https://example.com/new-collect");
        when(client.makeRequest(eq("PUT"), eq("/api/external/webhook"), eq(data), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("updated", true), 200, null));

        webhooks.update(data);

        verify(client).makeRequest("PUT", "/api/external/webhook", data, null);
    }

    @Test
    void updateAllowsEmptyMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        when(client.makeRequest(eq("PUT"), eq("/api/external/webhook"), eq(data), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        webhooks.update(data);

        verify(client).makeRequest("PUT", "/api/external/webhook", data, null);
    }

    // ---- replay ----

    @Test
    void replaySendsTransactionId() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transaction_id", "txn_1");
        when(client.makeRequest(eq("POST"), eq("/api/external/webhook/replay"), eq(data), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("replayed", true), 200, null));

        webhooks.replay(data);

        verify(client).makeRequest("POST", "/api/external/webhook/replay", data, null);
    }

    @Test
    void replayThrowsWhenTransactionIdMissing() {
        Map<String, Object> data = new LinkedHashMap<>();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> webhooks.replay(data));
        assertEquals("transaction_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    // ---- simulateInteracWebhook ----

    @Test
    void simulateInteracWebhookForwardsDataVerbatimWithNoValidation() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("amount", 100);
        when(client.makeRequest(eq("POST"), eq("/api/external/mock/simulate-webhook/interac"), eq(data), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("simulated", true), 200, null));

        webhooks.simulateInteracWebhook(data);

        verify(client).makeRequest("POST", "/api/external/mock/simulate-webhook/interac", data, null);
    }

    // ---- verifySignature ----

    @Test
    void verifySignatureReturnsTrueForValidSignature() {
        String secret = "whsec_test";
        String timestamp = "1700000000";
        String rawBody = "{\"event\":\"payout.completed\"}";
        String signature = hmacHex(secret, timestamp + "." + rawBody);

        assertTrue(webhooks.verifySignature(rawBody, signature, timestamp, secret));
    }

    @Test
    void verifySignatureIsCaseInsensitiveOnSuppliedSignature() {
        String secret = "whsec_test";
        String timestamp = "1700000000";
        String rawBody = "{\"event\":\"payout.completed\"}";
        String signature = hmacHex(secret, timestamp + "." + rawBody).toUpperCase();

        assertTrue(webhooks.verifySignature(rawBody, signature, timestamp, secret));
    }

    @Test
    void verifySignatureReturnsFalseForWrongSecret() {
        String timestamp = "1700000000";
        String rawBody = "{\"event\":\"payout.completed\"}";
        String signature = hmacHex("whsec_test", timestamp + "." + rawBody);

        assertFalse(webhooks.verifySignature(rawBody, signature, timestamp, "whsec_other"));
    }

    @Test
    void verifySignatureReturnsFalseForTamperedBody() {
        String secret = "whsec_test";
        String timestamp = "1700000000";
        String signature = hmacHex(secret, timestamp + ".{\"event\":\"payout.completed\"}");

        assertFalse(webhooks.verifySignature("{\"event\":\"payout.failed\"}", signature, timestamp, secret));
    }

    @Test
    void verifySignatureReturnsFalseForNonHexSignatureRatherThanThrowing() {
        assertFalse(webhooks.verifySignature("{}", "not-hex-zzz", "1700000000", "secret"));
    }

    @Test
    void verifySignatureThrowsWhenRawBodyBlank() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> webhooks.verifySignature("", "sig", "1700000000", "secret"));
        assertEquals("Payload is required for signature verification", e.getMessage());
    }

    @Test
    void verifySignatureThrowsWhenSignatureBlank() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> webhooks.verifySignature("{}", "", "1700000000", "secret"));
        assertEquals("Signature is required for signature verification", e.getMessage());
    }

    @Test
    void verifySignatureThrowsWhenSecretBlank() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> webhooks.verifySignature("{}", "sig", "1700000000", ""));
        assertEquals("Webhook secret is required for signature verification", e.getMessage());
    }

    @Test
    void verifySignatureThrowsWhenTimestampBlank() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> webhooks.verifySignature("{}", "sig", "", "secret"));
        assertEquals("Timestamp is required for signature verification", e.getMessage());
    }

    @Test
    void verifySignatureChecksSecretBeforeTimestampWhenBothMissing() {
        // Canonical check order is rawBody, signature, secret, timestamp -- secret's message
        // must surface even though timestamp is blank too.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> webhooks.verifySignature("{}", "sig", "", ""));
        assertEquals("Webhook secret is required for signature verification", e.getMessage());
    }

    // ---- constructEvent ----

    @Test
    void constructEventReturnsParsedPayloadWithVerifiedAndTimestamp() {
        String secret = "whsec_test";
        String timestamp = "1700000000";
        String rawBody = "{\"event\":\"payout.completed\",\"id\":\"evt_1\"}";
        String signature = hmacHex(secret, timestamp + "." + rawBody);

        Instant before = Instant.now();
        Map<String, Object> event = webhooks.constructEvent(rawBody, signature, timestamp, secret);
        Instant after = Instant.now();

        assertEquals("payout.completed", event.get("event"));
        assertEquals("evt_1", event.get("id"));
        assertEquals(Boolean.TRUE, event.get("verified"));

        Instant eventTimestamp = Instant.parse((String) event.get("timestamp"));
        assertFalse(eventTimestamp.isBefore(before));
        assertFalse(eventTimestamp.isAfter(after));
    }

    @Test
    void constructEventOverwritesExistingTimestampKeyInPayload() {
        String secret = "whsec_test";
        String timestamp = "1700000000";
        String rawBody = "{\"timestamp\":\"stale-value\"}";
        String signature = hmacHex(secret, timestamp + "." + rawBody);

        Map<String, Object> event = webhooks.constructEvent(rawBody, signature, timestamp, secret);

        assertTrue(!event.get("timestamp").equals("stale-value"));
    }

    @Test
    void constructEventThrowsBlaaizExceptionForInvalidSignature() {
        String rawBody = "{\"event\":\"payout.completed\"}";

        BlaaizException e = assertThrows(BlaaizException.class,
                () -> webhooks.constructEvent(rawBody, "deadbeef", "1700000000", "secret"));
        assertEquals("Invalid webhook signature", e.getMessage());
    }

    @Test
    void constructEventThrowsBlaaizExceptionForUnparseablePayload() {
        String secret = "whsec_test";
        String timestamp = "1700000000";
        String rawBody = "not json";
        String signature = hmacHex(secret, timestamp + "." + rawBody);

        BlaaizException e = assertThrows(BlaaizException.class,
                () -> webhooks.constructEvent(rawBody, signature, timestamp, secret));
        assertEquals("Invalid webhook payload: unable to parse JSON", e.getMessage());
    }

    @Test
    void constructEventThrowsBlaaizExceptionForNonObjectJsonPayload() {
        String secret = "whsec_test";
        String timestamp = "1700000000";
        String rawBody = "[1,2,3]";
        String signature = hmacHex(secret, timestamp + "." + rawBody);

        BlaaizException e = assertThrows(BlaaizException.class,
                () -> webhooks.constructEvent(rawBody, signature, timestamp, secret));
        assertEquals("Invalid webhook payload: unable to parse JSON", e.getMessage());
    }

    @Test
    void constructEventPropagatesIllegalArgumentExceptionForBlankArguments() {
        // constructEvent delegates argument-presence validation to verifySignature, which still
        // raises the plain local-validation exception (not BlaaizException) for blank inputs.
        assertThrows(IllegalArgumentException.class,
                () -> webhooks.constructEvent(null, "sig", "1700000000", "secret"));
    }

    // ---- transport failure propagation ----

    @Test
    void getPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq("/api/external/webhook"), isNull(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 500, "SERVER_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> webhooks.get());
        assertEquals("API request failed", e.getMessage());
        assertEquals(500, e.getStatus());
        assertEquals("SERVER_ERROR", e.getErrorCode());
    }
}
