# Fees and Files

## Fees

Get the service with `blaaiz.fees()`.

### `getBreakdown(Map<String, Object> feeData)`

`POST /api/external/fees/breakdown`

Returns the amount that leaves your wallet, the amount that the recipient gets, and the fees
between the two. Call it before a payout, and show the numbers to your user.

```java
// From from_amount: find what the recipient gets
BlaaizResponse breakdown = blaaiz.fees().getBreakdown(Map.of(
        "from_currency_id", "NGN",
        "to_currency_id", "CAD",
        "from_amount", 100000));

// From to_amount: find what you must send
BlaaizResponse reverse = blaaiz.fees().getBreakdown(Map.of(
        "from_currency_id", "USD",
        "to_currency_id", "NGN",
        "to_amount", 50000));
```

Required:

- `from_currency_id`
- `to_currency_id`
- `from_amount` or `to_amount` — give one of the two

When you give neither amount, the SDK throws `IllegalArgumentException` with the message
`Either from_amount or to_amount is required`.

The response holds `you_send`, `recipient_gets`, and `total_fees`.

### Short form

`Blaaiz.calculateFees()` does the same call with three arguments:

```java
BlaaizResponse fees = blaaiz.calculateFees("NGN", "CAD", 100000);
```

This short form always sends `from_amount`. To use `to_amount`, call `getBreakdown()`.

## Files

Get the service with `blaaiz.files()`.

### `getPresignedUrl(Map<String, Object> fileData)`

`POST /api/external/file/get-presigned-url`

Returns a pre-signed S3 URL and a `file_id`. This is step 1 of the manual upload.

```java
BlaaizResponse presigned = blaaiz.files().getPresignedUrl(Map.of(
        "customer_id", "customer-id",
        "file_category", "identity"));
```

Required:

- `customer_id`
- `file_category` — `identity`, `identity_back`, `proof_of_address`, or `liveness_check`

The response holds `url` and `file_id`. Some responses wrap the two keys in a second `data`
key.

### The three-step manual upload

```java
// Step 1: get the URL and the file_id
BlaaizResponse presigned = blaaiz.files().getPresignedUrl(Map.of(
        "customer_id", customerId,
        "file_category", "identity"));

// Step 2: PUT the file bytes to that URL with your own HTTP client.
//         Set the Content-Type header. S3 returns an ETag header on success.

// Step 3: attach the file to the customer
BlaaizResponse association = blaaiz.customers().uploadFiles(customerId, Map.of(
        "id_file", fileId));
```

The field name in step 3 depends on `file_category`:

| `file_category`     | Field name in step 3      |
| ------------------- | ------------------------- |
| `identity`          | `id_file`                 |
| `identity_back`     | `id_file_back`            |
| `proof_of_address`  | `proof_of_address_file`   |
| `liveness_check`    | `liveness_check_file`     |

### Use the one-call version instead

`customers().uploadFileComplete()` does all three steps for you, and it detects the content
type. Read [Customers](customers.md#uploadfilecompletestring-customerid-mapstring-object-fileoptions)
for the full description.

```java
UploadFileCompleteResult result = blaaiz.customers().uploadFileComplete(customerId, Map.of(
        "file", Files.readAllBytes(Path.of("passport.jpg")),
        "file_category", "identity",
        "filename", "passport.jpg"));
```
