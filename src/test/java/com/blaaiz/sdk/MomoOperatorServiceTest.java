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
class MomoOperatorServiceTest {

    @Mock
    private BlaaizClient client;

    private MomoOperatorService momoOperators;

    @BeforeEach
    void setUp() {
        momoOperators = new MomoOperatorService(client);
    }

    @Test
    void listSendsBareGetWithNoParamsOrHeaders() {
        BlaaizResponse response = new BlaaizResponse(
                List.of(Map.of("id", "operator_1", "name", "Test Operator")), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/momo-operator"), isNull(), isNull()))
                .thenReturn(response);

        BlaaizResponse result = momoOperators.list();

        assertEquals(response, result);
        verify(client).makeRequest("GET", "/api/external/momo-operator", null, null);
    }

    @Test
    void listPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq("/api/external/momo-operator"), isNull(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 500, "SERVER_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> momoOperators.list());
        assertEquals("API request failed", e.getMessage());
        assertEquals(500, e.getStatus());
        assertEquals("SERVER_ERROR", e.getErrorCode());
    }

    @Test
    void listSendsFiltersAsQueryParams() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("currency_id", "currency_1");
        filters.put("country_id", 5);
        BlaaizResponse response = new BlaaizResponse(List.of(), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/momo-operator"), eq(filters), isNull()))
                .thenReturn(response);

        BlaaizResponse result = momoOperators.list(filters);

        assertEquals(response, result);
        verify(client).makeRequest("GET", "/api/external/momo-operator", filters, null);
    }

    @Test
    void listSendsNoParamsWhenFiltersEmpty() {
        BlaaizResponse response = new BlaaizResponse(List.of(), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/momo-operator"), isNull(), isNull()))
                .thenReturn(response);

        momoOperators.list(new LinkedHashMap<>());

        verify(client).makeRequest("GET", "/api/external/momo-operator", null, null);
    }
}
