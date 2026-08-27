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
        data.put("method", "open_banking");
        data.put("amount", 100);
        data.put("wallet_id", "wallet_1");
        return data;
    }

    private static Map<String, Object> validCardCollection() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", "card");
        data.put("amount", 100);
        data.put("wallet_id", "wallet_1");
        data.put("customer_id", "cust_1");
        data.put("card_holder_name", "Jane Doe");
        data.put("card_number", "4111111111111111");
        data.put("expiry", "12/30");
        data.put("cvc", "123");
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
        for (String field : new String[] {"method", "amount", "wallet_id"}) {
            Map<String, Object> data = validCollection();
            data.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> collections.initiate(data));
            assertEquals(field + " is required", e.getMessage());
        }
        verifyNoInteractions(client);
    }

    @Test
    void initiateDoesNotRequireCustomerIdForOpenBanking() {
        // Only card collections need customer_id up front; open_banking does not.
        when(client.makeRequest(eq("POST"), eq("/api/external/collection"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.initiate(validCollection());

        verify(client).makeRequest("POST", "/api/external/collection", validCollection(), null);
    }

    @Test
    void initiateCardSendsPostWhenAllCardFieldsPresent() {
        when(client.makeRequest(eq("POST"), eq("/api/external/collection"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.initiate(validCardCollection());

        verify(client).makeRequest("POST", "/api/external/collection", validCardCollection(), null);
    }

    @Test
    void initiateCardThrowsWhenCardFieldMissing() {
        for (String field : new String[] {"customer_id", "card_holder_name", "card_number", "expiry", "cvc"}) {
            Map<String, Object> data = validCardCollection();
            data.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> collections.initiate(data));
            assertEquals(field + " is required", e.getMessage());
        }
        verifyNoInteractions(client);
    }

    @Test
    void initiateForwardsOptionalMerchantReferenceVerbatim() {
        Map<String, Object> data = validCollection();
        data.put("merchant_reference", "order-123");
        when(client.makeRequest(eq("POST"), eq("/api/external/collection"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.initiate(data);

        verify(client).makeRequest("POST", "/api/external/collection", data, null);
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

    @Test
    void getCryptoNetworksSendsFiltersAsQueryParams() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("transaction_type", "payout");
        when(client.makeRequest(eq("GET"), eq("/api/external/collection/crypto/networks"), eq(filters), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.getCryptoNetworks(filters);

        verify(client).makeRequest("GET", "/api/external/collection/crypto/networks", filters, null);
    }

    @Test
    void getCryptoNetworksSendsNoParamsWhenFiltersEmpty() {
        when(client.makeRequest(eq("GET"), eq("/api/external/collection/crypto/networks"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.getCryptoNetworks(new LinkedHashMap<>());

        verify(client).makeRequest("GET", "/api/external/collection/crypto/networks", null, null);
    }

    // ---- initiateInteracMoneyRequest ----

    @Test
    void initiateInteracMoneyRequestSendsPostToEndpoint() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("amount", 100);
        data.put("email", "payer@example.com");
        when(client.makeRequest(eq("POST"), eq("/api/external/collection/interac-money-request"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        collections.initiateInteracMoneyRequest(data);

        verify(client).makeRequest("POST", "/api/external/collection/interac-money-request", data, null);
    }

    @Test
    void initiateInteracMoneyRequestThrowsWhenAmountMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", "payer@example.com");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> collections.initiateInteracMoneyRequest(data));
        assertEquals("amount is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void initiateInteracMoneyRequestThrowsWhenEmailMissing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("amount", 100);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> collections.initiateInteracMoneyRequest(data));
        assertEquals("email is required", e.getMessage());
        verifyNoInteractions(client);
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
