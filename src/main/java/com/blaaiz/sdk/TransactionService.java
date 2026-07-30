package com.blaaiz.sdk;

import java.util.Collections;
import java.util.Map;

/** Transaction search and lookup. */
public class TransactionService extends BaseService {

    public TransactionService(BlaaizClient client) {
        super(client);
    }

    /**
     * Searches transactions. Despite being a search/list operation this is a {@code POST}
     * (the filters are sent as a JSON body), matching all three source SDKs.
     */
    public BlaaizResponse list(Map<String, Object> filters) {
        return client.makeRequest("POST", "/api/external/transaction",
                filters != null ? filters : Collections.emptyMap(), null);
    }

    public BlaaizResponse get(String transactionId) {
        requireNonBlank(transactionId, "Transaction ID is required");
        return client.makeRequest("GET", "/api/external/transaction/" + transactionId, null, null);
    }
}
