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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private BlaaizClient client;

    private PayoutService payouts;

    @BeforeEach
    void setUp() {
        payouts = new PayoutService(client);
    }

    private static Map<String, Object> baseFields() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("wallet_id", "wallet_1");
        data.put("customer_id", "cust_1");
        data.put("from_currency_id", "1");
        data.put("to_currency_id", "NGN");
        data.put("from_amount", 100);
        return data;
    }

    private static Map<String, Object> ngnBankTransfer() {
        Map<String, Object> data = baseFields();
        data.put("method", "bank_transfer");
        data.put("bank_id", "bank_1");
        data.put("account_number", "0123456789");
        return data;
    }

    private void stubOk() {
        when(client.makeRequest(eq("POST"), eq("/api/external/payout"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("id", "payout_1"), 200, null));
    }

    // ---- happy path ----

    @Test
    void initiateSendsWholePayloadVerbatimToPayoutEndpoint() {
        stubOk();
        Map<String, Object> data = ngnBankTransfer();

        BlaaizResponse result = payouts.initiate(data);

        assertEquals(200, result.getStatus());
        verify(client).makeRequest("POST", "/api/external/payout", data, null);
    }

    @Test
    void initiateAcceptsToAmountInPlaceOfFromAmount() {
        stubOk();
        Map<String, Object> data = ngnBankTransfer();
        data.remove("from_amount");
        data.put("to_amount", 50);

        payouts.initiate(data);

        verify(client).makeRequest("POST", "/api/external/payout", data, null);
    }

    @Test
    void initiateForwardsOptionalNoteVerbatim() {
        stubOk();
        Map<String, Object> data = ngnBankTransfer();
        data.put("note", "Invoice #123");

        payouts.initiate(data);

        // note is unvalidated -- forwarded exactly as supplied, alongside every other field.
        verify(client).makeRequest("POST", "/api/external/payout", data, null);
    }

    @Test
    void initiateWithoutNoteStillSucceeds() {
        // Confirms the SDK does not inject a default -- the API itself falls back to the
        // business name when `note` is omitted.
        stubOk();
        Map<String, Object> data = ngnBankTransfer();

        payouts.initiate(data);

        verify(client).makeRequest("POST", "/api/external/payout", data, null);
    }

    // ---- base required fields ----

    @Test
    void initiateThrowsWhenBaseRequiredFieldMissing() {
        for (String field : new String[] {"wallet_id", "customer_id", "method", "from_currency_id", "to_currency_id"}) {
            Map<String, Object> data = ngnBankTransfer();
            data.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(data));
            assertEquals(field + " is required", e.getMessage());
        }
        verifyNoInteractions(client);
    }

    @Test
    void initiateThrowsWhenNeitherFromAmountNorToAmountProvided() {
        Map<String, Object> data = ngnBankTransfer();
        data.remove("from_amount");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(data));
        assertEquals("Either from_amount or to_amount is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void initiateTreatsZeroAmountAsMissing() {
        Map<String, Object> data = ngnBankTransfer();
        data.put("from_amount", 0);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(data));
        assertEquals("Either from_amount or to_amount is required", e.getMessage());
    }

    // ---- bank_transfer: NGN ----

    @Test
    void bankTransferNgnRequiresBankIdAndAccountNumber() {
        for (String field : new String[] {"bank_id", "account_number"}) {
            Map<String, Object> data = ngnBankTransfer();
            data.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(data));
            assertEquals(field + " is required", e.getMessage());
        }
    }

    // ---- bank_transfer: GBP ----

    @Test
    void bankTransferGbpRequiresSortCodeAccountNumberAndAccountName() {
        Map<String, Object> data = baseFields();
        data.put("to_currency_id", "GBP");
        data.put("method", "bank_transfer");
        data.put("sort_code", "12-34-56");
        data.put("account_number", "12345678");
        data.put("account_name", "Jane Doe");
        stubOk();

        payouts.initiate(data);
        verify(client).makeRequest("POST", "/api/external/payout", data, null);

        for (String field : new String[] {"sort_code", "account_number", "account_name"}) {
            Map<String, Object> missing = new LinkedHashMap<>(data);
            missing.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(missing));
            assertEquals(field + " is required", e.getMessage());
        }
    }

    // ---- bank_transfer: EUR ----

    @Test
    void bankTransferEurRequiresIbanBicCodeAndAccountName() {
        Map<String, Object> data = baseFields();
        data.put("to_currency_id", "EUR");
        data.put("method", "bank_transfer");
        data.put("iban", "DE89370400440532013000");
        data.put("bic_code", "COBADEFFXXX");
        data.put("account_name", "Jane Doe");
        stubOk();

        payouts.initiate(data);
        verify(client).makeRequest("POST", "/api/external/payout", data, null);

        for (String field : new String[] {"iban", "bic_code", "account_name"}) {
            Map<String, Object> missing = new LinkedHashMap<>(data);
            missing.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(missing));
            assertEquals(field + " is required", e.getMessage());
        }
    }

    @Test
    void bankTransferWithUnrecognizedCurrencySkipsCurrencySpecificValidation() {
        // Only NGN/GBP/EUR have method-conditional extras; any other to_currency_id for
        // bank_transfer forwards through with only the base fields validated.
        stubOk();
        Map<String, Object> data = baseFields();
        data.put("to_currency_id", "USD");
        data.put("method", "bank_transfer");

        payouts.initiate(data);

        verify(client).makeRequest("POST", "/api/external/payout", data, null);
    }

    // ---- interac ----

    @Test
    void interacRequiresEmailAndNames() {
        Map<String, Object> data = baseFields();
        data.put("method", "interac");
        data.put("email", "jane@example.com");
        data.put("interac_first_name", "Jane");
        data.put("interac_last_name", "Doe");
        stubOk();

        payouts.initiate(data);
        verify(client).makeRequest("POST", "/api/external/payout", data, null);

        for (String field : new String[] {"email", "interac_first_name", "interac_last_name"}) {
            Map<String, Object> missing = new LinkedHashMap<>(data);
            missing.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(missing));
            assertEquals(field + " is required", e.getMessage());
        }
    }

    // ---- ach / wire ----

    private static Map<String, Object> achFields() {
        Map<String, Object> data = baseFields();
        data.put("method", "ach");
        data.put("type", "checking");
        data.put("account_number", "12345678");
        data.put("account_name", "Jane Doe");
        data.put("account_type", "individual");
        data.put("bank_name", "Chase");
        data.put("routing_number", "021000021");
        return data;
    }

    @Test
    void achRequiresBaseBankAccountFields() {
        stubOk();
        Map<String, Object> data = achFields();

        payouts.initiate(data);
        verify(client).makeRequest("POST", "/api/external/payout", data, null);

        for (String field : new String[] {"type", "account_number", "account_name", "account_type", "bank_name", "routing_number"}) {
            Map<String, Object> missing = new LinkedHashMap<>(data);
            missing.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(missing));
            assertEquals(field + " is required", e.getMessage());
        }
    }

    @Test
    void achDoesNotRequireSwiftCode() {
        stubOk();
        Map<String, Object> data = achFields();

        payouts.initiate(data);

        verify(client).makeRequest("POST", "/api/external/payout", data, null);
    }

    @Test
    void wireRequiresSameFieldsAsAchPlusSwiftCode() {
        Map<String, Object> data = achFields();
        data.put("method", "wire");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(data));
        assertEquals("swift_code is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void wireSucceedsWhenSwiftCodeProvided() {
        stubOk();
        Map<String, Object> data = achFields();
        data.put("method", "wire");
        data.put("swift_code", "CHASUS33");

        payouts.initiate(data);

        verify(client).makeRequest("POST", "/api/external/payout", data, null);
    }

    // ---- crypto ----

    @Test
    void cryptoRequiresWalletAddressTokenAndNetwork() {
        Map<String, Object> data = baseFields();
        data.put("method", "crypto");
        data.put("wallet_address", "0xabc123");
        data.put("wallet_token", "USDT");
        data.put("wallet_network", "TRC20");
        stubOk();

        payouts.initiate(data);
        verify(client).makeRequest("POST", "/api/external/payout", data, null);

        for (String field : new String[] {"wallet_address", "wallet_token", "wallet_network"}) {
            Map<String, Object> missing = new LinkedHashMap<>(data);
            missing.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> payouts.initiate(missing));
            assertEquals(field + " is required", e.getMessage());
        }
    }

    // ---- unrecognized method ----

    @Test
    void unrecognizedMethodSkipsMethodConditionalValidation() {
        // Only bank_transfer/interac/ach/wire/crypto have method-conditional extras; any other
        // method value forwards through with just the base fields validated, matching the
        // fall-through (no `else` branch) behavior in all three source SDKs.
        stubOk();
        Map<String, Object> data = baseFields();
        data.put("method", "mobile_money");

        payouts.initiate(data);

        verify(client).makeRequest("POST", "/api/external/payout", data, null);
    }

    // ---- transport failure propagation ----

    @Test
    void initiatePropagatesBlaaizExceptionFromClient() {
        Map<String, Object> data = ngnBankTransfer();
        when(client.makeRequest(eq("POST"), eq("/api/external/payout"), anyMap(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 422, "VALIDATION_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> payouts.initiate(data));
        assertEquals("API request failed", e.getMessage());
        assertEquals(422, e.getStatus());
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
    }
}
