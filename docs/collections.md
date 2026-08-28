# Collections

Get the service with `blaaiz.collections()`.

| Currency | Method                                    |
| -------- | ----------------------------------------- |
| CAD      | Interac (push mechanism)                   |
| NGN      | Bank transfer (virtual bank account), card |
| USD      | Card                                       |
| EUR, GBP | Open Banking                               |

## `initiate(Map<String, Object> collectionData)`

`POST /api/external/collection`

```java
BlaaizResponse collection = blaaiz.collections().initiate(Map.of(
        "method", "open_banking",
        "amount", 100.00,
        "wallet_id", "wallet-id"));
```

Required:

- `method` — `open_banking` or `card`
- `amount`
- `wallet_id`

Required for `method` `card`:

- `customer_id`
- `card_holder_name`
- `card_number` — the 16 digits of the card
- `expiry` — the expiry date in `MM/YY` format
- `cvc` — the 3-digit card security code

Optional:

| Field                | Description                                                        |
| -------------------- | ------------------------------------------------------------------ |
| `phone`              | The phone number of the payer                                       |
| `redirect_url`       | Where the payer returns after the payment. Must be `https`.          |
| `merchant_reference` | Your own reference. Maximum 255 characters. Unique for each business. |

The SDK forwards the payload without a change, so you can send any other field that the
[Blaaiz API reference](https://docs.business.blaaiz.com) documents.

For `merchant_reference`, see [Merchant reference](../README.md#merchant-reference).

### Card collection (NGN or USD)

```java
BlaaizResponse collection = blaaiz.collections().initiate(Map.of(
        "method", "card",
        "amount", 5000,
        "wallet_id", "wallet-id",
        "customer_id", "customer-id",
        "card_holder_name", "John Doe",
        "card_number", "4111111111111111",
        "expiry", "12/30",
        "cvc", "123"));
```

The response holds the payment URL. Send the payer to that URL.

## `initiateCrypto(Map<String, Object> cryptoData)`

`POST /api/external/collection/crypto`

```java
BlaaizResponse cryptoCollection = blaaiz.collections().initiateCrypto(Map.of(
        "amount", 100,
        "network", "ethereum",
        "token", "USDT",
        "wallet_id", "wallet-id"));
```

The SDK does no local validation here. It forwards `cryptoData` without a change.

## `getCryptoNetworks()`

`GET /api/external/collection/crypto/networks`

Returns the networks that crypto collections support. Call it before `initiateCrypto`.

```java
BlaaizResponse networks = blaaiz.collections().getCryptoNetworks();

// Filter by transaction type: collection or payout
BlaaizResponse payoutNetworks = blaaiz.collections().getCryptoNetworks(Map.of(
        "transaction_type", "payout"));
```

## `initiateInteracMoneyRequest(Map<String, Object> interacData)`

`POST /api/external/collection/interac-money-request`

Sends an Interac money request to a payer by email.

```java
BlaaizResponse request = blaaiz.collections().initiateInteracMoneyRequest(Map.of(
        "amount", 100,
        "email", "payer@example.com",
        "customer_name", "John Doe",
        "expiry_hours", 24,
        "note", "Invoice 2001"));
```

Required:

- `amount`
- `email`

Optional:

- `customer_name`
- `customer_id`
- `expiry_hours` — a value from 1 to 120
- `note`

## `acceptInteracMoneyRequest(Map<String, Object> interacData)`

`POST /api/external/collection/accept-interac-money-request`

```java
// Standard transfer: give the security answer
BlaaizResponse interac = blaaiz.collections().acceptInteracMoneyRequest(Map.of(
        "reference_number", "interac-reference",
        "security_answer", "answer",
        "email", "sender@example.com"));

// Auto deposit: no security answer
BlaaizResponse autoDeposit = blaaiz.collections().acceptInteracMoneyRequest(Map.of(
        "reference_number", "interac-reference"));
```

Required:

- `reference_number`

Optional:

- `security_answer` — needed for a standard transfer, not for an auto deposit
- `email` — the email address of the sender

## `attachCustomer(Map<String, Object> attachData)`

`POST /api/external/collection/attach-customer`

Links a customer to a collection that started without one.

```java
BlaaizResponse attachment = blaaiz.collections().attachCustomer(Map.of(
        "customer_id", "customer-id",
        "transaction_id", "transaction-id"));
```

Required:

- `customer_id`
- `transaction_id`
