package com.blaaiz.sdk;

import java.util.Map;

/**
 * Business-wallet-to-business-wallet currency swaps ({@code POST /api/external/swap}).
 *
 * <p>Present only in the Laravel source SDK -- see {@link RateService} for the same caveat:
 * Node.js and Python expose no equivalent, so confirm this endpoint is still live before
 * shipping.
 */
public class SwapService extends BaseService {

    public SwapService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse swap(Map<String, Object> data) {
        requireFields(data, "from_business_wallet_id", "to_business_wallet_id", "amount");
        return client.makeRequest("POST", "/api/external/swap", data, null);
    }
}
