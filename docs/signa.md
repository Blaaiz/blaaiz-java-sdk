# Signa

Get the service with `blaaiz.signa()`. Signa creates and manages KYC/KYB verification sessions
for a business's own customers.

A session collects one or more requirements: `DOCUMENTS`, `SELFIE`, `FACE_MATCH`, or
`PROOF_OF_ADDRESS`. A session runs in one of two modes:

- `HOSTED` — Blaaiz hosts the verification flow. Use `issueVerificationLink` to get the link for
  your customer.
- `HEADLESS` — Your application collects the documents and calls the SDK directly. Use
  `createDocumentUploadUrl`, `uploadSessionDocument`, and `submitSession`.

**Rule:** `createDocumentUploadUrl`, `uploadSessionDocument`, and `submitSession` work only on a
`HEADLESS` session. `issueVerificationLink` works only on a `HOSTED` session.

The API wraps each response in a `data` key. Read the session ID at `data.data.id`:

```java
@SuppressWarnings("unchecked")
Map<String, Object> body = (Map<String, Object>) session.getData();
@SuppressWarnings("unchecked")
Map<String, Object> data = (Map<String, Object>) body.get("data");
String sessionId = String.valueOf(data.get("id"));
```

Six methods have a short alias: `create`, `list`, `get`, `submit`, `cancel`, and
`uploadDocument`. Each alias calls its full-name method. `createDocumentUploadUrl` and
`issueVerificationLink` have no alias.

## `createSession(Map<String, Object> sessionData)`

`POST /api/external/compliance/kyc/sessions`

```java
BlaaizResponse session = blaaiz.signa().createSession(Map.of(
        "customer_reference", "customer-123",
        "idempotency_key", "signa-request-123",
        "requirements", List.of("DOCUMENTS", "SELFIE", "FACE_MATCH"),
        "fulfilment_mode", "HOSTED",
        "applicant", Map.of(
                "first_name", "Ada",
                "last_name", "Lovelace",
                "country", "GBR")));
```

Required:

- `customer_reference` — max 100 characters
- `idempotency_key` — max 100 characters. A repeated key returns the same session.
- `requirements` — a supported set. See [Supported requirement sets](#supported-requirement-sets).

Optional:

- `fulfilment_mode` — `HOSTED` or `HEADLESS`
- `applicant` — an object with `first_name`, `last_name` (max 100 characters each), `dob`
  (`YYYY-MM-DD`), and `country` (the ISO 3166-1 alpha-3 code, for example `GBR`)

The SDK checks `requirements` before it sends the request. Each value must be a string, and its
upper-case form must be one of the four requirement values. The API checks `fulfilment_mode` and
`applicant`; the SDK does not.

### Supported requirement sets

The API currently accepts only these combinations of `requirements` and `fulfilment_mode`. It refuses any other combination when you create the session.

| `requirements` | `fulfilment_mode` |
| --- | --- |
| `DOCUMENTS` | `HEADLESS` |
| `DOCUMENTS`, `SELFIE`, `FACE_MATCH` | `HOSTED` |
| `DOCUMENTS`, `SELFIE`, `FACE_MATCH`, `PROOF_OF_ADDRESS` | `HOSTED` |
| `DOCUMENTS`, `PROOF_OF_ADDRESS` | `HOSTED` or `HEADLESS` |
| `PROOF_OF_ADDRESS` | `HOSTED` or `HEADLESS` |

If you do not send `fulfilment_mode`, the API uses the first mode in the row for your set. The settings of your business can limit the modes that you can use.

## `listSessions(Map<String, Object> filters)`

`GET /api/external/compliance/kyc/sessions`

Call `listSessions()` with no arguments, or pass `null`, to use the API defaults. The API then
returns the first 20 sessions.

```java
BlaaizResponse all = blaaiz.signa().listSessions();

BlaaizResponse filtered = blaaiz.signa().listSessions(Map.of(
        "limit", 20,
        "offset", 0));
```

Filters:

- `limit` — 1 to 100. The default is 20.
- `offset` — 0 or more. The default is 0.

Response data: `{ sessions: [...], total, limit, offset }`.

## `getSession(String sessionId)`

`GET /api/external/compliance/kyc/sessions/{id}`

```java
BlaaizResponse session = blaaiz.signa().getSession("session-id");
```

## `submitSession(String sessionId)`

`POST /api/external/compliance/kyc/sessions/{id}/submit`

Submits the session for verification. Sends no body. Works only on a `HEADLESS` session.

```java
BlaaizResponse submitted = blaaiz.signa().submitSession("session-id");
```

## `cancelSession(String sessionId)`

`POST /api/external/compliance/kyc/sessions/{id}/cancel`

Sends no body.

```java
BlaaizResponse cancelled = blaaiz.signa().cancelSession("session-id");
```

## `createDocumentUploadUrl(String sessionId, Map<String, Object> uploadData)`

`POST /api/external/compliance/kyc/sessions/{id}/documents/upload-url`

Requests a short-lived upload URL for a document. Works only on a `HEADLESS` session.

```java
BlaaizResponse upload = blaaiz.signa().createDocumentUploadUrl("session-id", Map.of(
        "file_name", "passport.jpg",
        "id_doc_type", "PASSPORT"));
```

Required:

- `file_name` — max 128 characters; letters, numbers, spaces, dashes, and underscores only; must
  end in `.jpg`, `.jpeg`, `.png`, `.webp`, or `.pdf`
- `id_doc_type` — one of `PASSPORT`, `ID_CARD`, `DRIVERS`, `RESIDENCE_PERMIT`, `UTILITY_BILL`,
  `BANK_STATEMENT`, or `SELFIE`

Response data: `{ url, file_name, headers }`. `PUT` your document bytes to `url` with exactly the
given `headers`. The headers are part of the URL signature, and the URL is short-lived. Then
register the returned `file_name` with `uploadSessionDocument`.

## `uploadSessionDocument(String sessionId, Map<String, Object> documentData)`

`POST /api/external/compliance/kyc/sessions/{id}/documents`

Registers a document with the session. Works only on a `HEADLESS` session. Choose one of two
transports:

- **Staged** — Call `createDocumentUploadUrl` first. `PUT` the bytes to the returned URL. Then
  pass the returned `file_name` here.
- **Inline** — Pass the document bytes directly as `content_base64`. Use this only for a very
  small file. The API can reject a request body that is larger than approximately 8 KB.

```java
// Staged upload: read the staged file_name from the createDocumentUploadUrl response first.
@SuppressWarnings("unchecked")
Map<String, Object> uploadBody = (Map<String, Object>) upload.getData();
@SuppressWarnings("unchecked")
Map<String, Object> uploadInfo = (Map<String, Object>) uploadBody.get("data");
String stagedFileName = String.valueOf(uploadInfo.get("file_name"));

BlaaizResponse registered = blaaiz.signa().uploadSessionDocument("session-id", Map.of(
        "filename", "passport.jpg",
        "content_type", "image/jpeg",
        "id_doc_type", "PASSPORT",
        "country", "GBR",
        "file_name", stagedFileName));

// Inline upload
BlaaizResponse inline = blaaiz.signa().uploadSessionDocument("session-id", Map.of(
        "filename", "passport.jpg",
        "content_type", "image/jpeg",
        "id_doc_type", "PASSPORT",
        "country", "GBR",
        "content_base64", "...base64 document bytes..."));
```

Required:

- `filename` — max 191 characters
- `content_type` — `image/jpeg`, `image/png`, `image/webp`, or `application/pdf`
- `id_doc_type` — as in `createDocumentUploadUrl`
- `country` — the ISO 3166-1 alpha-3 code
- Exactly one of `file_name` or `content_base64`

The SDK throws `IllegalArgumentException` with the message
`Provide exactly one of file_name or content_base64` when you pass neither field, or both.

## `issueVerificationLink(String sessionId)`

`POST /api/external/compliance/kyc/sessions/{id}/verification-link`

Issues or replaces the verification link for a customer. Sends no body. Works only on a `HOSTED`
session.

```java
BlaaizResponse link = blaaiz.signa().issueVerificationLink("session-id");
```

Response data: `{ verification_link, link_expires_at }`.

## Errors

The SDK throws `IllegalArgumentException` for every field listed as required above, before it
sends the request. Every method that takes a `sessionId` throws `IllegalArgumentException` with
the message `Session ID is required` when `sessionId` is `null` or empty.

The API returns HTTP 422 for a validation failure it detects, and HTTP 404 for an unknown
session. Both surface as `BlaaizException`.

## OAuth scopes

Signa needs three scopes: `compliance-kyc:read`, `compliance-kyc:create`, and
`compliance-kyc:cancel`. `BlaaizClient.allScopes()` includes them by default. A business without
Signa access can still request a token: the API drops the scopes it does not hold and keeps the
rest.
