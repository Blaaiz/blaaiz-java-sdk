package com.blaaiz.sdk;

import java.util.Map;

/** Collection (money-in) operations. */
public class CollectionService extends BaseService {

    public CollectionService(BlaaizClient client) {
        super(client);
    }

    /**
     * @param collectionData must contain {@code customer_id}, {@code wallet_id}, {@code amount},
     *                       {@code currency}, and {@code method}. An optional {@code narration}
     *                       string is accepted and forwarded verbatim like any other field --
     *                       it is unvalidated here (its exact API meaning is unconfirmed across
     *                       source SDKs; only the Node.js README references it).
     */
    public BlaaizResponse initiate(Map<String, Object> collectionData) {
        requireFields(collectionData, "customer_id", "wallet_id", "amount", "currency", "method");
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
        return client.makeRequest("GET", "/api/external/collection/crypto/networks", null, null);
    }

    public BlaaizResponse acceptInteracMoneyRequest(Map<String, Object> interacData) {
        requireFields(interacData, "reference_number");
        return client.makeRequest("POST", "/api/external/collection/accept-interac-money-request", interacData, null);
    }
}
