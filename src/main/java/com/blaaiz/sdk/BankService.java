package com.blaaiz.sdk;

import java.util.Map;

/** Bank directory and account-lookup operations. */
public class BankService extends BaseService {

    public BankService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse list() {
        return client.makeRequest("GET", "/api/external/bank", null, null);
    }

    public BlaaizResponse lookupAccount(Map<String, Object> lookupData) {
        requireFields(lookupData, "account_number", "bank_id");
        return client.makeRequest("POST", "/api/external/bank/account-lookup", lookupData, null);
    }
}
