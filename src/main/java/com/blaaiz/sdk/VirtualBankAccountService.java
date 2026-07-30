package com.blaaiz.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/** Virtual bank account (dedicated collection account) management. */
public class VirtualBankAccountService extends BaseService {

    public VirtualBankAccountService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse create(Map<String, Object> vbaData) {
        requireFields(vbaData, "wallet_id");
        return client.makeRequest("POST", "/api/external/virtual-bank-account", vbaData, null);
    }

    public BlaaizResponse list(String walletId, String customerId) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (walletId != null) {
            params.put("wallet_id", walletId);
        }
        if (customerId != null) {
            params.put("customer_id", customerId);
        }
        return client.makeRequest("GET", "/api/external/virtual-bank-account", params, null);
    }

    public BlaaizResponse get(String vbaId) {
        requireNonBlank(vbaId, "Virtual bank account ID is required");
        return client.makeRequest("GET", "/api/external/virtual-bank-account/" + vbaId, null, null);
    }

    /**
     * @param reason optional closure reason. Sent only when non-{@code null}; an explicit empty
     *               string is still sent (mirrors the Node.js/Python {@code != null} check).
     */
    public BlaaizResponse close(String vbaId, String reason) {
        requireNonBlank(vbaId, "Virtual bank account ID is required");
        Map<String, Object> data = new LinkedHashMap<>();
        if (reason != null) {
            data.put("reason", reason);
        }
        return client.makeRequest("POST", "/api/external/virtual-bank-account/" + vbaId + "/close", data, null);
    }

    public BlaaizResponse getIdentificationType(String customerId, String country, String type) {
        boolean hasCustomerId = customerId != null && !customerId.isEmpty();
        boolean hasCountryAndType = country != null && !country.isEmpty() && type != null && !type.isEmpty();
        if (!hasCustomerId && !hasCountryAndType) {
            throw new IllegalArgumentException("Either customer_id or both country and type are required");
        }

        Map<String, Object> params = new LinkedHashMap<>();
        if (hasCustomerId) {
            params.put("customer_id", customerId);
        } else {
            params.put("country", country);
            params.put("type", type);
        }
        return client.makeRequest("GET", "/api/external/virtual-bank-account/identification-type", params, null);
    }
}
