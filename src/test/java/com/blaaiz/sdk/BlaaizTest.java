package com.blaaiz.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlaaizTest {

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private Call call;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void stubNewCall() {
        org.mockito.Mockito.lenient().when(okHttpClient.newCall(any())).thenReturn(call);
    }

    private static Response jsonResponse(int code, String body) {
        Request dummyRequest = new Request.Builder().url("https://api-dev.blaaiz.com/x").build();
        return new Response.Builder()
                .request(dummyRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("msg")
                .body(ResponseBody.create(body, MediaType.parse("application/json")))
                .build();
    }

    private Blaaiz newBlaaiz() {
        return new Blaaiz(new BlaaizClientOptions().apiKey("test-key"), okHttpClient);
    }

    private static String bodyOf(Request request) throws IOException {
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }

    // ---- service accessors ----

    @Test
    void serviceAccessorsReturnNonNullInstances() {
        Blaaiz blaaiz = newBlaaiz();

        assertNotNull(blaaiz.customers());
        assertNotNull(blaaiz.collections());
        assertNotNull(blaaiz.payouts());
        assertNotNull(blaaiz.wallets());
        assertNotNull(blaaiz.virtualBankAccounts());
        assertNotNull(blaaiz.transactions());
        assertNotNull(blaaiz.banks());
        assertNotNull(blaaiz.currencies());
        assertNotNull(blaaiz.fees());
        assertNotNull(blaaiz.files());
        assertNotNull(blaaiz.webhooks());
        assertNotNull(blaaiz.rates());
        assertNotNull(blaaiz.swaps());
        assertNotNull(blaaiz.refunds());
    }

    @Test
    void serviceAccessorsReturnTheSameInstanceOnRepeatedCalls() {
        Blaaiz blaaiz = newBlaaiz();
        assertEquals(blaaiz.customers(), blaaiz.customers());
    }

    // ---- testConnection ----

    @Test
    void testConnectionReturnsTrueOnSuccess() throws IOException {
        Blaaiz blaaiz = newBlaaiz();
        when(call.execute()).thenReturn(jsonResponse(200, "{\"data\":[]}"));

        assertTrue(blaaiz.testConnection());
    }

    @Test
    void testConnectionReturnsFalseOnHttpFailure() throws IOException {
        Blaaiz blaaiz = newBlaaiz();
        when(call.execute()).thenReturn(jsonResponse(500, "{\"message\":\"boom\"}"));

        assertFalse(blaaiz.testConnection());
    }

    @Test
    void testConnectionReturnsFalseOnTransportFailure() throws IOException {
        Blaaiz blaaiz = newBlaaiz();
        when(call.execute()).thenThrow(new IOException("network down"));

        assertFalse(blaaiz.testConnection());
    }

    // ---- createCompletePayout ----

    @Test
    void createCompletePayoutRequiresPayoutData() {
        Blaaiz blaaiz = newBlaaiz();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> blaaiz.createCompletePayout(new CompletePayoutConfig()));
        assertEquals("payoutData is required", e.getMessage());
    }

    @Test
    void createCompletePayoutCreatesCustomerThenFeesThenPayout() throws IOException {
        Blaaiz blaaiz = newBlaaiz();

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(captor.capture())).thenReturn(call);
        when(call.execute()).thenReturn(
                jsonResponse(201, "{\"data\":{\"id\":\"cust-123\"}}"),
                jsonResponse(200, "{\"data\":{\"fee\":5}}"),
                jsonResponse(201, "{\"data\":{\"id\":\"payout-1\"}}"));

        Map<String, Object> customerData = new LinkedHashMap<>();
        customerData.put("type", "individual");
        customerData.put("email", "a@b.com");
        customerData.put("country", "NG");
        customerData.put("id_type", "passport");
        customerData.put("id_number", "123");
        customerData.put("first_name", "Ada");
        customerData.put("last_name", "Lovelace");

        Map<String, Object> payoutData = new LinkedHashMap<>();
        payoutData.put("wallet_id", "wallet-1");
        payoutData.put("method", "crypto");
        payoutData.put("from_currency_id", "USD");
        payoutData.put("to_currency_id", "NGN");
        payoutData.put("from_amount", 100);
        payoutData.put("wallet_address", "0xabc");
        payoutData.put("wallet_token", "USDT");
        payoutData.put("wallet_network", "ETH");

        CompletePayoutResult result = blaaiz.createCompletePayout(
                new CompletePayoutConfig().customerData(customerData).payoutData(payoutData));

        assertEquals("cust-123", result.getCustomerId());
        // getFees()/getPayout() carry the full parsed response body (BlaaizResponse#getData()),
        // matching the source SDKs' un-unwrapped `feeBreakdown.data` / `payoutResult.data`.
        assertEquals(Map.of("data", Map.of("fee", 5)), result.getFees());
        assertEquals(Map.of("data", Map.of("id", "payout-1")), result.getPayout());

        // Request #1: customer creation.
        assertTrue(captor.getAllValues().get(0).url().encodedPath().endsWith("/api/external/customer"));

        // Request #2: fee breakdown must carry from_amount, never to_amount.
        Map<?, ?> feeBody = MAPPER.readValue(bodyOf(captor.getAllValues().get(1)), Map.class);
        assertEquals(100, feeBody.get("from_amount"));
        assertFalse(feeBody.containsKey("to_amount"));

        // Request #3: payout initiation includes the resolved customer_id.
        Map<?, ?> payoutBody = MAPPER.readValue(bodyOf(captor.getAllValues().get(2)), Map.class);
        assertEquals("cust-123", payoutBody.get("customer_id"));
    }

    @Test
    void createCompletePayoutSkipsCustomerCreationWhenCustomerIdPresent() throws IOException {
        Blaaiz blaaiz = newBlaaiz();

        when(call.execute()).thenReturn(
                jsonResponse(200, "{\"data\":{\"fee\":1}}"),
                jsonResponse(201, "{\"data\":{\"id\":\"payout-2\"}}"));

        Map<String, Object> payoutData = new LinkedHashMap<>();
        payoutData.put("wallet_id", "wallet-1");
        payoutData.put("customer_id", "existing-cust");
        payoutData.put("method", "crypto");
        payoutData.put("from_currency_id", "USD");
        payoutData.put("to_currency_id", "NGN");
        payoutData.put("from_amount", 50);
        payoutData.put("wallet_address", "0xabc");
        payoutData.put("wallet_token", "USDT");
        payoutData.put("wallet_network", "ETH");

        CompletePayoutResult result = blaaiz.createCompletePayout(
                new CompletePayoutConfig().payoutData(payoutData));

        assertEquals("existing-cust", result.getCustomerId());
        org.mockito.Mockito.verify(okHttpClient, org.mockito.Mockito.times(2)).newCall(any());
    }

    @Test
    void createCompletePayoutWrapsBlaaizExceptionPreservingStatusAndCode() throws IOException {
        Blaaiz blaaiz = newBlaaiz();

        when(call.execute()).thenReturn(jsonResponse(422, "{\"message\":\"bad fee request\",\"code\":\"FEE_ERR\"}"));

        Map<String, Object> payoutData = new LinkedHashMap<>();
        payoutData.put("wallet_id", "wallet-1");
        payoutData.put("customer_id", "cust-1");
        payoutData.put("method", "crypto");
        payoutData.put("from_currency_id", "USD");
        payoutData.put("to_currency_id", "NGN");
        payoutData.put("from_amount", 50);
        payoutData.put("wallet_address", "0xabc");
        payoutData.put("wallet_token", "USDT");
        payoutData.put("wallet_network", "ETH");

        BlaaizException e = assertThrows(BlaaizException.class, () -> blaaiz.createCompletePayout(
                new CompletePayoutConfig().payoutData(payoutData)));

        assertEquals("Complete payout failed: bad fee request", e.getMessage());
        assertEquals(422, e.getStatus());
        assertEquals("FEE_ERR", e.getErrorCode());
    }

    @Test
    void createCompletePayoutWrapsValidationFailureWithoutStatusOrCode() {
        Blaaiz blaaiz = newBlaaiz();

        // Missing all required payout fields triggers PayoutService's own IllegalArgumentException,
        // which createCompletePayout must still wrap into a BlaaizException with the failure prefix.
        Map<String, Object> payoutData = new LinkedHashMap<>();

        BlaaizException e = assertThrows(BlaaizException.class, () -> blaaiz.createCompletePayout(
                new CompletePayoutConfig().payoutData(payoutData)));

        assertTrue(e.getMessage().startsWith("Complete payout failed: "));
        assertNull(e.getStatus());
        assertNull(e.getErrorCode());
    }

    // ---- createCompleteCollection ----

    @Test
    void createCompleteCollectionRequiresCollectionData() {
        Blaaiz blaaiz = newBlaaiz();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> blaaiz.createCompleteCollection(new CompleteCollectionConfig()));
        assertEquals("collectionData is required", e.getMessage());
    }

    @Test
    void createCompleteCollectionCreatesCustomerAndVbaBeforeCollection() throws IOException {
        Blaaiz blaaiz = newBlaaiz();

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(captor.capture())).thenReturn(call);
        when(call.execute()).thenReturn(
                jsonResponse(201, "{\"data\":{\"id\":\"cust-9\"}}"),
                jsonResponse(201, "{\"data\":{\"id\":\"vba-1\",\"account_number\":\"0001\"}}"),
                jsonResponse(201, "{\"data\":{\"id\":\"coll-1\"}}"));

        Map<String, Object> customerData = new LinkedHashMap<>();
        customerData.put("type", "individual");
        customerData.put("email", "a@b.com");
        customerData.put("country", "NG");
        customerData.put("id_type", "passport");
        customerData.put("id_number", "123");
        customerData.put("first_name", "Ada");
        customerData.put("last_name", "Lovelace");

        Map<String, Object> collectionData = new LinkedHashMap<>();
        collectionData.put("wallet_id", "wallet-1");
        collectionData.put("amount", 100);
        collectionData.put("currency", "NGN");
        collectionData.put("method", "bank_transfer");

        CompleteCollectionResult result = blaaiz.createCompleteCollection(new CompleteCollectionConfig()
                .customerData(customerData)
                .collectionData(collectionData)
                .createVba(true));

        assertEquals("cust-9", result.getCustomerId());
        assertEquals(Map.of("data", Map.of("id", "coll-1")), result.getCollection());
        assertEquals(Map.of("data", Map.of("id", "vba-1", "account_number", "0001")), result.getVirtualAccount());

        Map<?, ?> vbaBody = MAPPER.readValue(bodyOf(captor.getAllValues().get(1)), Map.class);
        assertEquals("wallet-1", vbaBody.get("wallet_id"));
        assertEquals("Ada Lovelace", vbaBody.get("account_name"));

        Map<?, ?> collectionBody = MAPPER.readValue(bodyOf(captor.getAllValues().get(2)), Map.class);
        assertEquals("cust-9", collectionBody.get("customer_id"));
    }

    @Test
    void createCompleteCollectionSkipsVbaWhenNotRequested() throws IOException {
        Blaaiz blaaiz = newBlaaiz();

        when(call.execute()).thenReturn(jsonResponse(201, "{\"data\":{\"id\":\"coll-2\"}}"));

        Map<String, Object> collectionData = new LinkedHashMap<>();
        collectionData.put("customer_id", "cust-1");
        collectionData.put("wallet_id", "wallet-1");
        collectionData.put("amount", 100);
        collectionData.put("currency", "NGN");
        collectionData.put("method", "bank_transfer");

        CompleteCollectionResult result = blaaiz.createCompleteCollection(
                new CompleteCollectionConfig().collectionData(collectionData));

        assertNull(result.getVirtualAccount());
        org.mockito.Mockito.verify(okHttpClient, org.mockito.Mockito.times(1)).newCall(any());
    }

    @Test
    void createCompleteCollectionUsesFallbackAccountNameWithoutCustomerData() throws IOException {
        Blaaiz blaaiz = newBlaaiz();

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(captor.capture())).thenReturn(call);
        when(call.execute()).thenReturn(
                jsonResponse(201, "{\"data\":{\"id\":\"vba-2\"}}"),
                jsonResponse(201, "{\"data\":{\"id\":\"coll-3\"}}"));

        Map<String, Object> collectionData = new LinkedHashMap<>();
        collectionData.put("customer_id", "cust-1");
        collectionData.put("wallet_id", "wallet-1");
        collectionData.put("amount", 100);
        collectionData.put("currency", "NGN");
        collectionData.put("method", "bank_transfer");

        blaaiz.createCompleteCollection(new CompleteCollectionConfig()
                .collectionData(collectionData)
                .createVba(true));

        Map<?, ?> vbaBody = MAPPER.readValue(bodyOf(captor.getAllValues().get(0)), Map.class);
        assertEquals("Customer Account", vbaBody.get("account_name"));
    }

    // ---- convenience delegations ----

    @Test
    void getCustomerByIdDelegatesToCustomersGet() throws IOException {
        Blaaiz blaaiz = newBlaaiz();
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(captor.capture())).thenReturn(call);
        when(call.execute()).thenReturn(jsonResponse(200, "{\"data\":{\"id\":\"cust-1\"}}"));

        blaaiz.getCustomerById("cust-1");

        assertTrue(captor.getValue().url().encodedPath().endsWith("/api/external/customer/cust-1"));
        assertEquals("GET", captor.getValue().method());
    }

    @Test
    void getTransactionByIdDelegatesToTransactionsGet() throws IOException {
        Blaaiz blaaiz = newBlaaiz();
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(captor.capture())).thenReturn(call);
        when(call.execute()).thenReturn(jsonResponse(200, "{\"data\":{}}"));

        blaaiz.getTransactionById("txn-1");

        assertTrue(captor.getValue().url().encodedPath().endsWith("/api/external/transaction/txn-1"));
    }

    @Test
    void getWalletByIdDelegatesToWalletsGet() throws IOException {
        Blaaiz blaaiz = newBlaaiz();
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(captor.capture())).thenReturn(call);
        when(call.execute()).thenReturn(jsonResponse(200, "{\"data\":{}}"));

        blaaiz.getWalletById("wallet-1");

        assertTrue(captor.getValue().url().encodedPath().endsWith("/api/external/wallet/wallet-1"));
    }

    @Test
    void getAllCurrenciesDelegatesToCurrenciesList() throws IOException {
        Blaaiz blaaiz = newBlaaiz();
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(captor.capture())).thenReturn(call);
        when(call.execute()).thenReturn(jsonResponse(200, "{\"data\":[]}"));

        blaaiz.getAllCurrencies();

        assertTrue(captor.getValue().url().encodedPath().endsWith("/api/external/currency"));
    }

    @Test
    void getAllBanksDelegatesToBanksList() throws IOException {
        Blaaiz blaaiz = newBlaaiz();
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(captor.capture())).thenReturn(call);
        when(call.execute()).thenReturn(jsonResponse(200, "{\"data\":[]}"));

        blaaiz.getAllBanks();

        assertTrue(captor.getValue().url().encodedPath().endsWith("/api/external/bank"));
    }

    @Test
    void calculateFeesDelegatesToFeesGetBreakdownWithFromAmount() throws IOException {
        Blaaiz blaaiz = newBlaaiz();
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(okHttpClient.newCall(captor.capture())).thenReturn(call);
        when(call.execute()).thenReturn(jsonResponse(200, "{\"data\":{}}"));

        blaaiz.calculateFees("USD", "NGN", 250);

        Map<?, ?> body = MAPPER.readValue(bodyOf(captor.getValue()), Map.class);
        assertEquals("USD", body.get("from_currency_id"));
        assertEquals("NGN", body.get("to_currency_id"));
        assertEquals(250, body.get("from_amount"));
    }
}
