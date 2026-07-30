package com.blaaiz.sdk;

import java.util.Map;

/** Fee calculation. */
public class FeesService extends BaseService {

    public FeesService(BlaaizClient client) {
        super(client);
    }

    /**
     * @param feeData must contain {@code from_currency_id}, {@code to_currency_id}, and either
     *                {@code from_amount} or {@code to_amount}.
     */
    public BlaaizResponse getBreakdown(Map<String, Object> feeData) {
        requireFields(feeData, "from_currency_id", "to_currency_id");
        if (isBlank(feeData.get("from_amount")) && isBlank(feeData.get("to_amount"))) {
            throw new IllegalArgumentException("Either from_amount or to_amount is required");
        }
        return client.makeRequest("POST", "/api/external/fees/breakdown", feeData, null);
    }
}
