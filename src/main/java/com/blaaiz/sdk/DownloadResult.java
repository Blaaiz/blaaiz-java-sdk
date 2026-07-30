package com.blaaiz.sdk;

/** Result of a successful {@link BlaaizClient#downloadFile} GET. */
public final class DownloadResult {

    private final byte[] content;
    private final String contentType;
    private final String filename;

    public DownloadResult(byte[] content, String contentType, String filename) {
        this.content = content;
        this.contentType = contentType;
        this.filename = filename;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }
}
