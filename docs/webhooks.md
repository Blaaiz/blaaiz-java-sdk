# Webhooks

Get the service with `blaaiz.webhooks()`.

## Configuration

### `register(Map<String, Object> webhookData)`

`POST /api/external/webhook`

```java
BlaaizResponse webhook = blaaiz.webhooks().register(Map.of(
        "collection_url", "https://your-domain.com/webhooks/collection",
        "payout_url", "https://your-domain.com/webhooks/payout"));
```

Required:

- `collection_url`
- `payout_url`

### `get()`

`GET /api/external/webhook`

Returns the URLs that you registered.

```java
BlaaizResponse config = blaaiz.webhooks().get();
```

### `update(String webhookId, Map<String, Object> webhookData)`

`PUT /api/external/webhook/{webhookId}`

```java
BlaaizResponse updated = blaaiz.webhooks().update("webhook-id", Map.of(
        "collection_url", "https://your-domain.com/webhooks/v2/collection",
        "payout_url", "https://your-domain.com/webhooks/v2/payout"));
```

### `replay(Map<String, Object> replayData)`

`POST /api/external/webhook-replay`

Asks Blaaiz to send the webhook for one transaction again. Use it when your endpoint was down.

```java
BlaaizResponse replay = blaaiz.webhooks().replay(Map.of(
        "transaction_id", "transaction-id"));
```

Required:

- `transaction_id`

### `simulateInteracWebhook(Map<String, Object> simulateData)`

`POST /api/external/mock/simulate-webhook/interac`

Sends a mock Interac collection webhook to your `collection_url`. Use it to test your handler.

```java
BlaaizResponse simulate = blaaiz.webhooks().simulateInteracWebhook(Map.of(
        "interac_email", "sender@example.com"));
```

**Note:** This call works outside production only. The API rejects it in production. The SDK
does not block the call itself.

## Signature verification

Blaaiz signs each webhook with HMAC-SHA256 over `timestamp + "." + payload`. It sends these two
headers:

| Header                | Content                                        |
| --------------------- | ---------------------------------------------- |
| `X-Blaaiz-Signature`  | The HMAC-SHA256 digest, in lower-case hex        |
| `X-Blaaiz-Timestamp`  | The timestamp that goes into the signed string   |

HTTP header names are case-insensitive, so `x-blaaiz-signature` is the same header.

**Warning:** Pass the raw request body. Do not pass a parsed object, and do not serialize the
body again. Re-serialization changes the key order, the whitespace, or the escaping. The bytes
then differ, and the signature check fails.

### `verifySignature(String rawBody, String signature, String timestamp, String secret)`

Returns `true` when the signature matches.

```java
boolean valid = blaaiz.webhooks().verifySignature(rawBody, signature, timestamp, webhookSecret);
```

The method compares the digests with `MessageDigest.isEqual`, which takes constant time. This
prevents a timing attack.

It returns `false` for a signature that does not match, and for a signature that is not valid
hex. It throws `IllegalArgumentException` when an argument is `null` or empty.

### `constructEvent(String payload, String signature, String timestamp, String secret)`

Verifies the signature, then returns the parsed payload. This is the recommended method.

```java
try {
    Map<String, Object> event = blaaiz.webhooks()
            .constructEvent(rawBody, signature, timestamp, webhookSecret);

    System.out.println("Transaction: " + event.get("transaction_id"));
    System.out.println("Status: " + event.get("status"));
} catch (BlaaizException e) {
    // The signature did not match, or the payload is not valid JSON.
}
```

The returned map holds the payload, plus two keys that the method adds:

| Key         | Value                                                          |
| ----------- | -------------------------------------------------------------- |
| `verified`  | `true`                                                          |
| `timestamp` | The ISO-8601 time of the verification, in UTC                   |

**Note:** The `timestamp` key overwrites a `timestamp` key that the payload already holds.

`constructEvent` throws `BlaaizException` with the message `Invalid webhook signature` when the
signature does not match. It throws `BlaaizException` with the message
`Invalid webhook payload: unable to parse JSON` when the body is not a JSON object.

## Handler examples

### Spring Boot

Take the body as a `String`. Spring then leaves the bytes unchanged.

```java
@RestController
@RequestMapping("/webhooks")
public class BlaaizWebhookController {

    private final Blaaiz blaaiz;
    private final String webhookSecret;

    public BlaaizWebhookController(Blaaiz blaaiz,
                                   @Value("${blaaiz.webhook-secret}") String webhookSecret) {
        this.blaaiz = blaaiz;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/collection")
    public ResponseEntity<Map<String, Object>> collection(
            @RequestBody String rawBody,
            @RequestHeader("X-Blaaiz-Signature") String signature,
            @RequestHeader("X-Blaaiz-Timestamp") String timestamp) {

        try {
            Map<String, Object> event =
                    blaaiz.webhooks().constructEvent(rawBody, signature, timestamp, webhookSecret);

            // Process the collection: update your database, send a notification.
            return ResponseEntity.ok(Map.of("received", true));
        } catch (BlaaizException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid signature"));
        }
    }

    @PostMapping("/payout")
    public ResponseEntity<Map<String, Object>> payout(
            @RequestBody String rawBody,
            @RequestHeader("X-Blaaiz-Signature") String signature,
            @RequestHeader("X-Blaaiz-Timestamp") String timestamp) {

        try {
            Map<String, Object> event =
                    blaaiz.webhooks().constructEvent(rawBody, signature, timestamp, webhookSecret);

            // Process the payout result.
            return ResponseEntity.ok(Map.of("received", true));
        } catch (BlaaizException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid signature"));
        }
    }
}
```

### Jakarta Servlet

```java
@WebServlet("/webhooks/collection")
public class BlaaizWebhookServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String signature = request.getHeader("X-Blaaiz-Signature");
        String timestamp = request.getHeader("X-Blaaiz-Timestamp");

        try {
            Map<String, Object> event = blaaiz.webhooks()
                    .constructEvent(rawBody, signature, timestamp, webhookSecret);

            // Process the event.
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (BlaaizException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
```

## Rules for a webhook handler

1. Read the raw body before any JSON library reads it.
2. Verify the signature before you act on the payload.
3. Return HTTP 200 for a webhook that you accepted.
4. Return HTTP 400 for a signature that does not match.
5. Return the response fast. Do the slow work on a queue.
6. Handle the same webhook twice without a second effect. Blaaiz can send a webhook again.
7. Read the webhook secret from an environment variable.
