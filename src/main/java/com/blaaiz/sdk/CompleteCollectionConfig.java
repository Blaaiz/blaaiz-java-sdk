package com.blaaiz.sdk;

import java.util.Map;

/** Input to {@link Blaaiz#createCompleteCollection}. */
public final class CompleteCollectionConfig {

    private Map<String, Object> customerData;
    private Map<String, Object> collectionData;
    private boolean createVba = false;

    /** Optional: if present and {@code collectionData} has no {@code customer_id}, a customer is created first. */
    public CompleteCollectionConfig customerData(Map<String, Object> customerData) {
        this.customerData = customerData;
        return this;
    }

    public Map<String, Object> getCustomerData() {
        return customerData;
    }

    /** Required: forwarded to {@link CollectionService#initiate}, see there for required fields. */
    public CompleteCollectionConfig collectionData(Map<String, Object> collectionData) {
        this.collectionData = collectionData;
        return this;
    }

    public Map<String, Object> getCollectionData() {
        return collectionData;
    }

    /** Whether to create a virtual bank account for {@code collectionData.wallet_id} first. Defaults to {@code false}. */
    public CompleteCollectionConfig createVba(boolean createVba) {
        this.createVba = createVba;
        return this;
    }

    public boolean isCreateVba() {
        return createVba;
    }
}
