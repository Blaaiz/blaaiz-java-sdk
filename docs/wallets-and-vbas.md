# Wallets and Virtual Bank Accounts

## Wallets

Get the service with `blaaiz.wallets()`.

### `list()`

`GET /api/external/wallet`

Returns every wallet of your business, with the balance of each one.

```java
BlaaizResponse wallets = blaaiz.wallets().list();
```

### `get(String walletId)`

`GET /api/external/wallet/{walletId}`

```java
BlaaizResponse wallet = blaaiz.wallets().get("wallet-id");
```

Throws `IllegalArgumentException` when `walletId` is `null` or empty.

## Virtual Bank Accounts

Get the service with `blaaiz.virtualBankAccounts()`. A virtual bank account gives a customer a
NGN account number. Money that arrives at that account number goes into your wallet.

### `create(Map<String, Object> vbaData)`

`POST /api/external/virtual-bank-account`

```java
BlaaizResponse vba = blaaiz.virtualBankAccounts().create(Map.of(
        "wallet_id", "wallet-id",
        "account_name", "John Doe"));
```

Required:

- `wallet_id`

Optional:

- `account_name` — the name that the payer sees
- `customer_id` — link the account to a customer

The response holds `account_number` and `bank_name`.

### `list(String walletId, String customerId)`

`GET /api/external/virtual-bank-account`

Pass `null` for a filter that you do not need. The SDK sends only the filters that are not
`null`.

```java
BlaaizResponse all = blaaiz.virtualBankAccounts().list(null, null);
BlaaizResponse byWallet = blaaiz.virtualBankAccounts().list("wallet-id", null);
BlaaizResponse byCustomer = blaaiz.virtualBankAccounts().list(null, "customer-id");
BlaaizResponse byBoth = blaaiz.virtualBankAccounts().list("wallet-id", "customer-id");
```

### `get(String vbaId)`

`GET /api/external/virtual-bank-account/{vbaId}`

```java
BlaaizResponse account = blaaiz.virtualBankAccounts().get("vba-id");
```

### `close(String vbaId, String reason)`

`POST /api/external/virtual-bank-account/{vbaId}/close`

```java
// Without a reason
BlaaizResponse closed = blaaiz.virtualBankAccounts().close("vba-id", null);

// With a reason
BlaaizResponse closedWithReason =
        blaaiz.virtualBankAccounts().close("vba-id", "No longer needed");
```

The SDK sends `reason` only when it is not `null`. An empty string is still sent.

**Warning:** A closed virtual bank account cannot accept money again. Money that a payer sends
to a closed account number is returned to the payer.

### `getIdentificationType(String customerId, String country, String type)`

`GET /api/external/virtual-bank-account/identification-type`

Returns the identification document that a country and a customer type need. Use it to show the
correct field in your own form.

```java
// For a customer that exists
BlaaizResponse idType = blaaiz.virtualBankAccounts()
        .getIdentificationType("customer-id", null, null);

// For a new customer
BlaaizResponse idTypeNew = blaaiz.virtualBankAccounts()
        .getIdentificationType(null, "NG", "individual");
```

Give `customerId`, or give both `country` and `type`. When you give `customerId`, the SDK
ignores `country` and `type`. When you give none of the three, the SDK throws
`IllegalArgumentException` with the message
`Either customer_id or both country and type are required`.

The response holds `label` and `type`. For example, `label` is `Bank Verification Number` and
`type` is `bvn`.
