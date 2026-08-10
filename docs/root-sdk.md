# Root SDK

The `Blaaiz` class is the entry point. It builds one `BlaaizClient` and one instance of each
service. It also adds the two composite workflows and some short convenience methods.

## `Blaaiz(BlaaizClientOptions options)`

```java
import com.blaaiz.sdk.Blaaiz;
import com.blaaiz.sdk.BlaaizClientOptions;

// OAuth 2.0 client credentials
Blaaiz blaaiz = new Blaaiz(new BlaaizClientOptions()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .baseUrl("https://api-dev.blaaiz.com")
        .timeoutSeconds(30));

// Legacy API key
Blaaiz legacy = new Blaaiz(new BlaaizClientOptions()
        .apiKey("your-api-key")
        .baseUrl("https://api-dev.blaaiz.com"));
```

The constructor throws `BlaaizException` when you supply neither a client ID with a client
secret, nor an API key.

Build one instance and share it across your application. The class is thread-safe, and its
OkHttp connection pool is shared by all requests.

## `BlaaizClientOptions`

| Method                            | Default                       | Description                          |
| --------------------------------- | ----------------------------- | ------------------------------------ |
| `apiKey(String)`                  | none                          | The legacy API key                   |
| `clientId(String)`                | none                          | The OAuth client ID                  |
| `clientSecret(String)`            | none                          | The OAuth client secret              |
| `oauthScope(String)`              | all 21 scopes                 | Space-separated OAuth scopes         |
| `baseUrl(String)`                 | `https://api-dev.blaaiz.com`  | The API base URL                     |
| `timeoutSeconds(int)`             | `30`                          | Connect, read, and write timeout     |

Each method returns the same object, so you can chain the calls.

When you set both OAuth credentials and an API key, the SDK uses OAuth. A credential that is
`null`, empty, or the string `"0"` counts as absent. This rule matches the Node.js and Laravel
SDKs.

## Service accessors

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

## `testConnection()`

Returns `true` when `currencies().list()` succeeds. Catches every exception and returns `false`.
It never throws.

```java
if (!blaaiz.testConnection()) {
    System.err.println("Cannot reach the Blaaiz API.");
}
```

## `createCompletePayout(CompletePayoutConfig config)`

Does three steps in one call:

1. Creates the customer, when `customerData` is set and `payoutData` has no `customer_id`.
2. Gets a fee breakdown.
3. Starts the payout.

```java
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
                "bank_id", "bank-id"));

CompletePayoutResult result = blaaiz.createCompletePayout(config);
```

`CompletePayoutResult` holds:

| Method            | Type     | Description                                        |
| ----------------- | -------- | -------------------------------------------------- |
| `getCustomerId()` | `String` | The existing or the newly created customer ID       |
| `getPayout()`     | `Object` | The `data` payload of the payout response           |
| `getFees()`       | `Object` | The `data` payload of the fee-breakdown response    |

`payoutData` is required. When it is `null`, the method throws `IllegalArgumentException`. Any
failure inside the workflow becomes a `BlaaizException` with the prefix
`Complete payout failed:`.

**Note:** The fee breakdown always sends `from_amount`, never `to_amount`. A `payoutData` that
holds only `to_amount` therefore fails inside `fees().getBreakdown()`. The Node.js, Laravel, and
Python SDKs do the same. To use `to_amount`, call `fees().getBreakdown()` and
`payouts().initiate()` yourself.

## `createCompleteCollection(CompleteCollectionConfig config)`

Does three steps in one call:

1. Creates the customer, when `customerData` is set and `collectionData` has no `customer_id`.
2. Creates a virtual bank account, when `createVba` is `true`.
3. Starts the collection.

```java
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
```

`CompleteCollectionResult` holds:

| Method                 | Type     | Description                                                   |
| ---------------------- | -------- | ------------------------------------------------------------- |
| `getCustomerId()`      | `String` | The existing or the newly created customer ID                  |
| `getCollection()`      | `Object` | The `data` payload of the collection response                  |
| `getVirtualAccount()`  | `Object` | The virtual account payload, or `null` when `createVba` is off |

The account name of the new virtual bank account is `first_name + " " + last_name` from
`customerData`. When `customerData` is `null`, the account name is `Customer Account`.

`collectionData` is required. Any failure inside the workflow becomes a `BlaaizException` with
the prefix `Complete collection failed:`.

## Convenience methods

| Method                                                   | Same as                                |
| -------------------------------------------------------- | -------------------------------------- |
| `getCustomerById(String)`                                 | `customers().get(...)`                 |
| `getTransactionById(String)`                              | `transactions().get(...)`              |
| `getWalletById(String)`                                   | `wallets().get(...)`                   |
| `getAllCurrencies()`                                      | `currencies().list()`                  |
| `getAllBanks()`                                           | `banks().list()`                       |
| `calculateFees(String from, String to, Object amount)`    | `fees().getBreakdown(...)`             |

```java
BlaaizResponse fees = blaaiz.calculateFees("NGN", "CAD", 100000);
```
