# Examples

Runnable examples for the Blaaiz Java SDK. Each class has a `main` method, or static methods
that you can copy into your own application.

These examples are a separate Maven module. They are compiled against the real SDK API, so they
cannot drift out of date without the build failing.

## What is included

| File                            | What it shows                                                        |
| ------------------------------- | -------------------------------------------------------------------- |
| `ExampleClient.java`            | Builds the shared `Blaaiz` instance from environment variables         |
| `CheckRatesExample.java`        | Tests the connection, then reads the currencies and the FX rates      |
| `CreateCustomerExample.java`    | Creates, reads, updates, and lists customers                          |
| `InitiatePayoutExample.java`    | Fee breakdown, and a payload for every payout method                  |
| `UploadKycExample.java`         | Uploads a KYC document from a file, base64, a data URL, or a URL      |
| `CompleteWorkflowExample.java`  | The `createCompletePayout` and `createCompleteCollection` workflows   |
| `WebhookVerificationExample.java` | Registers webhooks, then verifies an incoming webhook               |

## Setup

1. Install the SDK into your local Maven repository, from the repository root:

   ```bash
   mvn -DskipTests install
   ```

2. Set the credentials. Use OAuth:

   ```bash
   export BLAAIZ_CLIENT_ID=your-client-id
   export BLAAIZ_CLIENT_SECRET=your-client-secret
   ```

   Or use the legacy API key:

   ```bash
   export BLAAIZ_API_KEY=your-api-key
   ```

3. Set the environment. It defaults to the development API.

   ```bash
   export BLAAIZ_API_URL=https://api-dev.blaaiz.com
   ```

4. Set the values that some examples need:

   ```bash
   export BLAAIZ_TEST_WALLET_ID=your-wallet-id
   export BLAAIZ_TEST_CUSTOMER_ID=your-customer-id
   export BLAAIZ_TEST_BANK_ID=your-bank-id
   export BLAAIZ_WEBHOOK_SECRET=your-webhook-secret
   ```

## Compile

```bash
mvn -f examples/pom.xml compile
```

## Run

```bash
mvn -f examples/pom.xml exec:java \
  -Dexec.mainClass=com.blaaiz.sdk.examples.CheckRatesExample
```

**Warning:** `InitiatePayoutExample` and `CompleteWorkflowExample` move real money. Run them
against the development environment only.

## Notes

- Build one `Blaaiz` instance and share it. The class is thread-safe.
- Pass the raw request body to the webhook methods. A body that you parsed and serialized again
  has different bytes, and the signature check then fails.
- `Map.of` holds at most 10 pairs. Build the larger payloads with a `LinkedHashMap`.
