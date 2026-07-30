package com.blaaiz.sdk;

/** Business wallet lookups. */
public class WalletService extends BaseService {

    public WalletService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse list() {
        return client.makeRequest("GET", "/api/external/wallet", null, null);
    }

    public BlaaizResponse get(String walletId) {
        requireNonBlank(walletId, "Wallet ID is required");
        return client.makeRequest("GET", "/api/external/wallet/" + walletId, null, null);
    }
}
