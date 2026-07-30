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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirtualBankAccountServiceTest {

    private static final String BASE = "/api/external/virtual-bank-account";

    @Mock
    private BlaaizClient client;

    private VirtualBankAccountService vbas;

    @BeforeEach
    void setUp() {
        vbas = new VirtualBankAccountService(client);
    }

    // ---- create ----

    @Test
    void createSendsVbaDataVerbatimToEndpoint() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("wallet_id", "wallet_1");
        data.put("customer_id", "cust_1");
        when(client.makeRequest(eq("POST"), eq(BASE), eq(data), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("id", "vba_1"), 200, null));

        BlaaizResponse result = vbas.create(data);

        assertEquals(200, result.getStatus());
        verify(client).makeRequest("POST", BASE, data, null);
    }

    @Test
    void createThrowsWhenWalletIdMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer_id", "cust_1");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> vbas.create(data));
        assertEquals("wallet_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createThrowsWhenWalletIdIsEmptyString() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("wallet_id", "");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> vbas.create(data));
        assertEquals("wallet_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createPropagatesBlaaizExceptionFromClient() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("wallet_id", "wallet_1");
        when(client.makeRequest(eq("POST"), eq(BASE), anyMap(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 422, "VALIDATION_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> vbas.create(data));
        assertEquals("API request failed", e.getMessage());
        assertEquals(422, e.getStatus());
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
    }

    // ---- list ----

    @Test
    void listWithBothFiltersOrdersWalletIdBeforeCustomerId() {
        when(client.makeRequest(eq("GET"), eq(BASE), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(List.of(), 200, null));

        vbas.list("wallet_1", "cust_1");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("wallet_id", "wallet_1");
        expected.put("customer_id", "cust_1");
        verify(client).makeRequest("GET", BASE, expected, null);
    }

    @Test
    void listWithOnlyWalletId() {
        when(client.makeRequest(eq("GET"), eq(BASE), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(List.of(), 200, null));

        vbas.list("wallet_1", null);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("wallet_id", "wallet_1");
        verify(client).makeRequest("GET", BASE, expected, null);
    }

    @Test
    void listWithOnlyCustomerId() {
        when(client.makeRequest(eq("GET"), eq(BASE), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(List.of(), 200, null));

        vbas.list(null, "cust_1");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("customer_id", "cust_1");
        verify(client).makeRequest("GET", BASE, expected, null);
    }

    @Test
    void listWithNeitherFilterSendsEmptyParamMap() {
        when(client.makeRequest(eq("GET"), eq(BASE), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(List.of(), 200, null));

        BlaaizResponse result = vbas.list(null, null);

        assertEquals(200, result.getStatus());
        verify(client).makeRequest("GET", BASE, new LinkedHashMap<>(), null);
    }

    @Test
    void listPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq(BASE), anyMap(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 500, "SERVER_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> vbas.list("wallet_1", null));
        assertEquals(500, e.getStatus());
    }

    // ---- get ----

    @Test
    void getSendsVbaIdInPath() {
        when(client.makeRequest(eq("GET"), eq(BASE + "/vba_1"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("id", "vba_1"), 200, null));

        BlaaizResponse result = vbas.get("vba_1");

        assertEquals(200, result.getStatus());
        verify(client).makeRequest("GET", BASE + "/vba_1", null, null);
    }

    @Test
    void getThrowsWhenVbaIdIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> vbas.get(null));
        assertEquals("Virtual bank account ID is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getThrowsWhenVbaIdIsEmptyString() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> vbas.get(""));
        assertEquals("Virtual bank account ID is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq(BASE + "/missing"), isNull(), isNull()))
                .thenThrow(new BlaaizException("Not found", 404, "NOT_FOUND"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> vbas.get("missing"));
        assertEquals(404, e.getStatus());
    }

    // ---- close ----

    @Test
    void closeWithoutReasonSendsEmptyBody() {
        when(client.makeRequest(eq("POST"), eq(BASE + "/vba_1/close"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("status", "closed"), 200, null));

        BlaaizResponse result = vbas.close("vba_1", null);

        assertEquals(200, result.getStatus());
        verify(client).makeRequest("POST", BASE + "/vba_1/close", new LinkedHashMap<>(), null);
    }

    @Test
    void closeWithReasonIncludesItInBody() {
        when(client.makeRequest(eq("POST"), eq(BASE + "/vba_1/close"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("status", "closed"), 200, null));

        vbas.close("vba_1", "No longer needed");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("reason", "No longer needed");
        verify(client).makeRequest("POST", BASE + "/vba_1/close", expected, null);
    }

    @Test
    void closeWithExplicitEmptyStringReasonStillIncludesIt() {
        // != null check, not truthiness -- an explicit "" reason is still sent.
        when(client.makeRequest(eq("POST"), eq(BASE + "/vba_1/close"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("status", "closed"), 200, null));

        vbas.close("vba_1", "");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("reason", "");
        verify(client).makeRequest("POST", BASE + "/vba_1/close", expected, null);
    }

    @Test
    void closeThrowsWhenVbaIdIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> vbas.close(null, "reason"));
        assertEquals("Virtual bank account ID is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void closeThrowsWhenVbaIdIsEmptyString() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> vbas.close("", "reason"));
        assertEquals("Virtual bank account ID is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void closePropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("POST"), eq(BASE + "/vba_1/close"), anyMap(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 409, "CONFLICT"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> vbas.close("vba_1", null));
        assertEquals(409, e.getStatus());
    }

    // ---- getIdentificationType ----

    private static final String ID_TYPE_ENDPOINT = BASE + "/identification-type";

    @Test
    void getIdentificationTypeWithCustomerIdOnly() {
        when(client.makeRequest(eq("GET"), eq(ID_TYPE_ENDPOINT), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("type", "BVN"), 200, null));

        BlaaizResponse result = vbas.getIdentificationType("cust_1", null, null);

        assertEquals(200, result.getStatus());
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("customer_id", "cust_1");
        verify(client).makeRequest("GET", ID_TYPE_ENDPOINT, expected, null);
    }

    @Test
    void getIdentificationTypeWithCountryAndTypeOnly() {
        when(client.makeRequest(eq("GET"), eq(ID_TYPE_ENDPOINT), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("type", "BVN"), 200, null));

        vbas.getIdentificationType(null, "NG", "individual");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("country", "NG");
        expected.put("type", "individual");
        verify(client).makeRequest("GET", ID_TYPE_ENDPOINT, expected, null);
    }

    @Test
    void getIdentificationTypeCustomerIdTakesPrecedenceOverCountryAndType() {
        when(client.makeRequest(eq("GET"), eq(ID_TYPE_ENDPOINT), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("type", "BVN"), 200, null));

        vbas.getIdentificationType("cust_1", "NG", "individual");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("customer_id", "cust_1");
        verify(client).makeRequest("GET", ID_TYPE_ENDPOINT, expected, null);
    }

    @Test
    void getIdentificationTypeThrowsWhenNothingProvided() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> vbas.getIdentificationType(null, null, null));
        assertEquals("Either customer_id or both country and type are required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getIdentificationTypeThrowsWhenOnlyCountryProvided() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> vbas.getIdentificationType(null, "NG", null));
        assertEquals("Either customer_id or both country and type are required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getIdentificationTypeThrowsWhenOnlyTypeProvided() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> vbas.getIdentificationType(null, null, "individual"));
        assertEquals("Either customer_id or both country and type are required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getIdentificationTypePropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq(ID_TYPE_ENDPOINT), anyMap(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 400, "BAD_REQUEST"));

        BlaaizException e = assertThrows(BlaaizException.class,
                () -> vbas.getIdentificationType("cust_1", null, null));
        assertEquals(400, e.getStatus());
    }
}
