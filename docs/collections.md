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
        "customer_id", "customer-id",
        "wallet_id", "wallet-id",
        "amount", 100.00,
        "currency", "EUR",
        "method", "open_banking"));
```

Required:

- `customer_id`
- `wallet_id`
- `amount`
- `currency` — `EUR`, `GBP`, `NGN`, or `USD`
- `method` — `open_banking`, `card`, or `bank_transfer`

Optional:

| Field          | Description                                     |
| -------------- | ----------------------------------------------- |
| `phone_number` | The phone number of the payer                    |
| `email`        | The email address of the payer                   |
| `reference`    | Your own reference for the collection            |
| `narration`    | A description of the payment                     |
| `redirect_url` | Where the payer returns after the payment        |

The SDK forwards the payload without a change, so you can send any other field that the
[Blaaiz API reference](https://docs.business.blaaiz.com) documents.

### Card collection (NGN or USD)

```java
BlaaizResponse collection = blaaiz.collections().initiate(Map.of(
        "customer_id", "customer-id",
        "wallet_id", "wallet-id",
        "amount", 5000,
        "currency", "NGN",
        "method", "card"));
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
```

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
