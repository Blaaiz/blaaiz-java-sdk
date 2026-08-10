# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

The Blaaiz team takes security bugs seriously. We are grateful for your work to disclose your
findings responsibly, and we will do our best to acknowledge your contribution.

### How to Report

**Warning:** Do not report a security vulnerability in a public GitHub issue.

Send your report by email to **security@blaaiz.com**.

Include this information:

- The type of issue, for example a signature bypass or a credential leak
- The full paths of the source files related to the issue
- The location of the affected code as a tag, a branch, a commit, or a direct URL
- Any special configuration needed to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code, if you have it
- The impact of the issue, and how an attacker can use it

### What to Expect

- **Acknowledgment**: We acknowledge your report within 48 hours.
- **Initial assessment**: We give an initial assessment within 72 hours.
- **Status updates**: We send a status update at least every 72 hours until the issue is closed.
- **Resolution**: We try to close critical vulnerabilities within 7 days, and high-severity
  vulnerabilities within 14 days.

### Disclosure Policy

- Do not disclose the vulnerability publicly until we close it.
- We will agree a disclosure schedule with you.
- We will credit you in the security advisory, unless you prefer to stay anonymous.

## Security Best Practices for Users

When you use the Blaaiz Java SDK, obey these rules.

### 1. Keep the SDK up to date

Change the version in your `pom.xml` to the most recent release:

```xml
<dependency>
  <groupId>com.blaaiz</groupId>
  <artifactId>blaaiz-java-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

### 2. Keep credentials out of source control

Read credentials from the environment. Do not write them in code.

```java
Blaaiz blaaiz = new Blaaiz(new BlaaizClientOptions()
        .clientId(System.getenv("BLAAIZ_CLIENT_ID"))
        .clientSecret(System.getenv("BLAAIZ_CLIENT_SECRET")));
```

Rotate your client secret and your API key on a fixed schedule.

### 3. Request only the scopes you need

The SDK requests all 21 scopes when you do not set `oauthScope`. Set the scopes your
integration uses, and no more:

```java
Blaaiz blaaiz = new Blaaiz(new BlaaizClientOptions()
        .clientId(System.getenv("BLAAIZ_CLIENT_ID"))
        .clientSecret(System.getenv("BLAAIZ_CLIENT_SECRET"))
        .oauthScope("wallet:read payout:create"));
```

### 4. Validate input before you send it

Validate amounts, currencies, and account numbers in your own code. The SDK checks only that
the required fields are present.

### 5. Verify every webhook signature

Use `webhooks().constructEvent()` or `webhooks().verifySignature()` on every incoming webhook.
Pass the raw request body. A parsed and re-serialized body has different bytes, and the
signature check then fails.

```java
Map<String, Object> event = blaaiz.webhooks()
        .constructEvent(rawBody, signature, timestamp, System.getenv("BLAAIZ_WEBHOOK_SECRET"));
```

`verifySignature` compares the digests with `MessageDigest.isEqual`, which takes constant time.
This prevents a timing attack.

### 6. Do not leak details in error responses

Log the `BlaaizException` for your own diagnosis. Return a generic message to the caller.

```java
try {
    blaaiz.payouts().initiate(payoutData);
} catch (BlaaizException e) {
    log.error("Payout failed: status={} code={}", e.getStatus(), e.getErrorCode(), e);
    return ResponseEntity.status(502).body(Map.of("error", "Transaction failed"));
}
```

### 7. Never log secrets

Do not log the API key, the client secret, the webhook secret, or the OAuth access token. Do
not log full customer identity documents.

### 8. Use HTTPS

Use an `https://` base URL in production. Set `baseUrl` to `https://api.blaaiz.com`.

## Security Features

The SDK has these security features:

- **OAuth 2.0 client credentials**: The SDK caches the token in memory and refreshes it 60
  seconds before it expires.
- **Webhook verification**: HMAC-SHA256 over `timestamp.payload`, compared in constant time.
- **Input validation**: The SDK checks required fields before it sends a request.
- **Secure defaults**: HTTPS base URL, and a 30-second timeout on connect, read, and write.
- **No credential logging**: The SDK never writes credentials to a log or to an exception
  message.

## Contact

- Security reports: security@blaaiz.com
- General questions: onboarding@blaaiz.com

Thank you for your help to keep Blaaiz and our users safe.
