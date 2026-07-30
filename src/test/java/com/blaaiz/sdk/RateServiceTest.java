package com.blaaiz.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateServiceTest {

    @Mock
    private BlaaizClient client;

    private RateService rates;

    @BeforeEach
    void setUp() {
        rates = new RateService(client);
    }

    // ---- list without a search term ----

    @Test
    void listSendsBareGetWithNoParamsWhenSearchTermIsNull() {
        BlaaizResponse response = new BlaaizResponse(
                List.of(Map.of("pair", "USD/NGN", "value", "1600")), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/rate"), isNull(), isNull())).thenReturn(response);

        BlaaizResponse result = rates.list(null);

        assertEquals(response, result);
        verify(client).makeRequest("GET", "/api/external/rate", null, null);
    }

    // ---- list with a search term ----

    @Test
    void listSendsSearchTermAsQueryParam() {
        Map<String, Object> expectedParams = new LinkedHashMap<>();
        expectedParams.put("search_term", "USD");
        BlaaizResponse response = new BlaaizResponse(
                List.of(Map.of("pair", "USD/NGN", "value", "1600")), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/rate"), eq(expectedParams), isNull()))
                .thenReturn(response);

        BlaaizResponse result = rates.list("USD");

        assertEquals(response, result);
        verify(client).makeRequest("GET", "/api/external/rate", expectedParams, null);
    }

    @Test
    void listSendsEmptyStringSearchTermAsQueryParam() {
        // Mirrors the Laravel source: only a null search term is omitted -- an explicit empty
        // string is still a non-null value and is forwarded as-is.
        Map<String, Object> expectedParams = new LinkedHashMap<>();
        expectedParams.put("search_term", "");
        BlaaizResponse response = new BlaaizResponse(List.of(), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/rate"), eq(expectedParams), isNull()))
                .thenReturn(response);

        BlaaizResponse result = rates.list("");

        assertEquals(response, result);
        verify(client).makeRequest("GET", "/api/external/rate", expectedParams, null);
    }

    // ---- error propagation ----

    @Test
    void listPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq("/api/external/rate"), isNull(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 500, "SERVER_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> rates.list(null));
        assertEquals("API request failed", e.getMessage());
        assertEquals(500, e.getStatus());
        assertEquals("SERVER_ERROR", e.getErrorCode());
    }

    @Test
    void listPropagatesBlaaizExceptionFromClientWithSearchTerm() {
        Map<String, Object> expectedParams = new LinkedHashMap<>();
        expectedParams.put("search_term", "ZZZ");
        when(client.makeRequest(eq("GET"), eq("/api/external/rate"), eq(expectedParams), isNull()))
                .thenThrow(new BlaaizException("Not found", 404, "NOT_FOUND"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> rates.list("ZZZ"));
        assertEquals("Not found", e.getMessage());
        assertEquals(404, e.getStatus());
        assertEquals("NOT_FOUND", e.getErrorCode());
    }
}
