package com.blaaiz.sdk;

/**
 * Dedicated exception type for network/API/OAuth/S3 transport failures raised by
 * {@link BlaaizClient} and the higher-level services built on top of it.
 *
 * <p>This is a deliberate two-tier design (unlike the Laravel SDK, which raises this
 * same exception type for local input-validation failures too): local validation errors
 * in service classes should use a plain unchecked {@link IllegalArgumentException}
 * instead, reserving {@code BlaaizException} for failures that actually involve a
 * request (HTTP call, OAuth token fetch, S3 upload/download). Node.js and Python's SDKs
 * both draw this same line, and were used as the tie-breaker for this Java port.
 */
public class BlaaizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer status;
    private final String errorCode;

    public BlaaizException(String message) {
        this(message, null, null);
    }

    public BlaaizException(String message, Integer status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    /** HTTP status code associated with the failure, if any. */
    public Integer getStatus() {
        return status;
    }

    /** API- or SDK-assigned error code associated with the failure, if any. */
    public String getErrorCode() {
        return errorCode;
    }
}
