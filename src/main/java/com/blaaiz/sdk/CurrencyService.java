package com.blaaiz.sdk;

/** Read-only access to the list of currencies supported by Blaaiz. */
public class CurrencyService extends BaseService {

    public CurrencyService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse list() {
        return client.makeRequest("GET", "/api/external/currency", null, null);
    }
}
