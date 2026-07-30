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
class FeesServiceTest {

    @Mock
    private BlaaizClient client;

    private FeesService fees;

    @BeforeEach
    void setUp() {
        fees = new FeesService(client);
    }

    private static Map<String, Object> withFromAmount() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_currency_id", "cur_1");
        data.put("to_currency_id", "cur_2");
        data.put("from_amount", 100);
        return data;
    }

    private static Map<String, Object> withToAmount() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_currency_id", "cur_1");
        data.put("to_currency_id", "cur_2");
        data.put("to_amount", 100);
        return data;
    }

    // ---- happy path ----

    @Test
    void getBreakdownSendsPostWithFromAmount() {
        Map<String, Object> data = withFromAmount();
        BlaaizResponse response = new BlaaizResponse(Map.of("fee", 2.5), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/fees/breakdown"), eq(data), isNull()))
                .thenReturn(response);

        BlaaizResponse result = fees.getBreakdown(data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/fees/breakdown", data, null);
    }

    @Test
    void getBreakdownSendsPostWithToAmount() {
        Map<String, Object> data = withToAmount();
        BlaaizResponse response = new BlaaizResponse(Map.of("fee", 2.5), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/fees/breakdown"), eq(data), isNull()))
                .thenReturn(response);

        BlaaizResponse result = fees.getBreakdown(data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/fees/breakdown", data, null);
    }

    @Test
    void getBreakdownAllowsBothFromAmountAndToAmountProvided() {
        // Neither the API contract nor any of the three source SDKs reject supplying both --
        // the value is forwarded as-is, whichever amount fields the caller included.
        Map<String, Object> data = withFromAmount();
        data.put("to_amount", 50);
        BlaaizResponse response = new BlaaizResponse(Map.of("fee", 2.5), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/fees/breakdown"), eq(data), isNull()))
                .thenReturn(response);

        BlaaizResponse result = fees.getBreakdown(data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/fees/breakdown", data, null);
    }

    // ---- required field validation ----

    @Test
    void getBreakdownThrowsWhenFromCurrencyIdMissing() {
        Map<String, Object> data = withFromAmount();
        data.remove("from_currency_id");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fees.getBreakdown(data));
        assertEquals("from_currency_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getBreakdownThrowsWhenFromCurrencyIdBlank() {
        Map<String, Object> data = withFromAmount();
        data.put("from_currency_id", "");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fees.getBreakdown(data));
        assertEquals("from_currency_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getBreakdownThrowsWhenToCurrencyIdMissing() {
        Map<String, Object> data = withFromAmount();
        data.remove("to_currency_id");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fees.getBreakdown(data));
        assertEquals("to_currency_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getBreakdownChecksFromCurrencyIdBeforeToCurrencyId() {
        // Both fields missing: from_currency_id is validated first, matching the field order
        // shared by the Laravel/Node.js/Python source SDKs.
        Map<String, Object> data = new LinkedHashMap<>();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fees.getBreakdown(data));
        assertEquals("from_currency_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getBreakdownThrowsWhenFeeDataIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fees.getBreakdown(null));
        assertEquals("from_currency_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    // ---- from_amount / to_amount cross-field validation ----

    @Test
    void getBreakdownThrowsWhenNeitherAmountProvided() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_currency_id", "cur_1");
        data.put("to_currency_id", "cur_2");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fees.getBreakdown(data));
        assertEquals("Either from_amount or to_amount is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getBreakdownThrowsWhenFromAmountIsZero() {
        // Zero is "falsy" under the same empty()/!value/not-value rule the Laravel, Node.js
        // and Python SDKs all use, so a zero from_amount does not satisfy the requirement.
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_currency_id", "cur_1");
        data.put("to_currency_id", "cur_2");
        data.put("from_amount", 0);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fees.getBreakdown(data));
        assertEquals("Either from_amount or to_amount is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getBreakdownThrowsWhenFromAmountIsBlankString() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from_currency_id", "cur_1");
        data.put("to_currency_id", "cur_2");
        data.put("from_amount", "");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fees.getBreakdown(data));
        assertEquals("Either from_amount or to_amount is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getBreakdownAmountCheckRunsAfterCurrencyIdChecks() {
        // Currency id fields are still validated first even though both amounts are absent.
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("to_currency_id", "cur_2");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fees.getBreakdown(data));
        assertEquals("from_currency_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    // ---- error propagation ----

    @Test
    void getBreakdownPropagatesBlaaizExceptionFromClient() {
        Map<String, Object> data = withFromAmount();
        when(client.makeRequest(eq("POST"), eq("/api/external/fees/breakdown"), eq(data), isNull()))
                .thenThrow(new BlaaizException("API request failed", 422, "VALIDATION_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> fees.getBreakdown(data));
        assertEquals("API request failed", e.getMessage());
        assertEquals(422, e.getStatus());
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
    }
}
