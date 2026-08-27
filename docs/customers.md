# Customers

Get the service with `blaaiz.customers()`.

## `create(Map<String, Object> customerData)`

`POST /api/external/customer`

```java
BlaaizResponse customer = blaaiz.customers().create(Map.of(
        "first_name", "John",
        "last_name", "Doe",
        "type", "individual",
        "email", "john.doe@example.com",
        "country", "NG",
        "id_type", "passport",
        "id_number", "A12345678"));
```

Required for every type:

- `type` — `individual` or `business`
- `email`
- `country` — the ISO country code, for example `NG`

Required for `type` `individual`:

- `first_name`
- `last_name`
- `id_type` — `drivers_license`, `passport`, `id_card`, or `resident_permit`
- `id_number`

Required for `type` `business`:

- `business_name`
- `registration_number`
- `incorporation_country` — the ISO country code

The `id_type` and `id_number` fields are for an individual customer only. The API rejects them
for a business customer.

```java
BlaaizResponse business = blaaiz.customers().create(Map.of(
        "type", "business",
        "email", "ops@acme.example.com",
        "country", "NG",
        "business_name", "Acme Ltd",
        "registration_number", "RC123456",
        "incorporation_country", "NG"));
```

The API wraps the new customer in a second `data` key. Read the identifier at `data.data.id`:

```java
@SuppressWarnings("unchecked")
Map<String, Object> body = (Map<String, Object>) customer.getData();
@SuppressWarnings("unchecked")
Map<String, Object> data = (Map<String, Object>) body.get("data");
String customerId = String.valueOf(data.get("id"));
```

## `list(Map<String, Object> filters)`

`GET /api/external/customer`

Pass `null` to list every customer.

```java
BlaaizResponse all = blaaiz.customers().list(null);

BlaaizResponse filtered = blaaiz.customers().list(Map.of(
        "email", "john@example.com",
        "verification_status", "VERIFIED",
        "type", "individual",
        "paginate", true));
```

Supported filters:

| Filter                  | Description                                              |
| ----------------------- | -------------------------------------------------------- |
| `email`                 | Match one email address                                   |
| `id_number`             | Match one identity document number                        |
| `registration_number`   | Match one business registration number                    |
| `verification_status`   | For example `VERIFIED`                                    |
| `type`                  | `individual` or `business`                                |
| `paginate`              | Set `true` to get `links` and `meta` beside `data`         |

The SDK sends the filters as query parameters. It skips a filter whose value is `null`.

## `get(String customerId)`

`GET /api/external/customer/{customerId}`

```java
BlaaizResponse customer = blaaiz.customers().get("customer-id");
```

Throws `IllegalArgumentException` when `customerId` is `null` or empty.

## `update(String customerId, Map<String, Object> updateData)`

`PUT /api/external/customer/{customerId}`

```java
BlaaizResponse updated = blaaiz.customers().update("customer-id", Map.of(
        "first_name", "Jane",
        "email", "jane.doe@example.com"));
```

The SDK sends `updateData` without a change. It validates only `customerId`.

## `addKyc(String customerId, Map<String, Object> kycData)`

`POST /api/external/customer/{customerId}/kyc-data`

```java
BlaaizResponse kyc = blaaiz.customers().addKyc("customer-id", Map.of(
        "id_type", "passport",
        "id_number", "A12345678"));
```

The SDK sends `kycData` without a change.

## `uploadFiles(String customerId, Map<String, Object> fileData)`

`POST /api/external/customer/{customerId}/files`

Attaches a file that you already uploaded to a pre-signed URL. Use the `file_id` that
`files().getPresignedUrl()` returned.

```java
BlaaizResponse association = blaaiz.customers().uploadFiles("customer-id", Map.of(
        "id_file", "file-id-from-the-presigned-url-call"));
```

For the one-call version, use `uploadFileComplete` below.

## `listBeneficiaries(String customerId)`

`GET /api/external/customer/{customerId}/beneficiary`

```java
BlaaizResponse beneficiaries = blaaiz.customers().listBeneficiaries("customer-id");
```

## `getBeneficiary(String customerId, String beneficiaryId)`

`GET /api/external/customer/{customerId}/beneficiary/{beneficiaryId}`

```java
BlaaizResponse beneficiary = blaaiz.customers().getBeneficiary("customer-id", "beneficiary-id");
```

## `submit(String customerId)`

`POST /api/external/customer/{customerId}/submit`

Submits the customer for KYC or KYB verification. This call sends no body.

```java
blaaiz.customers().submit("customer-id");
```

## `upgradeKybScope(String customerId, Map<String, Object> upgradeData)`

`POST /api/external/customer/{customerId}/upgrade-kyb-scope`

Upgrades a business customer from MINIMAL to FULL KYB scope.

```java
blaaiz.customers().upgradeKybScope("customer-id", Map.of(
        "owners", List.of(Map.of(
                "first_name", "Jane",
                "last_name", "Doe",
                "ownership_percentage", 100))));
```

Required:

- `owners` — a list with at least one owner. The ownership percentages must sum to exactly 100.

The SDK throws `IllegalArgumentException` when `owners` is missing or empty.

## `deleteOwner(String customerId, String ownerId)`

`DELETE /api/external/customer/{customerId}/owner/{ownerId}`

```java
blaaiz.customers().deleteOwner("customer-id", "owner-id");
```

## `getOwnerFilePresignedUrl(String customerId, String ownerId, Map<String, Object> presignedData)`

`POST /api/external/customer/{customerId}/owner/{ownerId}/file/presigned-url`

```java
BlaaizResponse presigned = blaaiz.customers().getOwnerFilePresignedUrl("customer-id", "owner-id", Map.of(
        "file_category", "id_document_front"));
```

Required:

- `file_category` — `id_document_front` or `id_document_back`

## `uploadOwnerFiles(String customerId, String ownerId, Map<String, Object> fileData)`

`POST /api/external/customer/{customerId}/owner/{ownerId}/files`

```java
blaaiz.customers().uploadOwnerFiles("customer-id", "owner-id", Map.of(
        "id_document_front", "file-uuid-front",
        "id_document_back", "file-uuid-back"));
```

Required:

- `id_document_front` — the file identifier for the front of the document

Optional:

- `id_document_back` — the file identifier for the back of the document

## Documents

Use these methods to manage the business documents of a customer.

```java
// List the documents
BlaaizResponse documents = blaaiz.customers().listDocuments("customer-id");

// Get one document
BlaaizResponse document = blaaiz.customers().getDocument("customer-id", "document-id");

// Get a pre-signed URL for a document upload. This call sends no body.
BlaaizResponse presigned = blaaiz.customers().getDocumentPresignedUrl("customer-id");

// Create a document
BlaaizResponse created = blaaiz.customers().createDocument("customer-id", Map.of(
        "type", "PROOF_OF_ADDRESS",
        "name", "Utility bill",
        "file_id", "file-uuid"));

// Update a document
BlaaizResponse updated = blaaiz.customers().updateDocument("customer-id", "document-id", Map.of(
        "name", "Renamed document"));

// Delete a document
blaaiz.customers().deleteDocument("customer-id", "document-id");
```

`createDocument` requires `type`, `name`, and `file_id`. The `type` value is one of these:
`CERTIFICATE_OF_INCORPORATION`, `ARTICLES_OF_INCORPORATION`, `BENEFICIAL_OWNERSHIP_CERTIFICATE`,
`INCORPORATION_DOCUMENTS`, `CAC_STATUS_REPORT`, `ACCOUNT_AGREEMENT`, `PROOF_OF_ADDRESS`,
`BANK_STATEMENT`, `LICENSE`, `SHARE_REGISTRATION`, `COMPANY_OWNERSHIP_STRUCTURE`,
`DIRECTORS_REGISTER`, or `OTHER`.

## `uploadFileComplete(String customerId, Map<String, Object> fileOptions)`

Does the three upload steps in one call:

1. `POST /api/external/file/get-presigned-url` to get the URL and the `file_id`.
2. `PUT` the file bytes to the pre-signed S3 URL.
3. `POST /api/external/customer/{customerId}/files` to attach the file.

```java
byte[] content = Files.readAllBytes(Path.of("passport.jpg"));

UploadFileCompleteResult result = blaaiz.customers().uploadFileComplete("customer-id", Map.of(
        "file", content,
        "file_category", "identity",
        "filename", "passport.jpg",
        "content_type", "image/jpeg"));
```

### Options

| Key             | Required | Description                                                        |
| --------------- | -------- | ------------------------------------------------------------------ |
| `file`          | Yes      | A `byte[]`, a base64 string, a data URL, or a public `http(s)` URL   |
| `file_category` | Yes      | `identity`, `identity_back`, `proof_of_address`, or `liveness_check` |
| `filename`      | No       | Used for the `Content-Disposition` header and for type detection     |
| `content_type`  | No       | Detected from the file when you leave it out                         |

### The four `file` input types

```java
// 1. Binary data
Map.of("file", Files.readAllBytes(path), "file_category", "identity");

// 2. Plain base64
Map.of("file", "iVBORw0KGgoAAAANSU...", "file_category", "identity");

// 3. Data URL: the SDK reads the content type from the prefix
Map.of("file", "data:image/png;base64,iVBORw0KGgo...", "file_category", "identity");

// 4. Public URL: the SDK downloads the file first
Map.of("file", "https://example.com/passport.jpg", "file_category", "identity");
```

For a public URL, the SDK follows any redirect. It reads the filename from the
`Content-Disposition` header. When that header is absent, it uses the last path segment of the
URL, and it adds an extension from the `Content-Type` header.

### Content type detection

When you do not set `content_type`, the SDK tries these steps in order:

1. The content type from the data URL prefix, for a data URL input.
2. The `Content-Type` header from the download, for a public URL input.
3. The magic bytes at the start of the file.
4. The extension of `filename`.

When all four fail, the SDK throws a `BlaaizException` that asks you to set `content_type`.

### The result

| Method                       | Type              | Description                             |
| ---------------------------- | ----------------- | --------------------------------------- |
| `getFileId()`                | `String`          | The file identifier from the API         |
| `getPresignedUrl()`          | `String`          | The S3 URL that the SDK uploaded to      |
| `getAssociationResponse()`   | `BlaaizResponse`  | The response of the attach-file call     |

### Errors

`uploadFileComplete` validates `customerId`, `file`, and `file_category` first, and throws
`IllegalArgumentException` for those. Every later failure becomes one `BlaaizException` whose
message starts with `File upload failed:`.
