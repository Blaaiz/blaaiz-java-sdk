package com.blaaiz.sdk;

import java.util.Map;

/** Bank directory, account lookup, and payee/IBAN verification operations. */
public class BankService extends BaseService {

    public BankService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse list() {
        return list(null);
    }

    /**
     * @param filters optional query parameters; recognised keys are {@code currency},
     *                {@code country} (2-letter code), and {@code country_id}. Sent as query
     *                parameters when non-empty.
     */
    public BlaaizResponse list(Map<String, Object> filters) {
        Map<String, Object> params = (filters == null || filters.isEmpty()) ? null : filters;
        return client.makeRequest("GET", "/api/external/bank", params, null);
    }

    public BlaaizResponse lookupAccount(Map<String, Object> lookupData) {
        requireFields(lookupData, "account_number", "bank_id");
        return client.makeRequest("POST", "/api/external/bank/account-lookup", lookupData, null);
    }

    /**
     * Confirmation-of-Payee check for a UK account.
     *
     * @param payeeData must contain {@code sort_code}, {@code account_number}, and
     *                  {@code account_name}.
     */
    public BlaaizResponse verifyPayee(Map<String, Object> payeeData) {
        requireFields(payeeData, "sort_code", "account_number", "account_name");
        return client.makeRequest("POST", "/api/external/bank/payee-verification", payeeData, null);
    }

    /**
     * SEPA reachability check for an IBAN.
     *
     * @param ibanData must contain {@code iban}.
     */
    public BlaaizResponse verifyIban(Map<String, Object> ibanData) {
        requireFields(ibanData, "iban");
        return client.makeRequest("POST", "/api/external/bank/iban-verification", ibanData, null);
    }
}
