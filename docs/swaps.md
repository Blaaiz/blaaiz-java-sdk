# Swaps

Get the service with `blaaiz.swaps()`. A swap moves money between two business wallets that
hold different currencies.

## `swap(Map<String, Object> data)`

`POST /api/external/swap`

```java
BlaaizResponse swap = blaaiz.swaps().swap(Map.of(
        "from_business_wallet_id", "wallet-id-usd",
        "to_business_wallet_id", "wallet-id-ngn",
        "amount", 100));
```

Required:

- `from_business_wallet_id`
- `to_business_wallet_id`
- `amount`

The SDK throws `IllegalArgumentException` when a required field is missing, and it does this
before it sends the request.

Get the wallet identifiers from `blaaiz.wallets().list()`. Check the current rate with
`blaaiz.rates().list()` before you swap.

**Note:** Only the Laravel SDK and this SDK have a swap service. The Node.js and Python SDKs
have no equivalent. Check the endpoint against the live API before you depend on it in
production.
