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
class SignaIdServiceTest {

    private static final String BASE_PATH = "/api/external/signa-id";
    private static final String WALLET_PATH = "/api/v1/signa-id/public/wallets/0xabc/status";

    @Mock
    private BlaaizClient client;

    private SignaIdService signaId;

    @BeforeEach
    void setUp() {
        signaId = new SignaIdService(client);
    }

    private static Map<String, Object> validReleaseRequest() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("idempotency_key", "release-123");
        data.put("purpose", "Open your trading account");
        data.put("scopes", List.of("identity", "id_document", "document_images"));
        data.put("origin", "https://yourapp.com");
        data.put("reference", "user_10482");
        return data;
    }

    private static void assertInvalid(String message, org.junit.jupiter.api.function.Executable executable) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(message, e.getMessage());
    }

    @Test
    void createReleaseRequestPostsTheDataUnchanged() {
        Map<String, Object> data = validReleaseRequest();
        BlaaizResponse response = new BlaaizResponse(Map.of("data", Map.of("id", "release-1")), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/release-requests"), eq(data), isNull()))
                .thenReturn(response);

        assertEquals(response, signaId.createReleaseRequest(data));
        verify(client).makeRequest("POST", BASE_PATH + "/release-requests", data, null);
    }

    @Test
    void exchangeReleaseCodePostsTheCode() {
        String code = "a".repeat(43);

        signaId.exchangeReleaseCode(code);

        verify(client).makeRequest("POST", BASE_PATH + "/releases/exchange", Map.of("code", code), null);
    }

    @Test
    void getReleaseAndGetReleaseDocumentEncodeBothIds() {
        signaId.getRelease("release/1");
        signaId.getReleaseDocument("release/1", "doc/1");

        verify(client).makeRequest("GET", BASE_PATH + "/releases/release%2F1", null, null);
        verify(client).makeRequest("GET", BASE_PATH + "/releases/release%2F1/documents/doc%2F1", null, null);
    }

    @Test
    void getWalletStatusAppendsChainIdOnlyWhenGiven() {
        signaId.getWalletStatus("0xabc");
        signaId.getWalletStatus("0xabc", null);
        signaId.getWalletStatus("0xabc", 8453);

        verify(client, org.mockito.Mockito.times(2)).makeRequest("GET", WALLET_PATH, null, null);
        verify(client).makeRequest("GET", WALLET_PATH + "?chain_id=8453", null, null);
    }

    @Test
    void createReleaseRequestValidatesTheShapeWithoutAnHttpCall() {
        assertInvalid("Release request data is required", () -> signaId.createReleaseRequest(null));

        for (String field : new String[] {"idempotency_key", "purpose", "scopes", "origin"}) {
            Map<String, Object> data = validReleaseRequest();
            data.remove(field);
            assertInvalid(field + " is required", () -> signaId.createReleaseRequest(data));
        }

        Map<String, Object> blankOrigin = validReleaseRequest();
        blankOrigin.put("origin", "");
        assertInvalid("origin is required", () -> signaId.createReleaseRequest(blankOrigin));

        Map<String, Object> emptyScopes = validReleaseRequest();
        emptyScopes.put("scopes", List.of());
        assertInvalid("scopes must be a non-empty array", () -> signaId.createReleaseRequest(emptyScopes));

        Map<String, Object> notAList = validReleaseRequest();
        notAList.put("scopes", "identity");
        assertInvalid("scopes must be a non-empty array", () -> signaId.createReleaseRequest(notAList));

        Map<String, Object> unknownScope = validReleaseRequest();
        unknownScope.put("scopes", List.of("identity", "selfie"));
        assertInvalid("scopes must contain only: identity, id_document, address, document_images",
                () -> signaId.createReleaseRequest(unknownScope));

        Map<String, Object> wrongCase = validReleaseRequest();
        wrongCase.put("scopes", List.of("Identity"));
        assertInvalid("scopes must contain only: identity, id_document, address, document_images",
                () -> signaId.createReleaseRequest(wrongCase));

        verifyNoInteractions(client);
    }

    @Test
    void idsCodeAndAddressAreValidatedWithoutAnHttpCall() {
        assertInvalid("Release code is required", () -> signaId.exchangeReleaseCode(null));
        assertInvalid("Release code is required", () -> signaId.exchangeReleaseCode(""));
        assertInvalid("Release ID is required", () -> signaId.getRelease(null));
        assertInvalid("Release ID is required", () -> signaId.getReleaseDocument(null, "doc-1"));
        assertInvalid("Document ID is required", () -> signaId.getReleaseDocument("release-1", null));
        assertInvalid("Wallet address is required", () -> signaId.getWalletStatus(null));
        assertInvalid("Wallet address is required", () -> signaId.getWalletStatus("", 1));

        verifyNoInteractions(client);
    }
}
