# Transactions, Banks, Currencies, and Rates

## Transactions

Get the service with `blaaiz.transactions()`.

### `list(Map<String, Object> filters)`

`POST /api/external/transaction`

**Note:** This search uses `POST`, not `GET`. The SDK sends the filters as a JSON body. The
Node.js, Laravel, and Python SDKs do the same.

Pass `null` to list every transaction. The SDK then sends an empty body.

```java
BlaaizResponse all = blaaiz.transactions().list(null);

BlaaizResponse page = blaaiz.transactions().list(Map.of(
        "page", 1,
        "limit", 10,
        "status", "SUCCESSFUL"));
```

### `get(String transactionId)`

`GET /api/external/transaction/{transactionId}`

```java
BlaaizResponse transaction = blaaiz.transactions().get("transaction-id");
```

Throws `IllegalArgumentException` when `transactionId` is `null` or empty.

## Banks

Get the service with `blaaiz.banks()`.

### `list()`

`GET /api/external/bank`

Returns the supported banks with their identifiers. Use `bank_id` for a NGN bank transfer
payout.

```java
BlaaizResponse banks = blaaiz.banks().list();
```

### `lookupAccount(Map<String, Object> lookupData)`

`POST /api/external/bank/account-lookup`

Returns the name on a bank account. Call it before a payout, and show the name to your user.

```java
BlaaizResponse accountInfo = blaaiz.banks().lookupAccount(Map.of(
        "account_number", "0123456789",
        "bank_id", "1"));
```

Required:

- `account_number`
- `bank_id`

## Currencies

Get the service with `blaaiz.currencies()`.

### `list()`

`GET /api/external/currency`

Returns the supported currencies with their identifiers. `Blaaiz.testConnection()` uses this
call.

```java
BlaaizResponse currencies = blaaiz.currencies().list();
```

## Rates

Get the service with `blaaiz.rates()`.

### `list(String searchTerm)`

`GET /api/external/rate`

Returns the FX rates. Pass `null` to get every rate. Pass a search term to filter the list; the
SDK sends it as the `search_term` query parameter.

```java
BlaaizResponse allRates = blaaiz.rates().list(null);
BlaaizResponse ngnRates = blaaiz.rates().list("NGN");
```

**Note:** Only the Laravel SDK and this SDK have a rate service. The Node.js and Python SDKs
have no equivalent. Check the endpoint against the live API before you depend on it in
production.
