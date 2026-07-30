package com.blaaiz.sdk;

/** Result of a successful {@link BlaaizClient#uploadFile} S3 PUT. */
public final class UploadResult {

    private final int status;
    private final String etag;

    public UploadResult(int status, String etag) {
        this.status = status;
        this.etag = etag;
    }

    public int getStatus() {
        return status;
    }

    public String getEtag() {
        return etag;
    }
}
