package com.blaaiz.sdk;

/** Result of {@link Blaaiz#createCompletePayout}. */
public final class CompletePayoutResult {

    private final String customerId;
    private final Object payout;
    private final Object fees;

    public CompletePayoutResult(String customerId, Object payout, Object fees) {
        this.customerId = customerId;
        this.payout = payout;
        this.fees = fees;
    }

    public String getCustomerId() {
        return customerId;
    }

    /** The {@code data} payload of the payout-initiation response. */
    public Object getPayout() {
        return payout;
    }

    /** The {@code data} payload of the fee-breakdown response. */
    public Object getFees() {
        return fees;
    }
}
