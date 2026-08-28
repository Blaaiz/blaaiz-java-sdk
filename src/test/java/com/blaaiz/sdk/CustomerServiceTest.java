package com.blaaiz.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private BlaaizClient client;

    private CustomerService customers;

    @BeforeEach
    void setUp() {
        customers = new CustomerService(client);
    }

    private static Map<String, Object> individualCustomer() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "individual");
        data.put("email", "jane@example.com");
        data.put("country", "NG");
        data.put("id_type", "PASSPORT");
        data.put("id_number", "A1234567");
        data.put("first_name", "Jane");
        data.put("last_name", "Doe");
        return data;
    }

    private static Map<String, Object> businessCustomer() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "business");
        data.put("email", "biz@example.com");
        data.put("country", "NG");
        data.put("business_name", "Acme Ltd");
        data.put("registration_number", "RC123456");
        data.put("incorporation_country", "NG");
        return data;
    }

    // ---- create ----

    @Test
    void createSendsPostToCustomerEndpoint() {
        BlaaizResponse response = new BlaaizResponse(Map.of("id", "cust_1"), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/customer"), anyMap(), isNull())).thenReturn(response);

        BlaaizResponse result = customers.create(individualCustomer());

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/customer", individualCustomer(), null);
    }

    @Test
    void createThrowsWhenRequiredFieldMissing() {
        Map<String, Object> data = individualCustomer();
        data.remove("email");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> customers.create(data));
        assertEquals("email is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void createThrowsWhenIndividualMissingFirstName() {
        Map<String, Object> data = individualCustomer();
        data.remove("first_name");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> customers.create(data));
        assertEquals("first_name is required when type is individual", e.getMessage());
    }

    @Test
    void createThrowsWhenIndividualMissingLastName() {
        Map<String, Object> data = individualCustomer();
        data.remove("last_name");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> customers.create(data));
        assertEquals("last_name is required when type is individual", e.getMessage());
    }

    @Test
    void createThrowsWhenIndividualMissingIdType() {
        Map<String, Object> data = individualCustomer();
        data.remove("id_type");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> customers.create(data));
        assertEquals("id_type is required when type is individual", e.getMessage());
    }

    @Test
    void createThrowsWhenIndividualMissingIdNumber() {
        Map<String, Object> data = individualCustomer();
        data.remove("id_number");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> customers.create(data));
        assertEquals("id_number is required when type is individual", e.getMessage());
    }

    @Test
    void createSendsPostForBusinessType() {
        when(client.makeRequest(eq("POST"), eq("/api/external/customer"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.create(businessCustomer());

        verify(client).makeRequest("POST", "/api/external/customer", businessCustomer(), null);
    }

    @Test
    void createThrowsWhenBusinessMissingBusinessName() {
        Map<String, Object> data = businessCustomer();
        data.remove("business_name");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> customers.create(data));
        assertEquals("business_name is required when type is business", e.getMessage());
    }

    @Test
    void createThrowsWhenBusinessMissingRegistrationNumber() {
        Map<String, Object> data = businessCustomer();
        data.remove("registration_number");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> customers.create(data));
        assertEquals("registration_number is required when type is business", e.getMessage());
    }

    @Test
    void createThrowsWhenBusinessMissingIncorporationCountry() {
        Map<String, Object> data = businessCustomer();
        data.remove("incorporation_country");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> customers.create(data));
        assertEquals("incorporation_country is required when type is business", e.getMessage());
    }

    @Test
    void createDoesNotRequireIdTypeOrNumberForBusinessType() {
        // Personal-ID fields are individual-only; a business payload need not carry them.
        when(client.makeRequest(eq("POST"), eq("/api/external/customer"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        Map<String, Object> data = businessCustomer();
        customers.create(data);

        verify(client).makeRequest("POST", "/api/external/customer", data, null);
    }

    // ---- list ----

    @Test
    void listForwardsFiltersAsIsToClient() {
        Map<String, Object> filters = Map.of("email", "jane@example.com", "paginate", true);
        when(client.makeRequest(eq("GET"), eq("/api/external/customer"), any(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.list(filters);

        verify(client).makeRequest("GET", "/api/external/customer", filters, null);
    }

    @Test
    void listWorksWithNullFilters() {
        when(client.makeRequest(eq("GET"), eq("/api/external/customer"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.list(null);

        verify(client).makeRequest("GET", "/api/external/customer", null, null);
    }

    // ---- get / update / addKyc / uploadFiles ----

    @Test
    void getRequiresNonBlankCustomerId() {
        assertThrows(IllegalArgumentException.class, () -> customers.get(""));
        assertThrows(IllegalArgumentException.class, () -> customers.get(null));
        verifyNoInteractions(client);
    }

    @Test
    void getSendsCustomerIdInPath() {
        when(client.makeRequest(eq("GET"), eq("/api/external/customer/cust_1"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.get("cust_1");

        verify(client).makeRequest("GET", "/api/external/customer/cust_1", null, null);
    }

    @Test
    void updateRequiresCustomerId() {
        assertThrows(IllegalArgumentException.class, () -> customers.update("", Map.of()));
        verifyNoInteractions(client);
    }

    @Test
    void updateAllowsNullUpdateData() {
        when(client.makeRequest(eq("PUT"), eq("/api/external/customer/cust_1"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.update("cust_1", null);

        verify(client).makeRequest("PUT", "/api/external/customer/cust_1", null, null);
    }

    @Test
    void addKycRequiresCustomerId() {
        assertThrows(IllegalArgumentException.class, () -> customers.addKyc(null, Map.of()));

        Map<String, Object> kyc = Map.of("document_type", "passport");
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/kyc-data"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.addKyc("cust_1", kyc);

        verify(client).makeRequest("POST", "/api/external/customer/cust_1/kyc-data", kyc, null);
    }

    @Test
    void uploadFilesPostsToFilesEndpoint() {
        Map<String, Object> files = Map.of("id_file", "file_123");
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.uploadFiles("cust_1", files);

        verify(client).makeRequest("POST", "/api/external/customer/cust_1/files", files, null);
    }

    // ---- submit / KYB scope / owners ----

    @Test
    void submitRequiresCustomerId() {
        assertThrows(IllegalArgumentException.class, () -> customers.submit(null));
        verifyNoInteractions(client);
    }

    @Test
    void submitPostsToSubmitEndpointWithNoBody() {
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/submit"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.submit("cust_1");

        verify(client).makeRequest("POST", "/api/external/customer/cust_1/submit", null, null);
    }

    @Test
    void upgradeKybScopeRequiresCustomerId() {
        assertThrows(IllegalArgumentException.class,
                () -> customers.upgradeKybScope(null, Map.of("owners", List.of(Map.of("id", "o1")))));
        verifyNoInteractions(client);
    }

    @Test
    void upgradeKybScopeThrowsWhenOwnersMissingOrEmpty() {
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> customers.upgradeKybScope("cust_1", new LinkedHashMap<>()));
        assertEquals("owners is required", e1.getMessage());

        Map<String, Object> emptyOwners = new LinkedHashMap<>();
        emptyOwners.put("owners", List.of());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> customers.upgradeKybScope("cust_1", emptyOwners));
        assertEquals("owners is required", e2.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void upgradeKybScopePostsToUpgradeEndpoint() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("owners", List.of(Map.of("first_name", "Jane", "ownership_percentage", 100)));
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/upgrade-kyb-scope"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.upgradeKybScope("cust_1", data);

        verify(client).makeRequest("POST", "/api/external/customer/cust_1/upgrade-kyb-scope", data, null);
    }

    @Test
    void deleteOwnerRequiresBothIds() {
        assertThrows(IllegalArgumentException.class, () -> customers.deleteOwner(null, "own_1"));
        assertThrows(IllegalArgumentException.class, () -> customers.deleteOwner("cust_1", null));
        verifyNoInteractions(client);
    }

    @Test
    void deleteOwnerSendsDeleteRequest() {
        when(client.makeRequest(eq("DELETE"), eq("/api/external/customer/cust_1/owner/own_1"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.deleteOwner("cust_1", "own_1");

        verify(client).makeRequest("DELETE", "/api/external/customer/cust_1/owner/own_1", null, null);
    }

    @Test
    void getOwnerFilePresignedUrlRequiresFileCategory() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> customers.getOwnerFilePresignedUrl("cust_1", "own_1", new LinkedHashMap<>()));
        assertEquals("file_category is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getOwnerFilePresignedUrlPostsToEndpoint() {
        Map<String, Object> data = Map.of("file_category", "id_document_front");
        when(client.makeRequest(eq("POST"),
                eq("/api/external/customer/cust_1/owner/own_1/file/presigned-url"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.getOwnerFilePresignedUrl("cust_1", "own_1", data);

        verify(client).makeRequest(
                "POST", "/api/external/customer/cust_1/owner/own_1/file/presigned-url", data, null);
    }

    @Test
    void uploadOwnerFilesRequiresIdDocumentFront() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> customers.uploadOwnerFiles("cust_1", "own_1", new LinkedHashMap<>()));
        assertEquals("id_document_front is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void uploadOwnerFilesPostsToEndpoint() {
        Map<String, Object> data = Map.of("id_document_front", "file_uuid_1");
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/owner/own_1/files"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.uploadOwnerFiles("cust_1", "own_1", data);

        verify(client).makeRequest("POST", "/api/external/customer/cust_1/owner/own_1/files", data, null);
    }

    // ---- documents ----

    @Test
    void listDocumentsSendsGetRequest() {
        when(client.makeRequest(eq("GET"), eq("/api/external/customer/cust_1/document"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.listDocuments("cust_1");

        verify(client).makeRequest("GET", "/api/external/customer/cust_1/document", null, null);
    }

    @Test
    void getDocumentRequiresBothIds() {
        assertThrows(IllegalArgumentException.class, () -> customers.getDocument("cust_1", null));
        assertThrows(IllegalArgumentException.class, () -> customers.getDocument(null, "doc_1"));
        verifyNoInteractions(client);
    }

    @Test
    void getDocumentSendsGetRequest() {
        when(client.makeRequest(eq("GET"), eq("/api/external/customer/cust_1/document/doc_1"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.getDocument("cust_1", "doc_1");

        verify(client).makeRequest("GET", "/api/external/customer/cust_1/document/doc_1", null, null);
    }

    @Test
    void getDocumentPresignedUrlPostsWithNoBody() {
        when(client.makeRequest(eq("POST"),
                eq("/api/external/customer/cust_1/document/presigned-url"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.getDocumentPresignedUrl("cust_1");

        verify(client).makeRequest("POST", "/api/external/customer/cust_1/document/presigned-url", null, null);
    }

    @Test
    void createDocumentThrowsWhenRequiredFieldMissing() {
        for (String field : new String[] {"type", "name", "file_id"}) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("type", "PROOF_OF_ADDRESS");
            data.put("name", "Utility bill");
            data.put("file_id", "file_uuid_1");
            data.remove(field);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> customers.createDocument("cust_1", data));
            assertEquals(field + " is required", e.getMessage());
        }
        verifyNoInteractions(client);
    }

    @Test
    void createDocumentPostsToDocumentEndpoint() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "PROOF_OF_ADDRESS");
        data.put("name", "Utility bill");
        data.put("file_id", "file_uuid_1");
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/document"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.createDocument("cust_1", data);

        verify(client).makeRequest("POST", "/api/external/customer/cust_1/document", data, null);
    }

    @Test
    void updateDocumentSendsPutRequest() {
        Map<String, Object> data = Map.of("name", "Renamed");
        when(client.makeRequest(eq("PUT"), eq("/api/external/customer/cust_1/document/doc_1"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.updateDocument("cust_1", "doc_1", data);

        verify(client).makeRequest("PUT", "/api/external/customer/cust_1/document/doc_1", data, null);
    }

    @Test
    void deleteDocumentSendsDeleteRequest() {
        when(client.makeRequest(eq("DELETE"), eq("/api/external/customer/cust_1/document/doc_1"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.deleteDocument("cust_1", "doc_1");

        verify(client).makeRequest("DELETE", "/api/external/customer/cust_1/document/doc_1", null, null);
    }

    // ---- beneficiaries ----

    @Test
    void listBeneficiariesRequiresCustomerId() {
        assertThrows(IllegalArgumentException.class, () -> customers.listBeneficiaries(null));
    }

    @Test
    void listBeneficiariesSendsGetRequest() {
        when(client.makeRequest(eq("GET"), eq("/api/external/customer/cust_1/beneficiary"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.listBeneficiaries("cust_1");

        verify(client).makeRequest("GET", "/api/external/customer/cust_1/beneficiary", null, null);
    }

    @Test
    void getBeneficiaryRequiresBothIds() {
        assertThrows(IllegalArgumentException.class, () -> customers.getBeneficiary("cust_1", null));
        assertThrows(IllegalArgumentException.class, () -> customers.getBeneficiary(null, "ben_1"));
    }

    @Test
    void getBeneficiarySendsBothIdsInPath() {
        when(client.makeRequest(eq("GET"), eq("/api/external/customer/cust_1/beneficiary/ben_1"), isNull(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        customers.getBeneficiary("cust_1", "ben_1");

        verify(client).makeRequest("GET", "/api/external/customer/cust_1/beneficiary/ben_1", null, null);
    }

    // ---- uploadFileComplete: validation ----

    @Test
    void uploadFileCompleteRequiresCustomerId() {
        assertThrows(IllegalArgumentException.class,
                () -> customers.uploadFileComplete(null, Map.of("file", "abc", "file_category", "identity")));
        verifyNoInteractions(client);
    }

    @Test
    void uploadFileCompleteRequiresFileOptions() {
        assertThrows(IllegalArgumentException.class, () -> customers.uploadFileComplete("cust_1", null));
        assertThrows(IllegalArgumentException.class, () -> customers.uploadFileComplete("cust_1", Map.of()));
    }

    @Test
    void uploadFileCompleteRequiresFile() {
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file_category", "identity");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> customers.uploadFileComplete("cust_1", opts));
        assertEquals("File is required", e.getMessage());
    }

    @Test
    void uploadFileCompleteRequiresFileCategory() {
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", "aGVsbG8=");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> customers.uploadFileComplete("cust_1", opts));
        assertEquals("file_category is required", e.getMessage());
    }

    @Test
    void uploadFileCompleteRejectsUnknownFileCategory() {
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", "aGVsbG8=");
        opts.put("file_category", "selfie");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> customers.uploadFileComplete("cust_1", opts));
        assertTrue(e.getMessage().contains("identity_back"));
        verifyNoInteractions(client);
    }

    // ---- uploadFileComplete: happy paths ----

    private void stubPresignedUrl(Object presignedData) {
        when(client.makeRequest(eq("POST"), eq("/api/external/file/get-presigned-url"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(presignedData, 200, null));
    }

    @Test
    void uploadFileCompleteHappyPathWithRawBase64AndExplicitContentType() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/presigned", "file_id", "file_1"));
        when(client.uploadFile(eq("https://s3.example.com/presigned"), any(byte[].class), eq("image/png"), isNull()))
                .thenReturn(new UploadResult(200, "\"etag-1\""));
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of("success", true), 200, null));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", Base64.getEncoder().encodeToString("hello-bytes".getBytes()));
        opts.put("file_category", "identity");
        opts.put("content_type", "image/png");

        UploadFileCompleteResult result = customers.uploadFileComplete("cust_1", opts);

        assertEquals("file_1", result.getFileId());
        assertEquals("https://s3.example.com/presigned", result.getPresignedUrl());
        assertEquals(Map.of("success", true), result.getAssociationResponse().getData());

        ArgumentCaptor<Map<String, Object>> presignedBody = ArgumentCaptor.forClass(Map.class);
        verify(client).makeRequest(eq("POST"), eq("/api/external/file/get-presigned-url"), presignedBody.capture(), isNull());
        assertEquals("cust_1", presignedBody.getValue().get("customer_id"));
        assertEquals("identity", presignedBody.getValue().get("file_category"));

        ArgumentCaptor<Map<String, Object>> associationBody = ArgumentCaptor.forClass(Map.class);
        verify(client).makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), associationBody.capture(), isNull());
        assertEquals("file_1", associationBody.getValue().get("id_file"));
    }

    @Test
    void uploadFileCompleteMapsIdentityBackToIdFileBack() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_2"));
        when(client.uploadFile(any(), any(byte[].class), any(), any())).thenReturn(new UploadResult(200, "\"etag\""));
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", Base64.getEncoder().encodeToString("hello".getBytes()));
        opts.put("file_category", "identity_back");
        opts.put("content_type", "image/png");

        customers.uploadFileComplete("cust_1", opts);

        ArgumentCaptor<Map<String, Object>> associationBody = ArgumentCaptor.forClass(Map.class);
        verify(client).makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), associationBody.capture(), isNull());
        assertEquals("file_2", associationBody.getValue().get("id_file_back"));
    }

    @Test
    void uploadFileCompleteParsesDoublyNestedPresignedResponse() {
        stubPresignedUrl(Map.of("data", Map.of("url", "https://s3.example.com/nested", "file_id", "file_3")));
        when(client.uploadFile(any(), any(byte[].class), any(), any())).thenReturn(new UploadResult(200, "\"etag\""));
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", Base64.getEncoder().encodeToString("hello".getBytes()));
        opts.put("file_category", "proof_of_address");
        opts.put("content_type", "image/png");

        UploadFileCompleteResult result = customers.uploadFileComplete("cust_1", opts);

        assertEquals("file_3", result.getFileId());
        assertEquals("https://s3.example.com/nested", result.getPresignedUrl());
    }

    @Test
    void uploadFileCompleteThrowsOnUnrecognizedPresignedResponseShape() {
        stubPresignedUrl(Map.of("unexpected", "shape"));

        BlaaizException e = assertThrows(BlaaizException.class, () ->
                customers.uploadFileComplete("cust_1", Map.of(
                        "file", Base64.getEncoder().encodeToString("hello".getBytes()),
                        "file_category", "identity",
                        "content_type", "image/png")));

        assertTrue(e.getMessage().contains("File upload failed:"));
        assertTrue(e.getMessage().contains("Invalid presigned URL response structure"));
    }

    @Test
    void uploadFileCompleteResolvesDataUrlAndDetectsContentTypeFromHeader() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_4"));
        when(client.uploadFile(eq("https://s3.example.com/p"), any(byte[].class), eq("image/jpeg"), isNull()))
                .thenReturn(new UploadResult(200, "\"etag\""));
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        String base64 = Base64.getEncoder().encodeToString("jpeg-bytes".getBytes());
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", "data:image/jpeg;base64," + base64);
        opts.put("file_category", "identity");

        customers.uploadFileComplete("cust_1", opts);

        verify(client).uploadFile(eq("https://s3.example.com/p"), any(byte[].class), eq("image/jpeg"), isNull());
    }

    @Test
    void uploadFileCompleteRejectsDataUrlWithNoBase64Payload() {
        // The data-URL parse failure happens inside the try block (after the presigned-URL
        // fetch), so per the source SDKs it surfaces wrapped as "File upload failed: ...",
        // not as a raw IllegalArgumentException -- only the upfront synchronous checks
        // (customerId/file/file_category) are exempt from wrapping.
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_11"));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", "data:image/jpeg;base64,");
        opts.put("file_category", "identity");

        BlaaizException e = assertThrows(BlaaizException.class, () -> customers.uploadFileComplete("cust_1", opts));
        assertEquals("File upload failed: Invalid data URL: no base64 data found after the comma", e.getMessage());
        verify(client, never()).uploadFile(any(), any(), any(), any());
    }

    @Test
    void uploadFileCompleteDownloadsHttpUrlAndUsesDownloadedMetadata() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_5"));
        when(client.downloadFile("https://example.com/id.png"))
                .thenReturn(new DownloadResult("png-bytes".getBytes(), "image/png", "id.png"));
        when(client.uploadFile(eq("https://s3.example.com/p"), any(byte[].class), eq("image/png"), eq("id.png")))
                .thenReturn(new UploadResult(200, "\"etag\""));
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", "https://example.com/id.png");
        opts.put("file_category", "identity");

        customers.uploadFileComplete("cust_1", opts);

        verify(client).uploadFile(eq("https://s3.example.com/p"), any(byte[].class), eq("image/png"), eq("id.png"));
    }

    @Test
    void uploadFileCompleteAutoDetectsContentTypeFromMagicBytes() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_6"));
        when(client.uploadFile(any(), any(byte[].class), eq("image/png"), any())).thenReturn(new UploadResult(200, "\"etag\""));
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        byte[] pngMagic = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A};
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", Base64.getEncoder().encodeToString(pngMagic));
        opts.put("file_category", "identity");
        // No content_type, no filename: must be detected from magic bytes.

        customers.uploadFileComplete("cust_1", opts);

        verify(client).uploadFile(eq("https://s3.example.com/p"), any(byte[].class), eq("image/png"), isNull());
    }

    @Test
    void uploadFileCompleteFallsBackToFilenameExtensionWhenMagicBytesUnrecognized() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_7"));
        when(client.uploadFile(any(), any(byte[].class), eq("application/pdf"), eq("doc.pdf")))
                .thenReturn(new UploadResult(200, "\"etag\""));
        when(client.makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), anyMap(), isNull()))
                .thenReturn(new BlaaizResponse(Map.of(), 200, null));

        Map<String, Object> opts = new LinkedHashMap<>();
        // Plain text bytes: no recognizable magic-byte signature.
        opts.put("file", Base64.getEncoder().encodeToString("not-really-a-pdf".getBytes()));
        opts.put("file_category", "identity");
        opts.put("filename", "doc.pdf");

        customers.uploadFileComplete("cust_1", opts);

        verify(client).uploadFile(eq("https://s3.example.com/p"), any(byte[].class), eq("application/pdf"), eq("doc.pdf"));
    }

    // ---- uploadFileComplete: error wrapping ----

    @Test
    void uploadFileCompleteWrapsUndetectableContentTypeAsFileUploadFailed() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_8"));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", Base64.getEncoder().encodeToString("random-bytes-no-signature".getBytes()));
        opts.put("file_category", "identity");
        // No content_type, no filename, and bytes don't match any known magic-byte signature.

        BlaaizException e = assertThrows(BlaaizException.class, () -> customers.uploadFileComplete("cust_1", opts));
        assertTrue(e.getMessage().startsWith("File upload failed:"));
        assertTrue(e.getMessage().contains("Could not determine file content type"));
        verify(client, never()).uploadFile(any(), any(), any(), any());
    }

    @Test
    void uploadFileCompleteWrapsS3FailureAsFileUploadFailed() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_9"));
        when(client.uploadFile(any(), any(byte[].class), any(), any()))
                .thenThrow(new BlaaizException("S3 upload failed: No ETag received from S3"));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", Base64.getEncoder().encodeToString("hello".getBytes()));
        opts.put("file_category", "identity");
        opts.put("content_type", "image/png");

        BlaaizException e = assertThrows(BlaaizException.class, () -> customers.uploadFileComplete("cust_1", opts));
        assertEquals("File upload failed: S3 upload failed: No ETag received from S3", e.getMessage());
        verify(client, never()).makeRequest(eq("POST"), eq("/api/external/customer/cust_1/files"), anyMap(), isNull());
    }

    @Test
    void uploadFileCompleteDoesNotDoubleWrapAlreadyWrappedFailure() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_10"));
        when(client.uploadFile(any(), any(byte[].class), any(), any()))
                .thenThrow(new BlaaizException("File upload failed: something went wrong upstream", 502, "BAD_GATEWAY"));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", Base64.getEncoder().encodeToString("hello".getBytes()));
        opts.put("file_category", "identity");
        opts.put("content_type", "image/png");

        BlaaizException e = assertThrows(BlaaizException.class, () -> customers.uploadFileComplete("cust_1", opts));
        assertEquals("File upload failed: something went wrong upstream", e.getMessage());
        assertEquals(502, e.getStatus());
        assertEquals("BAD_GATEWAY", e.getErrorCode());
    }

    @Test
    void uploadFileCompletePropagatesPresignedUrlRequestFailureWrapped() {
        when(client.makeRequest(eq("POST"), eq("/api/external/file/get-presigned-url"), anyMap(), isNull()))
                .thenThrow(new BlaaizException("API request failed", 500, null));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", Base64.getEncoder().encodeToString("hello".getBytes()));
        opts.put("file_category", "identity");
        opts.put("content_type", "image/png");

        BlaaizException e = assertThrows(BlaaizException.class, () -> customers.uploadFileComplete("cust_1", opts));
        assertEquals("File upload failed: API request failed", e.getMessage());
        assertEquals(500, e.getStatus());
        verify(client, times(1)).makeRequest(eq("POST"), eq("/api/external/file/get-presigned-url"), anyMap(), isNull());
        verify(client, never()).uploadFile(any(), any(), any(), any());
    }

    @Test
    void uploadFileCompleteRejectsInvalidBase64String() {
        stubPresignedUrl(Map.of("url", "https://s3.example.com/p", "file_id", "file_12"));

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("file", "not@@valid$$base64!!");
        opts.put("file_category", "identity");

        BlaaizException e = assertThrows(BlaaizException.class, () -> customers.uploadFileComplete("cust_1", opts));
        assertTrue(e.getMessage().startsWith("File upload failed:"));
        assertTrue(e.getMessage().contains("does not appear to be valid base64"));
    }
}
