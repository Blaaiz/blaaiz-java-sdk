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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignaServiceTest {

    private static final String BASE_PATH = "/api/external/compliance/kyc/sessions";

    @Mock
    private BlaaizClient client;

    private SignaService signa;

    @BeforeEach
    void setUp() {
        signa = new SignaService(client);
    }

    private static Map<String, Object> validSessionData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer_reference", "customer-123");
        data.put("idempotency_key", "request-123");
        data.put("requirements", List.of("DOCUMENTS", "SELFIE"));
        data.put("fulfilment_mode", "HOSTED");
        data.put("applicant", Map.of("first_name", "Ada", "country", "GBR"));
        return data;
    }

    private static Map<String, Object> validUploadUrlData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("file_name", "passport.jpg");
        data.put("id_doc_type", "PASSPORT");
        return data;
    }

    private static Map<String, Object> validDocumentData(String transportKey, String transportValue) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("filename", "passport.jpg");
        data.put("content_type", "image/jpeg");
        data.put("id_doc_type", "PASSPORT");
        data.put("country", "GBR");
        data.put(transportKey, transportValue);
        return data;
    }

    // ---- createSession ----

    @Test
    void createSessionSendsPostToSessionsEndpoint() {
        Map<String, Object> data = validSessionData();
        BlaaizResponse response = new BlaaizResponse(Map.of("id", "session-1"), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH), eq(data), isNull())).thenReturn(response);

        BlaaizResponse result = signa.createSession(data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", BASE_PATH, data, null);
    }

    @Test
    void createSessionAcceptsLowerCaseRequirementsUnchanged() {
        // Validation is case-insensitive, but the data forwarded to the API is not normalised --
        // the server does that.
        Map<String, Object> data = validSessionData();
        data.put("requirements", List.of("documents", "selfie"));
        BlaaizResponse response = new BlaaizResponse(Map.of(), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH), eq(data), isNull())).thenReturn(response);

        signa.createSession(data);

        verify(client).makeRequest("POST", BASE_PATH, data, null);
    }

    @Test
    void createSessionThrowsWhenSessionDataIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> signa.createSession(null));
        assertEquals("Session data is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createSessionThrowsWhenCustomerReferenceMissing() {
        Map<String, Object> data = validSessionData();
        data.remove("customer_reference");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> signa.createSession(data));
        assertEquals("customer_reference is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createSessionThrowsWhenIdempotencyKeyMissing() {
        Map<String, Object> data = validSessionData();
        data.remove("idempotency_key");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> signa.createSession(data));
        assertEquals("idempotency_key is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createSessionThrowsWhenRequirementsMissing() {
        Map<String, Object> data = validSessionData();
        data.remove("requirements");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> signa.createSession(data));
        assertEquals("requirements is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createSessionThrowsWhenRequirementsEmpty() {
        Map<String, Object> data = validSessionData();
        data.put("requirements", List.of());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> signa.createSession(data));
        assertEquals("requirements must be a non-empty array", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createSessionThrowsWhenRequirementsNotAList() {
        Map<String, Object> data = validSessionData();
        data.put("requirements", "DOCUMENTS");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> signa.createSession(data));
        assertEquals("requirements must be a non-empty array", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createSessionThrowsWhenRequirementUnknown() {
        Map<String, Object> data = validSessionData();
        data.put("requirements", List.of("UNKNOWN"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> signa.createSession(data));
        assertEquals("requirements must contain only: DOCUMENTS, SELFIE, FACE_MATCH, PROOF_OF_ADDRESS", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createSessionThrowsWhenRequirementNotAString() {
        Map<String, Object> data = validSessionData();
        data.put("requirements", List.of(42));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> signa.createSession(data));
        assertEquals("requirements must contain only: DOCUMENTS, SELFIE, FACE_MATCH, PROOF_OF_ADDRESS", e.getMessage());
        verifyNoInteractions(client);
    }

    // ---- listSessions ----

    @Test
    void listSessionsSendsLimitAndOffsetAsQueryParams() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("limit", 25);
        filters.put("offset", 50);
        BlaaizResponse response = new BlaaizResponse(Map.of("sessions", List.of()), 200, null);
        when(client.makeRequest(eq("GET"), eq(BASE_PATH), eq(filters), isNull())).thenReturn(response);

        BlaaizResponse result = signa.listSessions(filters);

        assertEquals(response, result);
        verify(client).makeRequest("GET", BASE_PATH, filters, null);
    }

    @Test
    void listSessionsSendsNoQueryStringWithNoFilters() {
        BlaaizResponse response = new BlaaizResponse(Map.of("sessions", List.of()), 200, null);
        when(client.makeRequest(eq("GET"), eq(BASE_PATH), isNull(), isNull())).thenReturn(response);

        signa.listSessions();
        signa.listSessions(null);
        signa.listSessions(new LinkedHashMap<>());

        verify(client, org.mockito.Mockito.times(3)).makeRequest("GET", BASE_PATH, null, null);
    }

    // ---- get / submit / cancel (id encoding) ----

    @Test
    void getSubmitAndCancelEncodeTheSessionIdPathSegment() {
        BlaaizResponse response = new BlaaizResponse(Map.of("id", "session/123"), 200, null);
        when(client.makeRequest(eq("GET"), eq(BASE_PATH + "/session%2F123"), isNull(), isNull())).thenReturn(response);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session%2F123/submit"), isNull(), isNull())).thenReturn(response);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session%2F123/cancel"), isNull(), isNull())).thenReturn(response);

        signa.getSession("session/123");
        signa.submitSession("session/123");
        signa.cancelSession("session/123");

        verify(client).makeRequest("GET", BASE_PATH + "/session%2F123", null, null);
        verify(client).makeRequest("POST", BASE_PATH + "/session%2F123/submit", null, null);
        verify(client).makeRequest("POST", BASE_PATH + "/session%2F123/cancel", null, null);
    }

    @Test
    void encodesSpacesAsPercentTwentyNotPlus() {
        BlaaizResponse response = new BlaaizResponse(Map.of(), 200, null);
        when(client.makeRequest(eq("GET"), eq(BASE_PATH + "/session%20123"), isNull(), isNull())).thenReturn(response);

        signa.getSession("session 123");

        verify(client).makeRequest("GET", BASE_PATH + "/session%20123", null, null);
    }

    // ---- session id validation ----

    @Test
    void everyIdTakingMethodRequiresANonBlankSessionId() {
        for (String sessionId : new String[] {null, ""}) {
            assertSessionIdRequired(() -> signa.getSession(sessionId));
            assertSessionIdRequired(() -> signa.submitSession(sessionId));
            assertSessionIdRequired(() -> signa.cancelSession(sessionId));
            assertSessionIdRequired(() -> signa.createDocumentUploadUrl(sessionId, validUploadUrlData()));
            assertSessionIdRequired(() -> signa.uploadSessionDocument(sessionId, validDocumentData("content_base64", "aGVsbG8=")));
            assertSessionIdRequired(() -> signa.issueVerificationLink(sessionId));
        }
        verifyNoInteractions(client);
    }

    private static void assertSessionIdRequired(org.junit.jupiter.api.function.Executable executable) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Session ID is required", e.getMessage());
    }

    // ---- createDocumentUploadUrl ----

    @Test
    void createDocumentUploadUrlSendsPostToUploadUrlEndpoint() {
        Map<String, Object> data = validUploadUrlData();
        BlaaizResponse response = new BlaaizResponse(Map.of("url", "https://s3.example/x"), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/documents/upload-url"), eq(data), isNull()))
                .thenReturn(response);

        BlaaizResponse result = signa.createDocumentUploadUrl("session-123", data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", BASE_PATH + "/session-123/documents/upload-url", data, null);
    }

    @Test
    void createDocumentUploadUrlThrowsWhenDataIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.createDocumentUploadUrl("session-123", null));
        assertEquals("Document data is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createDocumentUploadUrlThrowsWhenFileNameMissing() {
        Map<String, Object> data = validUploadUrlData();
        data.remove("file_name");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.createDocumentUploadUrl("session-123", data));
        assertEquals("file_name is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createDocumentUploadUrlThrowsWhenIdDocTypeInvalid() {
        Map<String, Object> data = validUploadUrlData();
        data.put("id_doc_type", "UNKNOWN");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.createDocumentUploadUrl("session-123", data));
        assertEquals("id_doc_type must be one of: PASSPORT, ID_CARD, DRIVERS, RESIDENCE_PERMIT, "
                + "UTILITY_BILL, BANK_STATEMENT, SELFIE", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createDocumentUploadUrlThrowsWhenIdDocTypeMissing() {
        Map<String, Object> data = validUploadUrlData();
        data.remove("id_doc_type");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.createDocumentUploadUrl("session-123", data));
        assertEquals("id_doc_type is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createDocumentUploadUrlThrowsWhenIdDocTypeNotAString() {
        Map<String, Object> data = validUploadUrlData();
        data.put("id_doc_type", 42);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.createDocumentUploadUrl("session-123", data));
        assertEquals("id_doc_type must be one of: PASSPORT, ID_CARD, DRIVERS, RESIDENCE_PERMIT, "
                + "UTILITY_BILL, BANK_STATEMENT, SELFIE", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createDocumentUploadUrlAcceptsLowerCaseIdDocTypeUnchanged() {
        Map<String, Object> data = validUploadUrlData();
        data.put("id_doc_type", "passport");
        BlaaizResponse response = new BlaaizResponse(Map.of(), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/documents/upload-url"), eq(data), isNull()))
                .thenReturn(response);

        signa.createDocumentUploadUrl("session-123", data);

        verify(client).makeRequest("POST", BASE_PATH + "/session-123/documents/upload-url", data, null);
    }

    // ---- uploadSessionDocument ----

    @Test
    void uploadSessionDocumentSendsInlineContentBase64() {
        Map<String, Object> data = validDocumentData("content_base64", "aGVsbG8=");
        BlaaizResponse response = new BlaaizResponse(Map.of("id", "session-123"), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/documents"), eq(data), isNull()))
                .thenReturn(response);

        BlaaizResponse result = signa.uploadSessionDocument("session-123", data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", BASE_PATH + "/session-123/documents", data, null);
    }

    @Test
    void uploadSessionDocumentSendsStagedFileNameViaUploadDocumentAlias() {
        Map<String, Object> data = validDocumentData("file_name", "a1b2c3_passport.jpg");
        BlaaizResponse response = new BlaaizResponse(Map.of("id", "session-123"), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/documents"), eq(data), isNull()))
                .thenReturn(response);

        BlaaizResponse result = signa.uploadDocument("session-123", data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", BASE_PATH + "/session-123/documents", data, null);
    }

    @Test
    void uploadSessionDocumentThrowsWhenDataIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.uploadSessionDocument("session-123", null));
        assertEquals("Document data is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void uploadSessionDocumentThrowsWhenRequiredFieldMissing() {
        for (String field : new String[] {"filename", "content_type", "id_doc_type", "country"}) {
            Map<String, Object> data = validDocumentData("content_base64", "aGVsbG8=");
            data.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> signa.uploadSessionDocument("session-123", data));
            assertEquals(field + " is required", e.getMessage());
        }
        verifyNoInteractions(client);
    }

    @Test
    void uploadSessionDocumentThrowsWhenContentTypeInvalid() {
        Map<String, Object> data = validDocumentData("content_base64", "aGVsbG8=");
        data.put("content_type", "application/zip");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.uploadSessionDocument("session-123", data));
        assertEquals("content_type must be one of: image/jpeg, image/png, image/webp, application/pdf", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void uploadSessionDocumentAcceptsUpperCaseContentTypeUnchanged() {
        Map<String, Object> data = validDocumentData("content_base64", "aGVsbG8=");
        data.put("content_type", "IMAGE/JPEG");
        BlaaizResponse response = new BlaaizResponse(Map.of(), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/documents"), eq(data), isNull()))
                .thenReturn(response);

        signa.uploadSessionDocument("session-123", data);

        verify(client).makeRequest("POST", BASE_PATH + "/session-123/documents", data, null);
    }

    @Test
    void uploadSessionDocumentThrowsWhenIdDocTypeInvalid() {
        Map<String, Object> data = validDocumentData("content_base64", "aGVsbG8=");
        data.put("id_doc_type", "UNKNOWN");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.uploadSessionDocument("session-123", data));
        assertEquals("id_doc_type must be one of: PASSPORT, ID_CARD, DRIVERS, RESIDENCE_PERMIT, "
                + "UTILITY_BILL, BANK_STATEMENT, SELFIE", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void uploadSessionDocumentAcceptsLowerCaseIdDocTypeUnchanged() {
        Map<String, Object> data = validDocumentData("content_base64", "aGVsbG8=");
        data.put("id_doc_type", "passport");
        BlaaizResponse response = new BlaaizResponse(Map.of(), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/documents"), eq(data), isNull()))
                .thenReturn(response);

        signa.uploadSessionDocument("session-123", data);

        verify(client).makeRequest("POST", BASE_PATH + "/session-123/documents", data, null);
    }

    @Test
    void uploadSessionDocumentTreatsAnEmptyTransportAsAbsent() {
        Map<String, Object> data = validDocumentData("content_base64", "aGVsbG8=");
        data.put("file_name", "");
        BlaaizResponse response = new BlaaizResponse(Map.of(), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/documents"), eq(data), isNull()))
                .thenReturn(response);

        signa.uploadSessionDocument("session-123", data);

        verify(client).makeRequest("POST", BASE_PATH + "/session-123/documents", data, null);
    }

    @Test
    void uploadSessionDocumentThrowsWhenNeitherTransportProvided() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("filename", "passport.jpg");
        data.put("content_type", "image/jpeg");
        data.put("id_doc_type", "PASSPORT");
        data.put("country", "GBR");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.uploadSessionDocument("session-123", data));
        assertEquals("Provide exactly one of file_name or content_base64", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void uploadSessionDocumentThrowsWhenBothTransportsProvided() {
        Map<String, Object> data = validDocumentData("content_base64", "aGVsbG8=");
        data.put("file_name", "staged.jpg");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> signa.uploadSessionDocument("session-123", data));
        assertEquals("Provide exactly one of file_name or content_base64", e.getMessage());
        verifyNoInteractions(client);
    }

    // ---- issueVerificationLink ----

    @Test
    void issueVerificationLinkSendsBarePostWithNoBody() {
        BlaaizResponse response = new BlaaizResponse(Map.of("verification_link", "https://example.com/v"), 200, null);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/verification-link"), isNull(), isNull()))
                .thenReturn(response);

        BlaaizResponse result = signa.issueVerificationLink("session-123");

        assertEquals(response, result);
        verify(client).makeRequest("POST", BASE_PATH + "/session-123/verification-link", null, null);
    }

    // ---- aliases delegate ----

    @Test
    void aliasesDelegateToTheirPrimaryMethods() {
        Map<String, Object> sessionData = validSessionData();
        Map<String, Object> filters = Map.of("limit", 10);
        BlaaizResponse response = new BlaaizResponse(Map.of(), 200, null);

        when(client.makeRequest(eq("POST"), eq(BASE_PATH), eq(sessionData), isNull())).thenReturn(response);
        when(client.makeRequest(eq("GET"), eq(BASE_PATH), eq(filters), isNull())).thenReturn(response);
        when(client.makeRequest(eq("GET"), eq(BASE_PATH + "/session-123"), isNull(), isNull())).thenReturn(response);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/submit"), isNull(), isNull())).thenReturn(response);
        when(client.makeRequest(eq("POST"), eq(BASE_PATH + "/session-123/cancel"), isNull(), isNull())).thenReturn(response);

        assertEquals(response, signa.create(sessionData));
        assertEquals(response, signa.list(filters));
        assertEquals(response, signa.get("session-123"));
        assertEquals(response, signa.submit("session-123"));
        assertEquals(response, signa.cancel("session-123"));

        verify(client).makeRequest("POST", BASE_PATH, sessionData, null);
        verify(client).makeRequest("GET", BASE_PATH, filters, null);
        verify(client).makeRequest("GET", BASE_PATH + "/session-123", null, null);
        verify(client).makeRequest("POST", BASE_PATH + "/session-123/submit", null, null);
        verify(client).makeRequest("POST", BASE_PATH + "/session-123/cancel", null, null);
        verifyNoMoreInteractions(client);
    }

    @Test
    void listAliasWithNoArgsSendsNoQueryString() {
        BlaaizResponse response = new BlaaizResponse(Map.of("sessions", List.of()), 200, null);
        when(client.makeRequest(eq("GET"), eq(BASE_PATH), isNull(), isNull())).thenReturn(response);

        BlaaizResponse result = signa.list();

        assertEquals(response, result);
        verify(client).makeRequest("GET", BASE_PATH, null, null);
    }

    // ---- error propagation ----

    @Test
    void getSessionPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(eq("GET"), eq(BASE_PATH + "/session-x"), isNull(), isNull()))
                .thenThrow(new BlaaizException("Session not found", 404, "NOT_FOUND"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> signa.getSession("session-x"));
        assertEquals("Session not found", e.getMessage());
        assertEquals(404, e.getStatus());
        assertEquals("NOT_FOUND", e.getErrorCode());
    }
}
