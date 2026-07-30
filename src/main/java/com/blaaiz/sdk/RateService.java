package com.blaaiz.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FX rate lookups ({@code GET /api/external/rate}).
 *
 * <p>Present only in the Laravel source SDK -- Node.js and Python expose neither this service
 * nor an equivalent method. Included here for completeness/parity with Laravel's real endpoint;
 * verify against the live API/OpenAPI spec before relying on it in production, since the other
 * two SDKs' silence could mean this is Laravel-specific, deprecated, or simply un-ported there.
 */
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
