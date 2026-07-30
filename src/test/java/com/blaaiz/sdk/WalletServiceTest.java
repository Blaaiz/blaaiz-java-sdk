package com.blaaiz.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private BlaaizClient client;

    private WalletService wallets;

    @BeforeEach
    void setUp() {
        wallets = new WalletService(client);
    }

    // ---- list ----

    @Test
    void listSendsBareGetWithNoParamsOrHeaders() {
        when(client.makeRequest(eq("GET"), eq("/api/external/wallet"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(List.of(Map.of("id", "wallet_1")), 200, null));

        BlaaizResponse result = wallets.list();

        assertEquals(200, result.getStatus());
        assertEquals(List.of(Map.of("id", "wallet_1")), result.getData());
        verify(client).makeRequest("GET", "/api/external/wallet", null, null);
    }

    @Test
    void listPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq("/api/external/wallet"), isNull(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 500, "SERVER_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> wallets.list());
        assertEquals("API request failed", e.getMessage());
        assertEquals(500, e.getStatus());
        assertEquals("SERVER_ERROR", e.getErrorCode());
    }

    // ---- get ----

    @Test
    void getSendsWalletIdInPath() {
        when(client.makeRequest(eq("GET"), eq("/api/external/wallet/wallet_1"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("id", "wallet_1"), 200, null));

        BlaaizResponse result = wallets.get("wallet_1");

        assertEquals(200, result.getStatus());
        verify(client).makeRequest("GET", "/api/external/wallet/wallet_1", null, null);
    }

    @Test
    void getThrowsWhenWalletIdIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> wallets.get(null));
        assertEquals("Wallet ID is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getThrowsWhenWalletIdIsEmptyString() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> wallets.get(""));
        assertEquals("Wallet ID is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq("/api/external/wallet/missing"), isNull(), isNull()))
                .thenThrow(new BlaaizException("Not found", 404, "NOT_FOUND"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> wallets.get("missing"));
        assertEquals("Not found", e.getMessage());
        assertEquals(404, e.getStatus());
        assertEquals("NOT_FOUND", e.getErrorCode());
    }
}
