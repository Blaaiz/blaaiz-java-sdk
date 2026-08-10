package com.blaaiz.sdk.examples;

import com.blaaiz.sdk.Blaaiz;
import com.blaaiz.sdk.BlaaizException;
import com.blaaiz.sdk.BlaaizResponse;

import java.util.Map;

/**
 * Registers the webhook endpoints, then verifies an incoming webhook.
 *
 * <p>Blaaiz signs each webhook with HMAC-SHA256 over {@code timestamp + "." + payload}. It
 * sends the signature in the {@code X-Blaaiz-Signature} header and the timestamp in the
 * {@code X-Blaaiz-Timestamp} header.
 *
 * <p><b>Warning:</b> pass the raw request body to {@code verifySignature} and to
 * {@code constructEvent}. A body that you parsed and serialized again has different bytes, and
 * the signature check then fails. In a servlet, read the body with
 * {@code request.getInputStream()} before any JSON library reads it.
 */
public final class WebhookVerificationExample {

    private WebhookVerificationExample() {
    }

    public static void main(String[] args) {
        Blaaiz blaaiz = ExampleClient.create();

        registerEndpoints(blaaiz);

        // In a real application these three values come from the incoming HTTP request.
        String rawBody = "{\"transaction_id\":\"txn-123\",\"status\":\"SUCCESSFUL\"}";
        String signature = "the-x-blaaiz-signature-header";
        String timestamp = "the-x-blaaiz-timestamp-header";

        handleWebhook(blaaiz, rawBody, signature, timestamp);
    }

    /** Tells Blaaiz where to send the collection webhooks and the payout webhooks. */
    static void registerEndpoints(Blaaiz blaaiz) {
        try {
            BlaaizResponse registered = blaaiz.webhooks().register(Map.of(
                    "collection_url", "https://your-domain.com/webhooks/collection",
                    "payout_url", "https://your-domain.com/webhooks/payout"));
            System.out.println("Registered webhooks: " + registered.getStatus());

            BlaaizResponse current = blaaiz.webhooks().get();
            System.out.println("Current configuration: " + current.getData());
        } catch (BlaaizException e) {
            System.err.println("Webhook registration failed: " + e.getMessage());
        }
    }

    /**
     * Verifies the signature and builds the event. Return HTTP 200 when this method succeeds,
     * and HTTP 400 when it throws.
     */
    static void handleWebhook(Blaaiz blaaiz, String rawBody, String signature, String timestamp) {
        String secret = ExampleClient.requireEnv("BLAAIZ_WEBHOOK_SECRET");

        try {
            Map<String, Object> event = blaaiz.webhooks()
                    .constructEvent(rawBody, signature, timestamp, secret);

            System.out.println("Transaction: " + event.get("transaction_id"));
            System.out.println("Status: " + event.get("status"));
            System.out.println("Verified: " + event.get("verified"));

            // Process the event here: update your database, send a notification.
        } catch (BlaaizException e) {
            System.err.println("Webhook verification failed: " + e.getMessage());
        }
    }

    /** Checks the signature without building the event, when you parse the payload yourself. */
    static boolean checkSignatureOnly(Blaaiz blaaiz, String rawBody, String signature, String timestamp) {
        String secret = ExampleClient.requireEnv("BLAAIZ_WEBHOOK_SECRET");
        return blaaiz.webhooks().verifySignature(rawBody, signature, timestamp, secret);
    }

    /** Asks Blaaiz to send a webhook again for one transaction. */
    static void replay(Blaaiz blaaiz, String transactionId) {
        blaaiz.webhooks().replay(Map.of("transaction_id", transactionId));
    }
}
