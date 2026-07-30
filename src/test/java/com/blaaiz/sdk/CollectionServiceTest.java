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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private BlaaizClient client;

    private CollectionService collections;

    @BeforeEach
    void setUp() {
        collections = new CollectionService(client);
    }

    private static Map<String, Object> validCollection() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer_id", "cust_1");
        data.put("wallet_id", "wallet_1");
        data.put("amount", 100);
        data.put("currency", "NGN");
        data.put("method", "bank_transfer");
        return data;
    }

    // ---- initiate ----

    @Test
    void initiateSendsPostToCollectionEndpoint() {
        BlaaizResponse response = new BlaaizResponse(Map.of("id", "coll_1"), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/collection"), anyMap(), isNull())).thenReturn(response);

        BlaaizResponse result = collections.initiate(validCollection());

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/collection", validCollection(), null);
    }

    @Test
    void initiateForwardsOptionalNarrationVerbatim() {
        Map<String, Object> data = validCollection();
        data.put("narration", "Invoice #123");
        when(client.makeRequest(eq("POST"), eq("/api/external/collection"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.initiate(data);

        // narration is not validated -- it is forwarded exactly as any other undocumented field.
        verify(client).makeRequest("POST", "/api/external/collection", data, null);
    }

    @Test
    void initiateThrowsWhenRequiredFieldMissing() {
        for (String field : new String[] {"customer_id", "wallet_id", "amount", "currency", "method"}) {
            Map<String, Object> data = validCollection();
            data.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> collections.initiate(data));
            assertEquals(field + " is required", e.getMessage());
        }
        verifyNoInteractions(client);
    }

    @Test
    void initiateTreatsFalsyValuesAsMissing() {
        Map<String, Object> data = validCollection();
        data.put("amount", 0);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> collections.initiate(data));
        assertEquals("amount is required", e.getMessage());
    }

    // ---- initiateCrypto ----

    @Test
    void initiateCryptoSendsPostWithNoValidation() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer_id", "cust_1");
        when(client.makeRequest(eq("POST"), eq("/api/external/collection/crypto"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.initiateCrypto(data);

        verify(client).makeRequest("POST", "/api/external/collection/crypto", data, null);
    }

    @Test
    void initiateCryptoAllowsEmptyMap() {
        // No SDK-side validation: even an empty payload is forwarded as-is.
        when(client.makeRequest(eq("POST"), eq("/api/external/collection/crypto"), any(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.initiateCrypto(Map.of());

        verify(client).makeRequest("POST", "/api/external/collection/crypto", Map.of(), null);
    }

    // ---- attachCustomer ----

    @Test
    void attachCustomerSendsPostToAttachEndpoint() {
        Map<String, Object> data = Map.of("customer_id", "cust_1", "transaction_id", "txn_1");
        when(client.makeRequest(eq("POST"), eq("/api/external/collection/attach-customer"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.attachCustomer(data);

        verify(client).makeRequest("POST", "/api/external/collection/attach-customer", data, null);
    }

    @Test
    void attachCustomerThrowsWhenCustomerIdMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transaction_id", "txn_1");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> collections.attachCustomer(data));
        assertEquals("customer_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void attachCustomerThrowsWhenTransactionIdMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer_id", "cust_1");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> collections.attachCustomer(data));
        assertEquals("transaction_id is required", e.getMessage());
    }

    // ---- getCryptoNetworks ----

    @Test
    void getCryptoNetworksSendsGetWithNoBody() {
        BlaaizResponse response = new BlaaizResponse(Map.of("networks", java.util.List.of("TRC20", "ERC20")), 200, null);
        when(client.makeRequest(eq("GET"), eq("/api/external/collection/crypto/networks"), isNull(), isNull()))
                .thenReturn(response);

        BlaaizResponse result = collections.getCryptoNetworks();

        assertEquals(response, result);
        verify(client).makeRequest("GET", "/api/external/collection/crypto/networks", null, null);
    }

    // ---- acceptInteracMoneyRequest ----

    @Test
    void acceptInteracMoneyRequestSendsPostToAcceptEndpoint() {
        Map<String, Object> data = Map.of("reference_number", "REF123");
        when(client.makeRequest(eq("POST"), eq("/api/external/collection/accept-interac-money-request"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.acceptInteracMoneyRequest(data);

        verify(client).makeRequest("POST", "/api/external/collection/accept-interac-money-request", data, null);
    }

    @Test
    void acceptInteracMoneyRequestThrowsWhenReferenceNumberMissing() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> collections.acceptInteracMoneyRequest(new LinkedHashMap<>()));
        assertEquals("reference_number is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void acceptInteracMoneyRequestThrowsWhenDataIsNull() {
        assertThrows(IllegalArgumentException.class, () -> collections.acceptInteracMoneyRequest(null));
        verifyNoInteractions(client);
    }

    // ---- propagates transport failures untouched ----

    @Test
    void initiatePropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("POST"), eq("/api/external/collection"), anyMap(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 400, "VALIDATION_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> collections.initiate(validCollection()));
        assertEquals("API request failed", e.getMessage());
        assertEquals(400, e.getStatus());
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
    }
}
