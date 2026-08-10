# Blaaiz Java SDK

Official Java SDK for the Blaaiz RaaS (Remittance as a Service) API. The SDK gives you methods
for payouts, collections, customer management, KYC document upload, wallets, and more.

Requires JDK 11 or later.

## Installation

### Maven

```xml
<dependency>
  <groupId>com.blaaiz</groupId>
  <artifactId>blaaiz-java-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.blaaiz:blaaiz-java-sdk:1.0.0'
```

## Quick Start

```java
import com.blaaiz.sdk.Blaaiz;
import com.blaaiz.sdk.BlaaizClientOptions;
import com.blaaiz.sdk.BlaaizResponse;

// The SDK uses the development environment by default.
Blaaiz blaaiz = new Blaaiz(new BlaaizClientOptions()
        .apiKey(System.getenv("BLAAIZ_API_KEY")));

// For production, set the base URL:
// Blaaiz blaaiz = new Blaaiz(new BlaaizClientOptions()
//         .apiKey(System.getenv("BLAAIZ_API_KEY"))
//         .baseUrl("https://api.blaaiz.com"));

boolean connected = blaaiz.testConnection();
System.out.println("API connected: " + connected);
```

## Authentication

The SDK supports two authentication methods.

### OAuth 2.0 client credentials (recommended for new integrations)

Supply a client ID and a client secret. The SDK then runs the OAuth `client_credentials` flow
for you. It gets a bearer token from `/oauth/token`, caches the token in memory, and refreshes
the token 60 seconds before it expires.

```java
Blaaiz blaaiz = new Blaaiz(new BlaaizClientOptions()
        .clientId(System.getenv("BLAAIZ_CLIENT_ID"))
        .clientSecret(System.getenv("BLAAIZ_CLIENT_SECRET"))
        // .oauthScope("wallet:read payout:create") // Optional: defaults to all scopes
        // .baseUrl("https://api.blaaiz.com")       // Optional: defaults to the dev environment
        // .timeoutSeconds(30)                      // Optional: request timeout in seconds
);
```

When you do not set `oauthScope`, the SDK requests all 21 supported scopes. To see the list,
call `BlaaizClient.allScopes()`.

Token refresh is thread-safe. Concurrent requests share one cached token.

### API key (legacy)

API keys still work for existing integrations.

```java
Blaaiz blaaiz = new Blaaiz(new BlaaizClientOptions()
        .apiKey(System.getenv("BLAAIZ_API_KEY")));
```

When you configure both OAuth credentials and an API key, the SDK uses OAuth.

## Features

- **Customer management**: Create, update, and manage customers with KYC verification
- **Collections**: Open Banking, Card, Crypto, Bank Transfer, and Interac
- **Payouts**: Bank transfer, Interac, ACH, Wire, and Crypto across many currencies
- **Virtual bank accounts**: Create and manage virtual accounts for NGN collections
- **Wallets**: Multi-currency wallet management
- **Transactions**: Transaction history and status
- **Webhooks**: Webhook configuration and signature verification
- **Files**: Document upload with pre-signed URLs
- **Fees**: Fee calculations and breakdowns
- **Banks and currencies**: The supported banks and currencies
- **Rates**: FX rate lookups
- **Swaps**: Currency swaps between business wallets

## Supported Currencies and Methods

### Collections

- **CAD**: Interac (push mechanism)
- **NGN**: Bank transfer (virtual bank account) and card payment
- **USD**: Card payment
- **EUR/GBP**: Open Banking

### Payouts

- **Bank transfer**: NGN, GBP, EUR
- **Interac**: CAD transactions
- **ACH**: USD transactions
- **Wire**: USD transactions
- **Crypto**: USDT and USDC on many networks

## Working with Requests and Responses

The SDK takes request payloads as `Map<String, Object>`. The keys are the `snake_case` field
names of the Blaaiz API. The SDK forwards each payload without a change, so you can send any
field in the [Blaaiz API reference](https://docs.business.blaaiz.com), including a field that
this README does not list.

Every service method returns a `BlaaizResponse`:

| Method        | Type                        | Description                                     |
| ------------- | --------------------------- | ----------------------------------------------- |
| `getData()`   | `Object`                    | The parsed JSON body: a `Map`, a `List`, or null |
| `getStatus()` | `int`                       | The HTTP status code                             |
| `getHeaders()`| `Map<String, List<String>>` | The response headers                             |

`getData()` returns `Object` because the API returns different shapes for different endpoints.
Cast the value to the shape that the endpoint documents:

```java
BlaaizResponse response = blaaiz.wallets().list();

@SuppressWarnings("unchecked")
Map<String, Object> body = (Map<String, Object>) response.getData();
System.out.println("Status: " + response.getStatus());
```

**Note:** Many endpoints wrap their payload in a second `data` key. A created customer is at
`data.data.id`, not `data.id`. The SDK does not unwrap this second key for you. The examples
below show the exact path for each endpoint.

## API Reference

The sections below show the most common calls. For the full reference, read the pages in
[`docs/`](docs/):

- [Root SDK: the facade and the composite workflows](docs/root-sdk.md)
- [Customers](docs/customers.md)
- [Collections](docs/collections.md)
- [Payouts](docs/payouts.md)
- [Wallets and virtual bank accounts](docs/wallets-and-vbas.md)
- [Transactions, banks, currencies, and rates](docs/transactions-banks-currencies-rates.md)
- [Fees and files](docs/fees-files.md)
- [Swaps](docs/swaps.md)
- [Webhooks](docs/webhooks.md)

### Services

Get each service from the `Blaaiz` facade:

| Method                     | Service                      |
| -------------------------- | ---------------------------- |
| `customers()`              | `CustomerService`            |
| `collections()`            | `CollectionService`          |
| `payouts()`                | `PayoutService`              |
| `wallets()`                | `WalletService`              |
| `virtualBankAccounts()`    | `VirtualBankAccountService`  |
| `transactions()`           | `TransactionService`         |
| `banks()`                  | `BankService`                |
| `currencies()`             | `CurrencyService`            |
| `fees()`                   | `FeesService`                |
| `files()`                  | `FileService`                |
| `webhooks()`               | `WebhookService`             |
| `rates()`                  | `RateService`                |
| `swaps()`                  | `SwapService`                |

### Customer Management

#### Create a customer

```java
BlaaizResponse customer = blaaiz.customers().create(Map.of(
        "first_name", "John",
        "last_name", "Doe",
        "type", "individual",          // individual or business
        "email", "john.doe@example.com",
        "country", "NG",
        "id_type", "passport",         // drivers_license, passport, id_card, resident_permit
        "id_number", "A12345678"
        // "business_name", "Company Name" // Required when type is business
));

@SuppressWarnings("unchecked")
Map<String, Object> body = (Map<String, Object>) customer.getData();
@SuppressWarnings("unchecked")
Map<String, Object> data = (Map<String, Object>) body.get("data");
System.out.println("Customer ID: " + data.get("id"));
```

**Note:** For the `individual` type, `first_name` and `last_name` are required. For the
`business` type, `business_name` is required instead.

#### Get a customer

```java
BlaaizResponse customer = blaaiz.customers().get("customer-id");
```

#### List customers

```java
BlaaizResponse customers = blaaiz.customers().list(null);
```

You can also pass filters and ask for pagination. The supported filters are `email`,
`id_number`, `registration_number`, `verification_status`, and `type`. Set `paginate` to `true`
to get a response that has `links` and `meta` beside `data`. `meta` holds `current_page`,
`total`, and the other page counters.

```java
BlaaizResponse verified = blaaiz.customers().list(Map.of(
        "email", "john@example.com",
        "verification_status", "VERIFIED",
        "type", "individual",
        "paginate", true
));
```

#### Update a customer

```java
BlaaizResponse updated = blaaiz.customers().update("customer-id", Map.of(
        "first_name", "Jane",
        "email", "jane.doe@example.com"
));
```

#### List the beneficiaries of a customer

```java
BlaaizResponse beneficiaries = blaaiz.customers().listBeneficiaries("customer-id");
BlaaizResponse beneficiary = blaaiz.customers().getBeneficiary("customer-id", "beneficiary-id");
```

### File Management and KYC

#### Upload a customer document

**Method 1: Complete file upload (recommended)**

`uploadFileComplete` does all three steps for you. It gets the pre-signed URL, uploads the file
to S3, and attaches the file to the customer.

```java
import com.blaaiz.sdk.UploadFileCompleteResult;

// Option A: upload from a byte array
byte[] fileBytes = Files.readAllBytes(Path.of("passport.jpg"));
UploadFileCompleteResult result = blaaiz.customers().uploadFileComplete("customer-id", Map.of(
        "file", fileBytes,
        "file_category", "identity",   // identity, identity_back, proof_of_address, liveness_check
        "filename", "passport.jpg",    // Optional
        "content_type", "image/jpeg"   // Optional: detected from the bytes when absent
));

// Option B: upload from a plain base64 string
UploadFileCompleteResult fromBase64 = blaaiz.customers().uploadFileComplete("customer-id", Map.of(
        "file", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
        "file_category", "identity"
));

// Option C: upload from a data URL; the SDK reads the content type from the prefix
UploadFileCompleteResult fromDataUrl = blaaiz.customers().uploadFileComplete("customer-id", Map.of(
        "file", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
        "file_category", "identity"
));

// Option D: upload from a public URL; the SDK downloads the file first
UploadFileCompleteResult fromUrl = blaaiz.customers().uploadFileComplete("customer-id", Map.of(
        "file", "https://example.com/documents/passport.jpg",
        "file_category", "identity"
));

System.out.println("File ID: " + result.getFileId());
System.out.println("Presigned URL: " + result.getPresignedUrl());
System.out.println("Association status: " + result.getAssociationResponse().getStatus());
```

`file` accepts four input types:

| Input type   | Java type | Example                                      |
| ------------ | --------- | -------------------------------------------- |
| Binary data  | `byte[]`  | `Files.readAllBytes(path)`                   |
| Base64       | `String`  | `"iVBORw0KGgoAAAANSU..."`                    |
| Data URL     | `String`  | `"data:image/jpeg;base64,/9j/4AAQ..."`       |
| Public URL   | `String`  | `"https://example.com/passport.jpg"`         |

When you do not set `content_type`, the SDK detects it from the magic bytes of the file. If
that fails, the SDK reads the extension of `filename`. If both fail, the SDK throws a
`BlaaizException` and asks you to set `content_type`.

**Method 2: Manual 3-step process**

```java
// Step 1: get a pre-signed URL
BlaaizResponse presigned = blaaiz.files().getPresignedUrl(Map.of(
        "customer_id", "customer-id",
        "file_category", "identity"
));

// Step 2: upload the file to that URL with your own HTTP client

// Step 3: attach the file to the customer
BlaaizResponse association = blaaiz.customers().uploadFiles("customer-id", Map.of(
        "id_file", "file-id-from-step-1"
));
```

### Collections

#### Open Banking collection (EUR/GBP)

```java
BlaaizResponse collection = blaaiz.collections().initiate(Map.of(
        "customer_id", "customer-id",
        "wallet_id", "wallet-id",
        "amount", 100.00,
        "currency", "EUR",             // EUR, GBP, NGN, USD
        "method", "open_banking",
        "email", "customer@example.com",
        "reference", "your-reference",
        "redirect_url", "https://your-site.com/callback"
));
```

#### Card collection (NGN/USD)

```java
BlaaizResponse collection = blaaiz.collections().initiate(Map.of(
        "customer_id", "customer-id",
        "wallet_id", "wallet-id",
        "amount", 5000,
        "currency", "NGN",
        "method", "card"
));
```

#### Accept an Interac money request (CAD)

```java
// With a security answer (standard transfer)
BlaaizResponse interac = blaaiz.collections().acceptInteracMoneyRequest(Map.of(
        "reference_number", "interac-reference",
        "security_answer", "answer",
        "email", "sender@example.com"   // Optional
));

// Auto deposit needs no security answer
BlaaizResponse autoDeposit = blaaiz.collections().acceptInteracMoneyRequest(Map.of(
        "reference_number", "interac-reference"
));
```

#### Crypto collection

```java
BlaaizResponse networks = blaaiz.collections().getCryptoNetworks();

BlaaizResponse cryptoCollection = blaaiz.collections().initiateCrypto(Map.of(
        "amount", 100,
        "network", "ethereum",
        "token", "USDT",
        "wallet_id", "wallet-id"
));
```

#### Attach a customer to a collection

```java
BlaaizResponse attachment = blaaiz.collections().attachCustomer(Map.of(
        "customer_id", "customer-id",
        "transaction_id", "transaction-id"
));
```

### Payouts

#### Bank transfer payout (NGN)

```java
BlaaizResponse payout = blaaiz.payouts().initiate(Map.of(
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "method", "bank_transfer",
        "from_amount", 1000,           // Use from_amount or to_amount
        "from_currency_id", "NGN",
        "to_currency_id", "NGN",
        "bank_id", "bank-id",          // Required for NGN
        "account_number", "0123456789",
        "phone_number", "+2348012345678"
));
```

#### Bank transfer payout (GBP)

```java
BlaaizResponse gbpPayout = blaaiz.payouts().initiate(Map.of(
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "method", "bank_transfer",
        "from_amount", 100,
        "from_currency_id", "GBP",
        "to_currency_id", "GBP",
        "sort_code", "123456",
        "account_number", "12345678",
        "account_name", "John Doe"
));
```

#### Bank transfer payout (EUR)

```java
BlaaizResponse eurPayout = blaaiz.payouts().initiate(Map.of(
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "method", "bank_transfer",
        "from_amount", 100,
        "from_currency_id", "EUR",
        "to_currency_id", "EUR",
        "iban", "DE89370400440532013000",
        "bic_code", "COBADEFFXXX",
        "account_name", "John Doe"
));
```

#### Interac payout (CAD)

```java
BlaaizResponse interacPayout = blaaiz.payouts().initiate(Map.of(
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "method", "interac",
        "from_amount", 100,
        "from_currency_id", "CAD",
        "to_currency_id", "CAD",
        "email", "recipient@example.com",
        "interac_first_name", "John",
        "interac_last_name", "Doe"
));
```

#### ACH payout (USD)

`Map.of` holds at most 10 pairs, so build the larger payloads with a `LinkedHashMap`.

```java
Map<String, Object> achPayout = new LinkedHashMap<>();
achPayout.put("wallet_id", "wallet-id");
achPayout.put("customer_id", "customer-id");
achPayout.put("method", "ach");
achPayout.put("from_amount", 100);
achPayout.put("from_currency_id", "USD");
achPayout.put("to_currency_id", "USD");
achPayout.put("type", "individual");       // individual or business
achPayout.put("account_number", "123456789");
achPayout.put("account_name", "John Doe");
achPayout.put("account_type", "checking"); // checking or savings
achPayout.put("bank_name", "Chase Bank");
achPayout.put("routing_number", "021000021");

BlaaizResponse response = blaaiz.payouts().initiate(achPayout);
```

#### Wire payout (USD)

```java
Map<String, Object> wirePayout = new LinkedHashMap<>();
wirePayout.put("wallet_id", "wallet-id");
wirePayout.put("customer_id", "customer-id");
wirePayout.put("method", "wire");
wirePayout.put("from_amount", 1000);
wirePayout.put("from_currency_id", "USD");
wirePayout.put("to_currency_id", "USD");
wirePayout.put("type", "individual");
wirePayout.put("account_number", "123456789");
wirePayout.put("account_name", "John Doe");
wirePayout.put("account_type", "checking");
wirePayout.put("bank_name", "Chase Bank");
wirePayout.put("routing_number", "021000021");
wirePayout.put("swift_code", "CHASUS33");

BlaaizResponse response = blaaiz.payouts().initiate(wirePayout);
```

#### Crypto payout

```java
BlaaizResponse cryptoPayout = blaaiz.payouts().initiate(Map.of(
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "method", "crypto",
        "from_amount", 100,
        "from_currency_id", "USD",
        "to_currency_id", "USDT",
        "wallet_address", "0x1234567890abcdef...",
        "wallet_token", "USDT",                  // USDT or USDC
        "wallet_network", "ETHEREUM_MAINNET"     // BSC_MAINNET, ETHEREUM_MAINNET, TRON_MAINNET, MATIC_MAINNET
));
```

#### Use to_amount instead of from_amount

To fix the amount that the recipient gets, send `to_amount`:

```java
BlaaizResponse payout = blaaiz.payouts().initiate(Map.of(
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "method", "bank_transfer",
        "to_amount", 50000,            // The recipient gets exactly this amount
        "from_currency_id", "USD",
        "to_currency_id", "NGN",
        "bank_id", "bank-id",
        "account_number", "0123456789"
));
```

### Virtual Bank Accounts

```java
// Create
BlaaizResponse vba = blaaiz.virtualBankAccounts().create(Map.of(
        "wallet_id", "wallet-id",
        "account_name", "John Doe"
));

// List: pass null for a filter that you do not need
BlaaizResponse all = blaaiz.virtualBankAccounts().list(null, null);
BlaaizResponse byWallet = blaaiz.virtualBankAccounts().list("wallet-id", null);
BlaaizResponse byCustomer = blaaiz.virtualBankAccounts().list(null, "customer-id");

// Get one
BlaaizResponse account = blaaiz.virtualBankAccounts().get("vba-id");

// Close, with or without a reason
BlaaizResponse closed = blaaiz.virtualBankAccounts().close("vba-id", null);
BlaaizResponse closedWithReason = blaaiz.virtualBankAccounts().close("vba-id", "No longer needed");
```

#### Get the identification type

This call returns the identification document that a country and a customer type need.

```java
// By customer ID, when the customer exists
BlaaizResponse idType = blaaiz.virtualBankAccounts().getIdentificationType("customer-id", null, null);

// By country and type, for a new customer
BlaaizResponse idTypeNew = blaaiz.virtualBankAccounts().getIdentificationType(null, "NG", "individual");
```

### Wallets

```java
BlaaizResponse wallets = blaaiz.wallets().list();
BlaaizResponse wallet = blaaiz.wallets().get("wallet-id");
```

### Transactions

```java
BlaaizResponse transactions = blaaiz.transactions().list(Map.of(
        "page", 1,
        "limit", 10,
        "status", "SUCCESSFUL"
));

BlaaizResponse transaction = blaaiz.transactions().get("transaction-id");
```

### Banks and Currencies

```java
BlaaizResponse banks = blaaiz.banks().list();

BlaaizResponse accountInfo = blaaiz.banks().lookupAccount(Map.of(
        "account_number", "0123456789",
        "bank_id", "1"
));

BlaaizResponse currencies = blaaiz.currencies().list();
```

### Rates

```java
BlaaizResponse allRates = blaaiz.rates().list(null);
BlaaizResponse ngnRates = blaaiz.rates().list("NGN");
```

### Swaps

`swaps().swap()` moves money between two business wallets in different currencies.

```java
BlaaizResponse swap = blaaiz.swaps().swap(Map.of(
        "from_business_wallet_id", "wallet-id-usd",
        "to_business_wallet_id", "wallet-id-ngn",
        "amount", 500
));
```

### Fees

```java
// From from_amount: find what the recipient gets
BlaaizResponse feeBreakdown = blaaiz.fees().getBreakdown(Map.of(
        "from_currency_id", "NGN",
        "to_currency_id", "CAD",
        "from_amount", 100000
));

// From to_amount: find what you must send
BlaaizResponse reverse = blaaiz.fees().getBreakdown(Map.of(
        "from_currency_id", "USD",
        "to_currency_id", "NGN",
        "to_amount", 50000
));
```

### Webhooks

```java
// Register
BlaaizResponse webhook = blaaiz.webhooks().register(Map.of(
        "collection_url", "https://your-domain.com/webhooks/collection",
        "payout_url", "https://your-domain.com/webhooks/payout"
));

// Read the current configuration
BlaaizResponse config = blaaiz.webhooks().get();

// Replay a webhook
BlaaizResponse replay = blaaiz.webhooks().replay(Map.of(
        "transaction_id", "transaction-id"
));

// Send a mock Interac webhook. This works outside production only.
BlaaizResponse simulate = blaaiz.webhooks().simulateInteracWebhook(Map.of(
        "interac_email", "sender@example.com"
));
```

## Advanced Usage

### Complete payout workflow

`createCompletePayout` creates the customer, gets a fee breakdown, and starts the payout.

```java
import com.blaaiz.sdk.CompletePayoutConfig;
import com.blaaiz.sdk.CompletePayoutResult;

CompletePayoutConfig config = new CompletePayoutConfig()
        .customerData(Map.of(
                "first_name", "John",
                "last_name", "Doe",
                "type", "individual",
                "email", "john@example.com",
                "country", "NG",
                "id_type", "passport",
                "id_number", "A12345678"))
        .payoutData(Map.of(
                "wallet_id", "wallet-id",
                "method", "bank_transfer",
                "from_amount", 1000,
                "from_currency_id", "NGN",
                "to_currency_id", "NGN",
                "account_number", "0123456789",
                "bank_id", "bank-id",
                "phone_number", "+2348012345678"));

CompletePayoutResult result = blaaiz.createCompletePayout(config);

System.out.println("Customer ID: " + result.getCustomerId());
System.out.println("Payout: " + result.getPayout());
System.out.println("Fees: " + result.getFees());
```

**Note:** The fee breakdown call always sends `from_amount`. When your `payoutData` holds only
`to_amount`, this call fails. This behavior is the same in the Node.js, Laravel, and Python
SDKs. To use `to_amount`, call `fees().getBreakdown()` and `payouts().initiate()` yourself.

### Complete collection workflow

`createCompleteCollection` creates the customer, optionally creates a virtual bank account, and
starts the collection.

```java
import com.blaaiz.sdk.CompleteCollectionConfig;
import com.blaaiz.sdk.CompleteCollectionResult;

CompleteCollectionConfig config = new CompleteCollectionConfig()
        .customerData(Map.of(
                "first_name", "Jane",
                "last_name", "Smith",
                "type", "individual",
                "email", "jane@example.com",
                "country", "NG",
                "id_type", "drivers_license",
                "id_number", "ABC123456"))
        .collectionData(Map.of(
                "method", "card",
                "amount", 5000,
                "currency", "NGN",
                "wallet_id", "wallet-id"))
        .createVba(true);

CompleteCollectionResult result = blaaiz.createCompleteCollection(config);

System.out.println("Customer ID: " + result.getCustomerId());
System.out.println("Collection: " + result.getCollection());
System.out.println("Virtual account: " + result.getVirtualAccount());
```

### Convenience methods

The facade also has these short forms:

```java
blaaiz.getCustomerById("customer-id");
blaaiz.getTransactionById("transaction-id");
blaaiz.getWalletById("wallet-id");
blaaiz.getAllCurrencies();
blaaiz.getAllBanks();
blaaiz.calculateFees("NGN", "CAD", 100000);
```

## Error Handling

The SDK throws two exception types:

| Exception                  | When                                                       |
| -------------------------- | ---------------------------------------------------------- |
| `IllegalArgumentException` | A required field is missing before the SDK sends a request  |
| `BlaaizException`          | The API returned an error, or the request itself failed     |

Both are unchecked, so you do not have to catch them.

```java
import com.blaaiz.sdk.BlaaizException;

try {
    BlaaizResponse customer = blaaiz.customers().create(customerData);
} catch (IllegalArgumentException e) {
    System.err.println("Invalid input: " + e.getMessage());
} catch (BlaaizException e) {
    System.err.println("Blaaiz API error: " + e.getMessage());
    System.err.println("Status code: " + e.getStatus());
    System.err.println("Error code: " + e.getErrorCode());

    if (e.isClientError()) {
        // 4xx: fix the request before you retry it
    } else if (e.isServerError()) {
        // 5xx: retry with an increasing delay
    }
}
```

`BlaaizException` also has `toMap()` and `toJson()` for structured logging.

### Error codes

`getErrorCode()` returns the `code` field of the API error. When the failure happens before the
API replies, `getErrorCode()` returns one of these SDK codes:

| Code                | Meaning                                            |
| ------------------- | -------------------------------------------------- |
| `TIMEOUT_ERROR`     | The request passed the configured timeout           |
| `REQUEST_ERROR`     | The request did not reach the API                   |
| `PARSE_ERROR`       | The SDK could not parse the response body           |
| `UNEXPECTED_ERROR`  | An unexpected failure inside the SDK                |
| `OAUTH_ERROR`       | The OAuth token request failed                      |
| `OAUTH_PARSE_ERROR` | The OAuth token response held no `access_token`     |
| `S3_UPLOAD_ERROR`   | S3 rejected the file upload                         |
| `S3_REQUEST_ERROR`  | The upload request to S3 did not complete           |

## Rate Limiting

The Blaaiz API allows 100 requests per minute. Every response carries the rate limit headers:

- `X-RateLimit-Limit`: the maximum requests per minute
- `X-RateLimit-Remaining`: the requests left in the current window
- `X-RateLimit-Reset`: the time when the window resets

Read them from `getHeaders()`:

```java
BlaaizResponse response = blaaiz.wallets().list();
List<String> remaining = response.getHeaders().get("x-ratelimit-remaining");
```

**Note:** The SDK makes one attempt per call. It has no retry and no backoff. Add your own
retry with an increasing delay when you get a 429 status.

## Webhook Handling

### Verify a webhook signature

Blaaiz signs each webhook with HMAC-SHA256 over `timestamp + "." + payload`.

**Warning:** `payload` must be the raw request body, exactly as it arrived. Do not pass a
parsed object. Parsing and re-serializing changes the bytes and breaks the signature check.

```java
// Method 1: check the signature yourself
boolean valid = blaaiz.webhooks().verifySignature(
        rawBody,        // The raw request body
        signature,      // The X-Blaaiz-Signature header
        timestamp,      // The X-Blaaiz-Timestamp header
        webhookSecret);

// Method 2: build a verified event (recommended)
try {
    Map<String, Object> event = blaaiz.webhooks()
            .constructEvent(rawBody, signature, timestamp, webhookSecret);

    // event holds the payload, plus verified=true and the verification timestamp
    System.out.println("Transaction: " + event.get("transaction_id"));
} catch (BlaaizException e) {
    System.err.println("Webhook verification failed: " + e.getMessage());
}
```

`verifySignature` compares the digests with `MessageDigest.isEqual`, which takes constant time.
This prevents a timing attack.

### Spring Boot webhook handler

Take the body as a `String`, not as a parsed object. This keeps the bytes unchanged.

```java
@RestController
@RequestMapping("/webhooks")
public class BlaaizWebhookController {

    private final Blaaiz blaaiz;
    private final String webhookSecret;

    public BlaaizWebhookController(Blaaiz blaaiz, @Value("${blaaiz.webhook-secret}") String webhookSecret) {
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
}
```

Return 200 for a webhook that you accepted. Return 400 for a webhook whose signature is not
valid.

## Environment Configuration

The SDK uses the development environment by default. Set `baseUrl` to change it:

```java
// Development (default)
Blaaiz dev = new Blaaiz(new BlaaizClientOptions()
        .apiKey(System.getenv("BLAAIZ_API_KEY"))
        .baseUrl("https://api-dev.blaaiz.com"));

// Production
Blaaiz prod = new Blaaiz(new BlaaizClientOptions()
        .apiKey(System.getenv("BLAAIZ_API_KEY"))
        .baseUrl("https://api.blaaiz.com"));
```

### Environment variables (recommended)

| Variable                | Purpose                                                |
| ----------------------- | ------------------------------------------------------ |
| `BLAAIZ_CLIENT_ID`      | The OAuth client ID                                     |
| `BLAAIZ_CLIENT_SECRET`  | The OAuth client secret                                 |
| `BLAAIZ_OAUTH_SCOPE`    | The OAuth scopes; defaults to all 21 scopes             |
| `BLAAIZ_API_KEY`        | The legacy API key                                      |
| `BLAAIZ_API_URL`        | The base URL; defaults to the dev environment           |
| `BLAAIZ_WEBHOOK_SECRET` | The secret for webhook signature verification           |

```java
BlaaizClientOptions options = new BlaaizClientOptions()
        .clientId(System.getenv("BLAAIZ_CLIENT_ID"))
        .clientSecret(System.getenv("BLAAIZ_CLIENT_SECRET"));

String baseUrl = System.getenv("BLAAIZ_API_URL");
if (baseUrl != null) {
    options.baseUrl(baseUrl);
}

Blaaiz blaaiz = new Blaaiz(options);
```

### Spring Boot bean

Build one `Blaaiz` instance and share it. The class is thread-safe, and it holds an OkHttp
connection pool that many requests can share.

```java
@Configuration
public class BlaaizConfig {

    @Bean
    public Blaaiz blaaiz(
            @Value("${blaaiz.client-id}") String clientId,
            @Value("${blaaiz.client-secret}") String clientSecret,
            @Value("${blaaiz.base-url}") String baseUrl) {

        return new Blaaiz(new BlaaizClientOptions()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .baseUrl(baseUrl));
    }
}
```

## Best Practices

1. Build one `Blaaiz` instance and share it across your application.
2. Validate customer data before you create a customer.
3. Use the fees API to show the fees to your user before they confirm.
4. Verify every webhook signature with the SDK methods.
5. Store the customer IDs and the transaction IDs that you get back.
6. Retry a 429 or a 5xx response with an increasing delay.
7. Read credentials from environment variables.
8. Log the status code and the error code from `BlaaizException`.
9. Pass the raw request body to the webhook methods.
10. Request only the OAuth scopes that your integration uses.

## Examples

The [`examples/`](examples/) directory holds runnable classes for the common workflows.

## Development

```bash
mvn test          # Run the 251 unit tests
mvn package       # Build the jar
mvn javadoc:javadoc  # Generate the Javadoc
```

Read [.github/CONTRIBUTING.md](.github/CONTRIBUTING.md) before you open a pull request.

## Support

- Email: onboarding@blaaiz.com
- Documentation: https://docs.business.blaaiz.com

## License

MIT. Read [LICENSE](LICENSE).
