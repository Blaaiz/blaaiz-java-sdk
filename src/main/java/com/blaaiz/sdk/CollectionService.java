package com.blaaiz.sdk;

import java.util.Map;

/** Collection (money-in) operations. */
public class CollectionService extends BaseService {

    public CollectionService(BlaaizClient client) {
        super(client);
    }

    /**
     * Initiates a collection.
     *
     * @param collectionData must contain {@code method} ({@code open_banking} or {@code card}),
     *                       {@code amount}, and {@code wallet_id}. When {@code method} is
     *                       {@code card}, {@code customer_id}, {@code card_holder_name},
     *                       {@code card_number}, {@code expiry}, and {@code cvc} are also
     *                       required. Optional {@code merchant_reference} (max 255, unique per
     *                       business) is forwarded verbatim and echoed on the resulting
     *                       transaction; a duplicate value for the same business is rejected by
     *                       the API with HTTP 422.
     */
    public BlaaizResponse initiate(Map<String, Object> collectionData) {
        requireFields(collectionData, "method", "amount", "wallet_id");

        if ("card".equals(collectionData.get("method"))) {
            requireFields(collectionData, "customer_id", "card_holder_name", "card_number", "expiry", "cvc");
        }

        return client.makeRequest("POST", "/api/external/collection", collectionData, null);
    }

    public BlaaizResponse initiateCrypto(Map<String, Object> cryptoData) {
        return client.makeRequest("POST", "/api/external/collection/crypto", cryptoData, null);
    }

    public BlaaizResponse attachCustomer(Map<String, Object> attachData) {
        requireFields(attachData, "customer_id", "transaction_id");
        return client.makeRequest("POST", "/api/external/collection/attach-customer", attachData, null);
    }

    public BlaaizResponse getCryptoNetworks() {
        return getCryptoNetworks(null);
    }

    /**
     * @param filters optional query parameters; {@code transaction_type} ({@code collection} or
     *                {@code payout}) is the only recognised key. Sent as query parameters when
     *                non-empty.
     */
    public BlaaizResponse getCryptoNetworks(Map<String, Object> filters) {
        Map<String, Object> params = (filters == null || filters.isEmpty()) ? null : filters;
        return client.makeRequest("GET", "/api/external/collection/crypto/networks", params, null);
    }

    public BlaaizResponse initiateInteracMoneyRequest(Map<String, Object> interacData) {
        requireFields(interacData, "amount", "email");
        return client.makeRequest("POST", "/api/external/collection/interac-money-request", interacData, null);
    }

    public BlaaizResponse acceptInteracMoneyRequest(Map<String, Object> interacData) {
        requireFields(interacData, "reference_number");
        return client.makeRequest("POST", "/api/external/collection/accept-interac-money-request", interacData, null);
    }
}
