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
class FileServiceTest {

    @Mock
    private BlaaizClient client;

    private FileService files;

    @BeforeEach
    void setUp() {
        files = new FileService(client);
    }

    private static Map<String, Object> validFileData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer_id", "cust_1");
        data.put("file_category", "identity");
        return data;
    }

    // ---- getPresignedUrl ----

    @Test
    void getPresignedUrlSendsPostWithFileData() {
        BlaaizResponse response = new BlaaizResponse(
                Map.of("data", Map.of("url", "https://s3.example.com/presigned", "file_id", "file_1")),
                200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/file/get-presigned-url"), eq(validFileData()), isNull()))
                .thenReturn(response);

        BlaaizResponse result = files.getPresignedUrl(validFileData());

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/file/get-presigned-url", validFileData(), null);
    }

    @Test
    void getPresignedUrlForwardsExtraFieldsVerbatim() {
        Map<String, Object> data = validFileData();
        data.put("id_file_back", "back_scan_ref");
        data.put("file_category", "identity_back");
        BlaaizResponse response = new BlaaizResponse(Map.of("data", Map.of("url", "https://s3.example.com/x")), 200, null);
        when(client.makeRequest(eq("POST"), eq("/api/external/file/get-presigned-url"), eq(data), isNull()))
                .thenReturn(response);

        BlaaizResponse result = files.getPresignedUrl(data);

        assertEquals(response, result);
        verify(client).makeRequest("POST", "/api/external/file/get-presigned-url", data, null);
    }

    @Test
    void getPresignedUrlThrowsWhenCustomerIdMissing() {
        Map<String, Object> data = validFileData();
        data.remove("customer_id");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> files.getPresignedUrl(data));
        assertEquals("customer_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getPresignedUrlThrowsWhenCustomerIdBlank() {
        Map<String, Object> data = validFileData();
        data.put("customer_id", "");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> files.getPresignedUrl(data));
        assertEquals("customer_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getPresignedUrlThrowsWhenFileCategoryMissing() {
        Map<String, Object> data = validFileData();
        data.remove("file_category");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> files.getPresignedUrl(data));
        assertEquals("file_category is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getPresignedUrlChecksCustomerIdBeforeFileCategory() {
        // Both fields missing: customer_id is validated first, matching the check order shared
        // by the Laravel/Node.js/Python source SDKs (customer_id listed before file_category).
        Map<String, Object> data = new LinkedHashMap<>();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> files.getPresignedUrl(data));
        assertEquals("customer_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getPresignedUrlThrowsWhenFileDataIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> files.getPresignedUrl(null));
        assertEquals("customer_id is required", e.getMessage());
        verifyNoInteractions(client);
    }

    @Test
    void getPresignedUrlPropagatesBlaaizExceptionFromClient() {
        when(client.makeRequest(
                eq("POST"), eq("/api/external/file/get-presigned-url"), eq(validFileData()), isNull()))
                .thenThrow(new BlaaizException("Invalid file_category", 400, "VALIDATION_ERROR"));

        BlaaizException e = assertThrows(BlaaizException.class, () -> files.getPresignedUrl(validFileData()));
        assertEquals("Invalid file_category", e.getMessage());
        assertEquals(400, e.getStatus());
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
    }
}
