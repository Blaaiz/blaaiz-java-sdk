package com.blaaiz.sdk;

import java.util.Map;

/** Payout (money-out) operations. */
public class PayoutService extends BaseService {

    public PayoutService(BlaaizClient client) {
        super(client);
    }

    /**
     * @param payoutData must contain {@code wallet_id}, {@code customer_id}, {@code method},
     *                   {@code from_currency_id}, {@code to_currency_id}, and either
     *                   {@code from_amount} or {@code to_amount}. Additional fields are required
     *                   depending on {@code method}: {@code bank_transfer} needs currency-specific
     *                   fields for {@code NGN}/{@code GBP}/{@code EUR}; {@code interac} needs
     *                   {@code email}/{@code interac_first_name}/{@code interac_last_name};
     *                   {@code ach}/{@code wire} need standard bank-account fields plus
     *                   {@code swift_code} for {@code wire}; {@code crypto} needs
     *                   {@code wallet_address}/{@code wallet_token}/{@code wallet_network}. An
     *                   optional {@code note} string is forwarded verbatim (populates the
     *                   transaction description; defaults to the business name when omitted).
     */
    public BlaaizResponse initiate(Map<String, Object> payoutData) {
        requireFields(payoutData, "wallet_id", "customer_id", "method", "from_currency_id", "to_currency_id");
        if (isBlank(payoutData.get("from_amount")) && isBlank(payoutData.get("to_amount"))) {
            throw new IllegalArgumentException("Either from_amount or to_amount is required");
        }

        Object methodObj = payoutData.get("method");
        String method = methodObj != null ? String.valueOf(methodObj) : null;
        Object toCurrencyObj = payoutData.get("to_currency_id");
        String toCurrency = toCurrencyObj != null ? String.valueOf(toCurrencyObj) : null;

        if ("bank_transfer".equals(method)) {
            validateBankTransferFields(payoutData, toCurrency);
        } else if ("interac".equals(method)) {
            requireFields(payoutData, "email", "interac_first_name", "interac_last_name");
        } else if ("ach".equals(method) || "wire".equals(method)) {
            validateAchWireFields(payoutData, method);
        } else if ("crypto".equals(method)) {
            requireFields(payoutData, "wallet_address", "wallet_token", "wallet_network");
        }

        return client.makeRequest("POST", "/api/external/payout", payoutData, null);
    }

    private static void validateBankTransferFields(Map<String, Object> payoutData, String toCurrency) {
        if ("NGN".equals(toCurrency)) {
            requireFields(payoutData, "bank_id", "account_number");
        } else if ("GBP".equals(toCurrency)) {
            requireFields(payoutData, "sort_code", "account_number", "account_name");
        } else if ("EUR".equals(toCurrency)) {
            requireFields(payoutData, "iban", "bic_code", "account_name");
        }
    }

    private static void validateAchWireFields(Map<String, Object> payoutData, String method) {
        requireFields(payoutData, "type", "account_number", "account_name", "account_type", "bank_name", "routing_number");
        if ("wire".equals(method)) {
            requireFields(payoutData, "swift_code");
        }
    }
}
