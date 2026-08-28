package com.blaaiz.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Webhook endpoint registration/management, the Interac webhook test simulator, and
 * signature verification / event construction.
 *
 * <p>{@link #verifySignature} and {@link #constructEvent} must be called with the exact raw
 * request body string received over the wire -- never a re-serialized object. Re-serializing
 * JSON (even without any semantic change) can alter key order, whitespace, or escaping, which
 * changes the bytes fed into the HMAC and silently breaks verification. Callers are expected to
 * pull the signature/timestamp values from the {@code X-Blaaiz-Signature} /
 * {@code X-Blaaiz-Timestamp} request headers (case-insensitively, i.e.
 * {@code x-blaaiz-signature} / {@code x-blaaiz-timestamp}) by convention.
 */
public class WebhookService extends BaseService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public WebhookService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse register(Map<String, Object> webhookData) {
        requireFields(webhookData, "collection_url", "payout_url");
        return client.makeRequest("POST", "/api/external/webhook", webhookData, null);
    }

    public BlaaizResponse get() {
        return client.makeRequest("GET", "/api/external/webhook", null, null);
    }

    public BlaaizResponse update(String webhookId, Map<String, Object> webhookData) {
        requireNonBlank(webhookId, "Webhook ID is required");
        return client.makeRequest("PUT", "/api/external/webhook/" + webhookId, webhookData, null);
    }

    public BlaaizResponse replay(Map<String, Object> replayData) {
        requireFields(replayData, "transaction_id");
        return client.makeRequest("POST", "/api/external/webhook-replay", replayData, null);
    }

    /**
     * Sends a mock Interac collection webhook to your configured {@code collection_url}.
     *
     * <p>No local validation -- {@code simulateData} is forwarded verbatim. Only available
     * outside production; the API itself returns an error if called in production, this SDK
     * does not attempt to enforce that client-side.
     */
    public BlaaizResponse simulateInteracWebhook(Map<String, Object> simulateData) {
        return client.makeRequest("POST", "/api/external/mock/simulate-webhook/interac", simulateData, null);
    }

    /**
     * Verifies an HMAC-SHA256 webhook signature over {@code timestamp + "." + rawBody}.
     *
     * <p>Argument-validation order is {@code rawBody}, {@code signature}, {@code secret}, then
     * {@code timestamp} last, matching the Laravel and Python source SDKs (the Node.js SDK
     * checks timestamp before secret and was not used as the tie-breaker here) -- note this
     * differs from the method's own parameter order, which is
     * {@code (rawBody, signature, timestamp, secret)} to match all three source SDKs' public
     * signatures. This only affects which {@link IllegalArgumentException} message surfaces
     * when multiple arguments are missing at once.
     *
     * <p>Never throws for a mismatched signature -- only for missing/blank arguments -- and
     * always returns {@code false} rather than throwing when {@code signature} is not valid hex.
     */
    public boolean verifySignature(String rawBody, String signature, String timestamp, String secret) {
        requireNonBlank(rawBody, "Payload is required for signature verification");
        requireNonBlank(signature, "Signature is required for signature verification");
        requireNonBlank(secret, "Webhook secret is required for signature verification");
        requireNonBlank(timestamp, "Timestamp is required for signature verification");

        String signedPayload = timestamp + "." + rawBody;
        String expectedHex = hmacSha256Hex(secret, signedPayload);

        byte[] expectedBytes = hexToBytes(expectedHex);
        byte[] actualBytes = hexToBytes(signature.toLowerCase(Locale.ROOT));
        if (expectedBytes == null || actualBytes == null) {
            return false;
        }
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    /**
     * Verifies the signature and returns the parsed JSON payload merged with
     * {@code verified: true} and an ISO-8601 {@code timestamp} of when verification occurred
     * (overwriting any {@code timestamp} key already present in the payload).
     *
     * <p>Unlike the local argument-validation performed by {@link #verifySignature}, a failed
     * verification or an unparseable payload is reported via {@link BlaaizException} here,
     * matching all three source SDKs' choice to use their single "real" exception type
     * (Laravel's {@code BlaaizException}, Node's {@code Error}, Python's {@code ValueError})
     * for these two specific failures rather than a plain local-validation error.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> constructEvent(String payload, String signature, String timestamp, String secret) {
        if (!verifySignature(payload, signature, timestamp, secret)) {
            throw new BlaaizException("Invalid webhook signature");
        }

        Object parsed;
        try {
            parsed = OBJECT_MAPPER.readValue(payload, Object.class);
        } catch (Exception e) {
            throw new BlaaizException("Invalid webhook payload: unable to parse JSON");
        }
        if (!(parsed instanceof Map)) {
            throw new BlaaizException("Invalid webhook payload: unable to parse JSON");
        }

        Map<String, Object> event = new LinkedHashMap<>((Map<String, Object>) parsed);
        event.put("verified", true);
        event.put("timestamp", Instant.now().toString());
        return event;
    }

    private static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            return null;
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                return null;
            }
            bytes[i] = (byte) ((hi << 4) + lo);
        }
        return bytes;
    }
}
