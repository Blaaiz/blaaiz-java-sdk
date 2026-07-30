package com.blaaiz.sdk;

/** Result of {@link Blaaiz#createCompleteCollection}. */
public final class CompleteCollectionResult {

    private final String customerId;
    private final Object collection;
    private final Object virtualAccount;

    public CompleteCollectionResult(String customerId, Object collection, Object virtualAccount) {
        this.customerId = customerId;
        this.collection = collection;
        this.virtualAccount = virtualAccount;
    }

    public String getCustomerId() {
        return customerId;
    }

    /** The {@code data} payload of the collection-initiation response. */
    public Object getCollection() {
        return collection;
    }

    /** The {@code data} payload of the virtual-bank-account creation response, or {@code null} if {@code createVba} was false. */
    public Object getVirtualAccount() {
        return virtualAccount;
    }
}
