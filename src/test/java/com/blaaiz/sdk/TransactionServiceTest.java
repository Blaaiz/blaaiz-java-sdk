package com.blaaiz.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
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
class TransactionServiceTest {

    @Mock
    private BlaaizClient client;

    private TransactionService transactions;

    @BeforeEach
    void setUp() {
        transactions = new TransactionService(client);
    }

    // ---- list ----

    @Test
    void listSendsFiltersAsPostBody() {
        Map<String, Object> filters = Map.of("status", "completed", "page", 1);
        when(client.makeRequest(eq("POST"), eq("/api/external/transaction"), eq(filters), isNull()))
                .thenReturn(new BlaaizResponse(List.of(Map.of("id", "txn_1")), 200, null));

        BlaaizResponse result = transactions.list(filters);

        assertEquals(200, result.getStatus());
        assertEquals(List.of(Map.of("id", "txn_1")), result.getData());
        verify(client).makeRequest("POST", "/api/external/transaction", filters, null);
    }

    @Test
    void listSendsEmptyMapWhenFiltersIsNull() {
        when(client.makeRequest(eq("POST"), eq("/api/external/transaction"), eq(Collections.emptyMap()), isNull()))
                .thenReturn(new BlaaizResponse(List.of(), 200, null));

        BlaaizResponse result = transactions.list(null);

        assertEquals(200, result.getStatus());
        verify(client).makeRequest("POST", "/api/external/transaction", Collections.emptyMap(), null);
    }

    @Test
    void listIsPostNotGetEvenThoughItIsAListOperation() {
        when(client.makeRequest(eq("POST"), eq("/api/external/transaction"), eq(Collections.emptyMap()), isNull()))
                .thenReturn(new BlaaizResponse(List.of(), 200, null));

        transactions.list(Collections.emptyMap());

        verify(client).makeRequest(eq("POST"), eq("/api/external/transaction"), eq(Collections.emptyMap()), isNull());
    }

    @Test
    void listPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("POST"), eq("/api/external/transaction"), eq(Collections.emptyMap()), isNull()))
                .thenThrow(new BlaaizException("API request failed", 500, "SERVER_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> transactions.list(null));
        assertEquals("API request failed", e.getMessage());
        assertEquals(500, e.getStatus());
        assertEquals("SERVER_ERROR", e.getErrorCode());
    }

    // ---- get ----

    @Test
    void getSendsTransactionIdInPath() {
        when(client.makeRequest(eq("GET"), eq("/api/external/transaction/txn_1"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("id", "txn_1"), 200, null));

        BlaaizResponse result = transactions.get("txn_1");

        assertEquals(200, result.getStatus());
        assertEquals(Map.of("id", "txn_1"), result.getData());
        verify(client).makeRequest("GET", "/api/external/transaction/txn_1", null, null);
    }

    @Test
    void getThrowsWhenTransactionIdIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> transactions.get(null));
        assertEquals("Transaction ID is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getThrowsWhenTransactionIdIsEmptyString() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> transactions.get(""));
        assertEquals("Transaction ID is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq("/api/external/transaction/missing"), isNull(), isNull()))
                .thenThrow(new BlaaizException("Not found", 404, "NOT_FOUND"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> transactions.get("missing"));
        assertEquals("Not found", e.getMessage());
        assertEquals(404, e.getStatus());
        assertEquals("NOT_FOUND", e.getErrorCode());
    }
}
