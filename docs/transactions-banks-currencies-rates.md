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

// Filter by merchant reference
BlaaizResponse byMerchantReference = blaaiz.transactions().list(Map.of(
        "merchant_reference", "invoice-2001"));
```

Supported filters include `start_date`, `end_date`, `wallet_id`, `customer_id`, `type`
(`SEND_MONEY`, `FUND_WALLET`, or `SWAP`), `status` (`FAILED`, `SUCCESSFUL`, or `EXPIRED`), and
`merchant_reference`. Each transaction in the response carries a `merchant_reference` value,
which is `null` when the transaction has none.

### `get(String transactionId)`

`GET /api/external/transaction/{transactionId}`

```java
BlaaizResponse transaction = blaaiz.transactions().get("transaction-id");
```

The `get()` argument accepts a transaction id, a reference, or a merchant reference. The API
resolves the value in that order.

Throws `IllegalArgumentException` when `transactionId` is `null` or empty.

## Banks

Get the service with `blaaiz.banks()`.

### `list()`

`GET /api/external/bank`

Returns the supported banks with their identifiers. Use `bank_id` for a NGN bank transfer
payout.

```java
BlaaizResponse banks = blaaiz.banks().list();

// Filter by currency, country, or country_id
BlaaizResponse ngnBanks = blaaiz.banks().list(Map.of("currency", "NGN"));
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

### `verifyPayee(Map<String, Object> payeeData)`

`POST /api/external/bank/payee-verification`

Runs a Confirmation of Payee check for a UK account. The response holds `matched`,
`match_confidence_code`, and `suggested_account_name`.

```java
BlaaizResponse payee = blaaiz.banks().verifyPayee(Map.of(
        "sort_code", "123456",
        "account_number", "12345678",
        "account_name", "John Doe"));
```

Required:

- `sort_code`
- `account_number`
- `account_name`

### `verifyIban(Map<String, Object> ibanData)`

`POST /api/external/bank/iban-verification`

Checks the SEPA reachability of an IBAN. The response holds `sepa_reachable` and
`sepa_inst_reachable`.

```java
BlaaizResponse iban = blaaiz.banks().verifyIban(Map.of(
        "iban", "DE89370400440532013000"));
```

Required:

- `iban`

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
