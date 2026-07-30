package com.blaaiz.sdk;

/**
 * Result of {@link CustomerService#uploadFileComplete}: the file-association API response plus
 * the {@code file_id} and {@code presigned_url} produced along the way.
 *
 * <p>The source SDKs return a single flattened map/array ({@code {...associationResponse,
 * file_id, presigned_url}}); this class gives that same information a typed home in Java rather
 * than merging heterogeneous keys into one untyped map.
 */
public final class UploadFileCompleteResult {

    private final BlaaizResponse associationResponse;
    private final String fileId;
    private final String presignedUrl;

    public UploadFileCompleteResult(BlaaizResponse associationResponse, String fileId, String presignedUrl) {
        this.associationResponse = associationResponse;
        this.fileId = fileId;
        this.presignedUrl = presignedUrl;
    }

    public BlaaizResponse getAssociationResponse() {
        return associationResponse;
    }

    public String getFileId() {
        return fileId;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }
}
