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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private BlaaizClient client;

    private RefundService refunds;

    @BeforeEach
    void setUp() {
        refunds = new RefundService(client);
    }

    // ---- initiate ----

    @Test
    void initiateSendsPostToRefundEndpoint() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transaction_id", "txn_1");
        data.put("reason", "duplicate");
        BlaaizResponse response = new BlaaizResponse(Map.of("id", "ref_1"), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/refund"), eq(data), isNull())).thenReturn(response);

        BlaaizResponse result = refunds.initiate(data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/refund", data, null);
    }

    @Test
    void initiateThrowsWhenTransactionIdMissing() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> refunds.initiate(new LinkedHashMap<>()));
        assertEquals("transaction_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void initiateThrowsWhenDataIsNull() {
        assertThrows(IllegalArgumentException.class, () -> refunds.initiate(null));
        verifyNoInteractions(client);
    }

    // ---- get ----

    @Test
    void getSendsRefundIdInPath() {
        BlaaizResponse response = new BlaaizResponse(Map.of("id", "ref_1"), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/refund/ref_1"), isNull(), isNull())).thenReturn(response);

        BlaaizResponse result = refunds.get("ref_1");

        assertEquals(response, result);
        verify(client).makeRequest("GET", "/api/external/refund/ref_1", null, null);
    }

    @Test
    void getRequiresNonBlankRefundId() {
        assertThrows(IllegalArgumentException.class, () -> refunds.get(""));
        assertThrows(IllegalArgumentException.class, () -> refunds.get(null));
        verifyNoInteractions(client);
    }

    // ---- error propagation ----

    @Test
    void getPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq("/api/external/refund/ref_x"), isNull(), isNull()))
                .thenThrow(new BlaaizException("Refund not found", 404, "NOT_FOUND"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> refunds.get("ref_x"));
        assertEquals("Refund not found", e.getMessage());
        assertEquals(404, e.getStatus());
        assertEquals("NOT_FOUND", e.getErrorCode());
    }
}
