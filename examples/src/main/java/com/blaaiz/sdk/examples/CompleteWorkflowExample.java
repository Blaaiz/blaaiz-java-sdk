package com.blaaiz.sdk.examples;

import com.blaaiz.sdk.Blaaiz;
import com.blaaiz.sdk.BlaaizException;
import com.blaaiz.sdk.CompleteCollectionConfig;
import com.blaaiz.sdk.CompleteCollectionResult;
import com.blaaiz.sdk.CompletePayoutConfig;
import com.blaaiz.sdk.CompletePayoutResult;

import java.util.Map;

/**
 * Runs the two composite workflows on the {@link Blaaiz} facade.
 *
 * <p>{@code createCompletePayout} creates the customer, gets a fee breakdown, and starts the
 * payout. {@code createCompleteCollection} creates the customer, optionally creates a virtual
 * bank account, and starts the collection.
 */
public final class CompleteWorkflowExample {

    private CompleteWorkflowExample() {
    }

    public static void main(String[] args) {
        Blaaiz blaaiz = ExampleClient.create();
        String walletId = ExampleClient.requireEnv("BLAAIZ_TEST_WALLET_ID");
        String bankId = ExampleClient.requireEnv("BLAAIZ_TEST_BANK_ID");

        completePayout(blaaiz, walletId, bankId);
        completeCollection(blaaiz, walletId);
    }

    /**
     * Creates a customer and pays out to a Nigerian bank account in one call.
     *
     * <p>Note: the fee breakdown inside this workflow always sends {@code from_amount}. A
     * {@code payoutData} that holds only {@code to_amount} fails. This behavior is the same in
     * the Node.js, Laravel, and Python SDKs. To use {@code to_amount}, call
     * {@code fees().getBreakdown()} and {@code payouts().initiate()} yourself.
     */
    static void completePayout(Blaaiz blaaiz, String walletId, String bankId) {
        CompletePayoutConfig config = new CompletePayoutConfig()
                .customerData(Map.of(
                        "first_name", "John",
                        "last_name", "Doe",
                        "type", "individual",
                        "email", "john@example.com",
                        "country", "NG",
                        "id_type", "passport",
                        "id_number", "A12345678"))
                .payoutData(Map.of(
                        "wallet_id", walletId,
                        "method", "bank_transfer",
                        "from_amount", 1000,
                        "from_currency_id", "NGN",
                        "to_currency_id", "NGN",
                        "account_number", "0123456789",
                        "bank_id", bankId));

        try {
            CompletePayoutResult result = blaaiz.createCompletePayout(config);
            System.out.println("Customer ID: " + result.getCustomerId());
            System.out.println("Payout: " + result.getPayout());
            System.out.println("Fees: " + result.getFees());
        } catch (BlaaizException e) {
            System.err.println("Complete payout failed: " + e.getMessage());
        }
    }

    /** Creates a customer, creates a virtual bank account, and starts a card collection. */
    static void completeCollection(Blaaiz blaaiz, String walletId) {
        CompleteCollectionConfig config = new CompleteCollectionConfig()
                .customerData(Map.of(
                        "first_name", "Jane",
                        "last_name", "Smith",
                        "type", "individual",
                        "email", "jane@example.com",
                        "country", "NG",
                        "id_type", "drivers_license",
                        "id_number", "ABC123456"))
                .collectionData(Map.of(
                        "method", "card",
                        "amount", 5000,
                        "currency", "NGN",
                        "wallet_id", walletId))
                .createVba(true);

        try {
            CompleteCollectionResult result = blaaiz.createCompleteCollection(config);
            System.out.println("Customer ID: " + result.getCustomerId());
            System.out.println("Collection: " + result.getCollection());
            System.out.println("Virtual account: " + result.getVirtualAccount());
        } catch (BlaaizException e) {
            System.err.println("Complete collection failed: " + e.getMessage());
        }
    }
}
