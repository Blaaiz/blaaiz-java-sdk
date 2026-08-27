# Refunds

Get the service with `blaaiz.refunds()`. A refund returns money for a transaction.

## `initiate(Map<String, Object> data)`

`POST /api/external/refund`

```java
BlaaizResponse refund = blaaiz.refunds().initiate(Map.of(
        "transaction_id", "transaction-id",
        "reason", "Customer request",
        "reference", "refund-2001"));
```

Required:

- `transaction_id`

Optional:

- `reason` (maximum 250 characters)
- `reference` (maximum 100 characters)

The SDK throws `IllegalArgumentException` when `transaction_id` is missing, and it does this
before it sends the request.

The response data holds the refund. The refund fields are `id`, `status`, `type`, `amount`,
`currency`, `transaction_id`, `reference`, `business_customer_id`, `refund_reference`,
`failure_reason`, `created_at`, and `updated_at`.

## `get(String refundId)`

`GET /api/external/refund/{id}`

```java
BlaaizResponse refund = blaaiz.refunds().get("refund-id");
```

The SDK throws `IllegalArgumentException` when `refundId` is null or empty. The API returns
HTTP 404 when the refund does not exist.
