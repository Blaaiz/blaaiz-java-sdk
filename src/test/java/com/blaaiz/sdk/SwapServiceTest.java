package com.blaaiz.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SwapServiceTest {

    @Mock
    private BlaaizClient client;

    private SwapService swaps;

    @BeforeEach
    void setUp() {
        swaps = new SwapService(client);
    }

    // ---- required field validation ----

    @Test
    void swapThrowsWhenFromBusinessWalletIdMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("to_business_wallet_id", "w2");
        data.put("amount", 100);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> swaps.initiate(data));
        assertEquals("from_business_wallet_id is required", e.getMessage());
    }

    @Test
    void swapThrowsWhenToBusinessWalletIdMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_business_wallet_id", "w1");
        data.put("amount", 100);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> swaps.initiate(data));
        assertEquals("to_business_wallet_id is required", e.getMessage());
    }

    @Test
    void swapThrowsWhenAmountMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_business_wallet_id", "w1");
        data.put("to_business_wallet_id", "w2");

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> swaps.initiate(data));
        assertEquals("amount is required", e.getMessage());
    }

    @Test
    void swapThrowsWhenAmountIsZero() {
        // Mirrors the shared BaseService "empty" rule (Laravel empty() / Node !value / Python
        // not value): a zero amount is treated as absent, same as null or "".
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_business_wallet_id", "w1");
        data.put("to_business_wallet_id", "w2");
        data.put("amount", 0);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> swaps.initiate(data));
        assertEquals("amount is required", e.getMessage());
    }

    @Test
    void swapThrowsOnEmptyMap() {
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> swaps.initiate(new LinkedHashMap<>()));
        assertEquals("from_business_wallet_id is required", e.getMessage());
    }

    @Test
    void swapNeverCallsClientWhenValidationFails() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_business_wallet_id", "w1");

        assertThrows(IllegalArgumentException.class, () -> swaps.initiate(data));

        org.mockito.Mockito.verifyNoInteractions(client);
    }

    // ---- happy path ----

    @Test
    void swapSendsPostWithDataVerbatim() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_business_wallet_id", "w1");
        data.put("to_business_wallet_id", "w2");
        data.put("amount", 100);

        BlaaizResponse response =
                new BlaaizResponse(Map.of("message", "Money swap successful!"), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/swap"), eq(data), isNull()))
                .thenReturn(response);

        BlaaizResponse result = swaps.initiate(data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/swap", data, null);
    }

    @Test
    void swapForwardsOptionalAmountTypeFieldVerbatim() {
        // amount_type ("from"/"to") is not validated locally -- it is forwarded as-is, exactly
        // like the Laravel source (which only validates the three required fields).
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_business_wallet_id", "w1");
        data.put("to_business_wallet_id", "w2");
        data.put("amount", 160000);
        data.put("amount_type", "to");

        BlaaizResponse response =
                new BlaaizResponse(Map.of("message", "Money swap successful!"), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/swap"), eq(data), isNull()))
                .thenReturn(response);

        BlaaizResponse result = swaps.initiate(data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/swap", data, null);
    }

    // ---- error propagation ----

    @Test
    void swapPropagatesBlaaizExceptionFromClient() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_business_wallet_id", "w1");
        data.put("to_business_wallet_id", "w2");
        data.put("amount", 100);

        when(client.makeRequest(eq("POST"), eq("/api/external/swap"), eq(data), isNull()))
                .thenThrow(new BlaaizException("Insufficient balance", 400, "INSUFFICIENT_BALANCE"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> swaps.initiate(data));
        assertEquals("Insufficient balance", e.getMessage());
        assertEquals(400, e.getStatus());
        assertEquals("INSUFFICIENT_BALANCE", e.getErrorCode());
    }
}
