package com.blaaiz.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/** FX rate lookups ({@code GET /api/external/rate}). */
public class RateService extends BaseService {

    public RateService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse list(String searchTerm) {
        Map<String, Object> params = null;
        if (searchTerm != null) {
            params = new LinkedHashMap<>();
            params.put("search_term", searchTerm);
        }
        return client.makeRequest("GET", "/api/external/rate", params, null);
    }
}
