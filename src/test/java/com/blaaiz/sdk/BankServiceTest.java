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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    private BlaaizClient client;

    private BankService banks;

    @BeforeEach
    void setUp() {
        banks = new BankService(client);
    }

    private static Map<String, Object> validLookup() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("account_number", "0123456789");
        data.put("bank_id", "bank_1");
        return data;
    }

    // ---- list ----

    @Test
    void listSendsBareGetWithNoParamsOrHeaders() {
        BlaaizResponse response = new BlaaizResponse(
                List.of(Map.of("id", "bank_1", "name", "Test Bank")), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/bank"), isNull(), isNull())).thenReturn(response);

        BlaaizResponse result = banks.list();

        assertEquals(response, result);
        verify(client).makeRequest("GET", "/api/external/bank", null, null);
    }

    @Test
    void listPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq("/api/external/bank"), isNull(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 500, "SERVER_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> banks.list());
        assertEquals("API request failed", e.getMessage());
        assertEquals(500, e.getStatus());
        assertEquals("SERVER_ERROR", e.getErrorCode());
    }

    // ---- lookupAccount ----

    @Test
    void lookupAccountSendsPostToAccountLookupEndpoint() {
        BlaaizResponse response = new BlaaizResponse(
                Map.of("account_name", "John Doe"), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/bank/account-lookup"), eq(validLookup()), isNull()))
                .thenReturn(response);

        BlaaizResponse result = banks.lookupAccount(validLookup());

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/bank/account-lookup", validLookup(), null);
    }

    @Test
    void lookupAccountThrowsWhenAccountNumberMissing() {
        Map<String, Object> data = validLookup();
        data.remove("account_number");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> banks.lookupAccount(data));
        assertEquals("account_number is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void lookupAccountThrowsWhenAccountNumberBlank() {
        Map<String, Object> data = validLookup();
        data.put("account_number", "");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> banks.lookupAccount(data));
        assertEquals("account_number is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void lookupAccountThrowsWhenBankIdMissing() {
        Map<String, Object> data = validLookup();
        data.remove("bank_id");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> banks.lookupAccount(data));
        assertEquals("bank_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void lookupAccountChecksAccountNumberBeforeBankId() {
        // Both fields missing: account_number is validated first, matching the check order
        // shared by the Laravel/Node.js/Python source SDKs.
        Map<String, Object> data = new LinkedHashMap<>();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> banks.lookupAccount(data));
        assertEquals("account_number is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void lookupAccountThrowsWhenLookupDataIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> banks.lookupAccount(null));
        assertEquals("account_number is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void lookupAccountPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(
                eq("POST"), eq("/api/external/bank/account-lookup"), eq(validLookup()), isNull()))
                .thenThrow(new BlaaizException("Bank not found", 404, "NOT_FOUND"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> banks.lookupAccount(validLookup()));
        assertEquals("Bank not found", e.getMessage());
        assertEquals(404, e.getStatus());
        assertEquals("NOT_FOUND", e.getErrorCode());
    }
}
