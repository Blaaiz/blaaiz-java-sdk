# Swaps

Get the service with `blaaiz.swaps()`. A swap moves money between two business wallets that
hold different currencies.

## `initiate(Map<String, Object> data)`

`POST /api/external/swap`

```java
BlaaizResponse swap = blaaiz.swaps().initiate(Map.of(
        "from_business_wallet_id", "wallet-id-usd",
        "to_business_wallet_id", "wallet-id-ngn",
        "amount", 100));
```

Required:

- `from_business_wallet_id`
- `to_business_wallet_id`
- `amount`

Optional:

- `amount_type` (`from` or `to`, default `from`)

The SDK throws `IllegalArgumentException` when a required field is missing, and it does this
before it sends the request.

Get the wallet identifiers from `blaaiz.wallets().list()`. Check the current rate with
`blaaiz.rates().list()` before you swap.
