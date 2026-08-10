package com.blaaiz.sdk.examples;

import com.blaaiz.sdk.Blaaiz;
import com.blaaiz.sdk.BlaaizException;
import com.blaaiz.sdk.BlaaizResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shows a fee breakdown, then starts a payout for each supported method.
 *
 * <p>Only the NGN bank transfer runs. The other methods are shown as payload builders, so you
 * can copy the one you need.
 */
public final class InitiatePayoutExample {

    private InitiatePayoutExample() {
    }

    public static void main(String[] args) {
        Blaaiz blaaiz = ExampleClient.create();

        String walletId = ExampleClient.requireEnv("BLAAIZ_TEST_WALLET_ID");
        String customerId = ExampleClient.requireEnv("BLAAIZ_TEST_CUSTOMER_ID");
        String bankId = ExampleClient.requireEnv("BLAAIZ_TEST_BANK_ID");

        try {
            // Show the fees before you send the payout.
            BlaaizResponse fees = blaaiz.fees().getBreakdown(Map.of(
                    "from_currency_id", "NGN",
                    "to_currency_id", "NGN",
                    "from_amount", 1000));
            System.out.println("Fee breakdown: " + fees.getData());

            BlaaizResponse payout = blaaiz.payouts().initiate(Map.of(
                    "wallet_id", walletId,
                    "customer_id", customerId,
                    "method", "bank_transfer",
                    "from_amount", 1000,
                    "from_currency_id", "NGN",
                    "to_currency_id", "NGN",
                    "bank_id", bankId,
                    "account_number", "0123456789"));
            System.out.println("Payout: " + payout.getData());
        } catch (BlaaizException e) {
            System.err.println("Payout failed: " + e.getMessage());
            System.err.println("Status: " + e.getStatus() + " code: " + e.getErrorCode());
        }
    }

    /** GBP bank transfer: sort code and account number. */
    static Map<String, Object> gbpBankTransfer(String walletId, String customerId) {
        return Map.of(
                "wallet_id", walletId,
                "customer_id", customerId,
                "method", "bank_transfer",
                "from_amount", 100,
                "from_currency_id", "GBP",
                "to_currency_id", "GBP",
                "sort_code", "123456",
                "account_number", "12345678",
                "account_name", "John Doe");
    }

    /** EUR bank transfer: IBAN and BIC. */
    static Map<String, Object> eurBankTransfer(String walletId, String customerId) {
        return Map.of(
                "wallet_id", walletId,
                "customer_id", customerId,
                "method", "bank_transfer",
                "from_amount", 100,
                "from_currency_id", "EUR",
                "to_currency_id", "EUR",
                "iban", "DE89370400440532013000",
                "bic_code", "COBADEFFXXX",
                "account_name", "John Doe");
    }

    /** CAD Interac payout: the recipient gets an email. */
    static Map<String, Object> interac(String walletId, String customerId) {
        return Map.of(
                "wallet_id", walletId,
                "customer_id", customerId,
                "method", "interac",
                "from_amount", 100,
                "from_currency_id", "CAD",
                "to_currency_id", "CAD",
                "email", "recipient@example.com",
                "interac_first_name", "John",
                "interac_last_name", "Doe");
    }

    /** USD ACH payout. Map.of holds at most 10 pairs, so build this one with a LinkedHashMap. */
    static Map<String, Object> ach(String walletId, String customerId) {
        Map<String, Object> payout = new LinkedHashMap<>();
        payout.put("wallet_id", walletId);
        payout.put("customer_id", customerId);
        payout.put("method", "ach");
        payout.put("from_amount", 100);
        payout.put("from_currency_id", "USD");
        payout.put("to_currency_id", "USD");
        payout.put("type", "individual");
        payout.put("account_number", "123456789");
        payout.put("account_name", "John Doe");
        payout.put("account_type", "checking");
        payout.put("bank_name", "Chase Bank");
        payout.put("routing_number", "021000021");
        return payout;
    }

    /** USD wire payout: the same fields as ACH, plus a SWIFT code. */
    static Map<String, Object> wire(String walletId, String customerId) {
        Map<String, Object> payout = new LinkedHashMap<>(ach(walletId, customerId));
        payout.put("method", "wire");
        payout.put("from_amount", 1000);
        payout.put("swift_code", "CHASUS33");
        return payout;
    }

    /** Crypto payout on one of the supported networks. */
    static Map<String, Object> crypto(String walletId, String customerId) {
        return Map.of(
                "wallet_id", walletId,
                "customer_id", customerId,
                "method", "crypto",
                "from_amount", 100,
                "from_currency_id", "USD",
                "to_currency_id", "USDT",
                "wallet_address", "0x1234567890abcdef1234567890abcdef12345678",
                "wallet_token", "USDT",
                "wallet_network", "ETHEREUM_MAINNET");
    }
}
