package com.blaaiz.sdk;

import java.util.Map;

/** Mobile money operator directory. */
public class MomoOperatorService extends BaseService {

    public MomoOperatorService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse list() {
        return list(null);
    }

    /**
     * @param filters optional query parameters; recognised keys are {@code currency_id} (the
     *                destination currency ID) and {@code country_id}. Sent as query parameters
     *                when non-empty.
     */
    public BlaaizResponse list(Map<String, Object> filters) {
        Map<String, Object> params = (filters == null || filters.isEmpty()) ? null : filters;
        return client.makeRequest("GET", "/api/external/momo-operator", params, null);
    }
}
