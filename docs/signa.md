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
`uploadDocument`. Each alias calls its full-name method. `createDocumentUploadUrl`,
`issueVerificationLink`, and the three methods in [Read captured data](#read-captured-data) have
no alias.

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

## Read captured data

Three methods read the personal data that Signa captured during verification. Each method needs
the `compliance-kyc:pii:read` scope. See [OAuth scopes](#oauth-scopes).

**Warning:** Each response carries personal data. The API sends `Cache-Control: no-store`. Do
not log the response body. Do not cache the response body.

Each method returns HTTP 409 until the session reaches a verdict: `APPROVED` or `REJECTED`.

### `getSessionApplicantData(String sessionId)`

`GET /api/external/compliance/kyc/sessions/{id}/applicant-data`

Returns the applicant data that Signa captured: name, date of birth, nationality, address, and
the presented identity document.

```java
BlaaizResponse applicant = blaaiz.signa().getSessionApplicantData("session-id");
```

Response data:

```
{ session_id, first_name, middle_name, last_name, date_of_birth, country, nationality,
  address: { line, city, state, postal_code },
  document: { type, number, issuing_country, issue_date, expiry_date } | null,
  extracted_at }
```

Every field except `session_id` and `extracted_at` can be `null`. The `document.number` field is
`null` when the live provider read failed. The whole `data` value is `null` when the session has
a verdict but Signa did not capture applicant data.

### `listSessionDocuments(String sessionId)`

`GET /api/external/compliance/kyc/sessions/{id}/documents`

Lists the documents that Signa captured for the session.

```java
BlaaizResponse documents = blaaiz.signa().listSessionDocuments("session-id");
```

Response data is an array. Each entry has this shape:

```
{ id, kind, document_type, document_side, content_type, available, unavailable_reason }
```

- `kind` is one of `DOCUMENT`, `SELFIE`, `PROOF_OF_ADDRESS`, `LIVENESS_REFERENCE`, or `OTHER`.
- `document_side` is `FRONT_SIDE`, `BACK_SIDE`, or `null`.
- `unavailable_reason` is `NOT_RETAINED`, `RETRIEVAL_FAILED`, or `null`.

### `getSessionDocument(String sessionId, String documentId)`

`GET /api/external/compliance/kyc/sessions/{id}/documents/{documentId}`

Returns a download link for one document. The link expires after 15 minutes. The response never
contains the document bytes.

Anyone who has the download link can download the document until the link expires. Do not log
the link. Do not send it to a client that you do not control.

```java
BlaaizResponse download = blaaiz.signa().getSessionDocument("session-id", "document-id");
```

Response data: `{ url, content_type, expires_at }`.

The API returns HTTP 410 when Signa no longer retains the document. This method is rate limited
to 30 requests per minute and 600 requests per hour, per business. The API returns HTTP 429 above
these limits.

## Errors

The SDK throws `IllegalArgumentException` for every field listed as required above, before it
sends the request. Every method that takes a `sessionId` throws `IllegalArgumentException` with
the message `Session ID is required` when `sessionId` is `null` or empty. `getSessionDocument`
also throws `IllegalArgumentException` with the message `Document ID is required` when
`documentId` is `null` or empty.

The API returns HTTP 422 for a validation failure it detects, and HTTP 404 for an unknown
session. Both surface as `BlaaizException`. The [Read captured data](#read-captured-data) methods
can also return HTTP 403, 409, 410, or 429; see that section for the meaning of each one.

## OAuth scopes

Signa needs four scopes: `compliance-kyc:read`, `compliance-kyc:create`, `compliance-kyc:cancel`,
and `compliance-kyc:pii:read`. `BlaaizClient.allScopes()` includes all four by default.

Blaaiz grants `compliance-kyc:pii:read` to a credential only on request. The API drops a
requested scope that the credential does not hold, so a token request still succeeds without it.
A business without this grant gets a token that lacks `compliance-kyc:pii:read`, and the methods
in [Read captured data](#read-captured-data) then return HTTP 403. Only OAuth tokens are
scope-checked.
