package com.blaaiz.sdk;

import java.util.Map;

/** Input to {@link Blaaiz#createCompletePayout}. */
public final class CompletePayoutConfig {

    private Map<String, Object> customerData;
    private Map<String, Object> payoutData;

    /** Optional: if present and {@code payoutData} has no {@code customer_id}, a customer is created first. */
    public CompletePayoutConfig customerData(Map<String, Object> customerData) {
        this.customerData = customerData;
        return this;
    }

    public Map<String, Object> getCustomerData() {
        return customerData;
    }

    /** Required: forwarded to {@link PayoutService#initiate}, see there for required fields. */
    public CompletePayoutConfig payoutData(Map<String, Object> payoutData) {
        this.payoutData = payoutData;
        return this;
    }

    public Map<String, Object> getPayoutData() {
        return payoutData;
    }
}
