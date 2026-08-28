package com.blaaiz.sdk;

import java.util.Map;

/** Business-wallet-to-business-wallet currency swaps ({@code POST /api/external/swap}). */
public class SwapService extends BaseService {

    public SwapService(BlaaizClient client) {
        super(client);
    }

    /**
     * @param swapData must contain {@code from_business_wallet_id}, {@code to_business_wallet_id},
     *                 and {@code amount}. Optional {@code amount_type} ({@code from} or {@code to},
     *                 default {@code from}) is forwarded verbatim.
     */
    public BlaaizResponse initiate(Map<String, Object> swapData) {
        requireFields(swapData, "from_business_wallet_id", "to_business_wallet_id", "amount");
        return client.makeRequest("POST", "/api/external/swap", swapData, null);
    }

    /**
     * @deprecated use {@link #initiate(Map)} instead. Kept for backward compatibility;
     *             it will be removed in a future major version.
     */
    @Deprecated
    public BlaaizResponse swap(Map<String, Object> swapData) {
        return initiate(swapData);
    }
}
