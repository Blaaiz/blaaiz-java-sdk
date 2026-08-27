package com.blaaiz.sdk;

import java.util.Map;

/** Refund creation and lookup ({@code /api/external/refund}). */
public class RefundService extends BaseService {

    public RefundService(BlaaizClient client) {
        super(client);
    }

    /**
     * @param refundData must contain {@code transaction_id}. Optional {@code reason} (max 250)
     *                   and {@code reference} (max 100) are forwarded verbatim.
     */
    public BlaaizResponse initiate(Map<String, Object> refundData) {
        requireFields(refundData, "transaction_id");
        return client.makeRequest("POST", "/api/external/refund", refundData, null);
    }

    public BlaaizResponse get(String refundId) {
        requireNonBlank(refundId, "Refund ID is required");
        return client.makeRequest("GET", "/api/external/refund/" + refundId, null, null);
    }
}
