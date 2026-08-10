# Payouts

Get the service with `blaaiz.payouts()`.

## `initiate(Map<String, Object> payoutData)`

`POST /api/external/payout`

The SDK validates the payload before it sends the request. The required fields depend on
`method` and on `to_currency_id`.

### Fields that every method needs

- `wallet_id`
- `customer_id`
- `method` — `bank_transfer`, `interac`, `ach`, `wire`, or `crypto`
- `from_currency_id`
- `to_currency_id`
- `from_amount` or `to_amount` — give one of the two

`from_amount` fixes the amount that leaves your wallet. `to_amount` fixes the amount that the
recipient gets.

### Optional fields

| Field          | Description                                                            |
| -------------- | ---------------------------------------------------------------------- |
| `note`         | The transaction description. It defaults to your business name.         |
| `phone_number` | The phone number of the recipient                                       |

`initiate()` forwards the whole payload without a change. You can send any field that the
[Blaaiz API reference](https://docs.business.blaaiz.com) documents, even a field that this page
does not list. The SDK therefore works with a new API field with no SDK update.

## Bank transfer

`method` is `bank_transfer`. The extra required fields depend on `to_currency_id`.

### NGN

Required: `bank_id`, `account_number`

```java
BlaaizResponse payout = blaaiz.payouts().initiate(Map.of(
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "method", "bank_transfer",
        "from_amount", 1000,
        "from_currency_id", "NGN",
        "to_currency_id", "NGN",
        "bank_id", "bank-id",
        "account_number", "0123456789"));
```

Get `bank_id` from `blaaiz.banks().list()`. Check the account name first with
`blaaiz.banks().lookupAccount()`.

### GBP

Required: `sort_code`, `account_number`, `account_name`

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
        "account_name", "John Doe"));
```

### EUR

Required: `iban`, `bic_code`, `account_name`

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
        "account_name", "John Doe"));
```

## Interac (CAD)

`method` is `interac`. Required: `email`, `interac_first_name`, `interac_last_name`

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
        "interac_last_name", "Doe"));
```

## ACH (USD)

`method` is `ach`. Required: `type`, `account_number`, `account_name`, `account_type`,
`bank_name`, `routing_number`

`Map.of` holds at most 10 pairs. Build this payload with a `LinkedHashMap`.

```java
Map<String, Object> achPayout = new LinkedHashMap<>();
achPayout.put("wallet_id", "wallet-id");
achPayout.put("customer_id", "customer-id");
achPayout.put("method", "ach");
achPayout.put("from_amount", 100);
achPayout.put("from_currency_id", "USD");
achPayout.put("to_currency_id", "USD");
achPayout.put("type", "individual");        // individual or business
achPayout.put("account_number", "123456789");
achPayout.put("account_name", "John Doe");
achPayout.put("account_type", "checking");  // checking or savings
achPayout.put("bank_name", "Chase Bank");
achPayout.put("routing_number", "021000021");

BlaaizResponse response = blaaiz.payouts().initiate(achPayout);
```

## Wire (USD)

`method` is `wire`. Required: the ACH fields, and `swift_code`.

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

## Crypto

`method` is `crypto`. Required: `wallet_address`, `wallet_token`, `wallet_network`

```java
BlaaizResponse cryptoPayout = blaaiz.payouts().initiate(Map.of(
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "method", "crypto",
        "from_amount", 100,
        "from_currency_id", "USD",
        "to_currency_id", "USDT",
        "wallet_address", "0x1234567890abcdef1234567890abcdef12345678",
        "wallet_token", "USDT",
        "wallet_network", "ETHEREUM_MAINNET"));
```

| Field            | Values                                                                 |
| ---------------- | ---------------------------------------------------------------------- |
| `wallet_token`   | `USDT`, `USDC`                                                          |
| `wallet_network` | `BSC_MAINNET`, `ETHEREUM_MAINNET`, `TRON_MAINNET`, `MATIC_MAINNET`      |

**Warning:** A crypto payout cannot be reversed. Check `wallet_address` and `wallet_network`
before you send the request.

## Use `to_amount`

To fix the amount that the recipient gets, send `to_amount` in place of `from_amount`:

```java
BlaaizResponse payout = blaaiz.payouts().initiate(Map.of(
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "method", "bank_transfer",
        "to_amount", 50000,
        "from_currency_id", "USD",
        "to_currency_id", "NGN",
        "bank_id", "bank-id",
        "account_number", "0123456789"));
```

**Note:** `Blaaiz.createCompletePayout()` always sends `from_amount` to the fee-breakdown call.
To use `to_amount`, call `payouts().initiate()` directly, as shown above.

## Validation errors

The SDK throws `IllegalArgumentException` before it sends the request when a required field is
missing. A field counts as missing when its value is `null`, an empty string, the number zero,
or `false`. This rule matches the Node.js, Laravel, and Python SDKs.

```java
try {
    blaaiz.payouts().initiate(payoutData);
} catch (IllegalArgumentException e) {
    // For example: "bank_id is required"
} catch (BlaaizException e) {
    // The API rejected the payout, or the request failed.
}
```

## Check the fees first

Show the fees to your user before they confirm the payout:

```java
BlaaizResponse fees = blaaiz.fees().getBreakdown(Map.of(
        "from_currency_id", "NGN",
        "to_currency_id", "CAD",
        "from_amount", 100000));
```

Read [Fees and files](fees-files.md) for more.
