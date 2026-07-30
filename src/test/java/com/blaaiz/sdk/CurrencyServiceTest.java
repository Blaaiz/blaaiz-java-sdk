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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private BlaaizClient client;

    private CurrencyService currencies;

    @BeforeEach
    void setUp() {
        currencies = new CurrencyService(client);
    }

    @Test
    void listSendsBareGetWithNoParamsOrHeaders() {
        BlaaizResponse response = new BlaaizResponse(
                List.of(Map.of("id", "NGN", "name", "Nigerian Naira")), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/currency"), isNull(), isNull()))
                .thenReturn(response);

        BlaaizResponse result = currencies.list();

        assertEquals(response, result);
        verify(client).makeRequest("GET", "/api/external/currency", null, null);
    }

    @Test
    void listReturnsResponseUnmodified() {
        // Mirrors the Laravel/Node.js/Python source SDKs: list() has no params and performs
        // no local validation or unwrapping -- the client's response is returned as-is.
        BlaaizResponse response = new BlaaizResponse(Map.of("data", List.of()), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/currency"), isNull(), isNull()))
                .thenReturn(response);

        assertEquals(response, currencies.list());
    }

    @Test
    void listPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq("/api/external/currency"), isNull(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 500, "SERVER_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> currencies.list());
        assertEquals("API request failed", e.getMessage());
        assertEquals(500, e.getStatus());
        assertEquals("SERVER_ERROR", e.getErrorCode());
    }

    @Test
    void listPropagatesUnauthorizedError() {
        when(client.makeRequest(eq("GET"), eq("/api/external/currency"), isNull(), isNull()))
                .thenThrow(new BlaaizException("Unauthorized", 401, "UNAUTHORIZED"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> currencies.list());
        assertEquals(401, e.getStatus());
        assertEquals("UNAUTHORIZED", e.getErrorCode());
    }
}
