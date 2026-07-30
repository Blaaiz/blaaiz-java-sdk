package com.blaaiz.sdk;

import java.util.Map;

/** Raw presigned-URL access; most callers should prefer {@code CustomerService.uploadFileComplete}. */
public class FileService extends BaseService {

    public FileService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse getPresignedUrl(Map<String, Object> fileData) {
        requireFields(fileData, "customer_id", "file_category");
        return client.makeRequest("POST", "/api/external/file/get-presigned-url", fileData, null);
    }
}
