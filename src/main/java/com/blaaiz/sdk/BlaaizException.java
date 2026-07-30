package com.blaaiz.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    /**
     * {@code true} if {@link #getStatus()} falls in {@code [400, 500)}. Mirrors the Laravel
     * SDK's {@code isClientError()}: a {@code null} status (no HTTP call ever completed, e.g.
     * a transport/OAuth-plumbing failure) is neither a client nor a server error, so this
     * returns {@code false} rather than throwing on the unboxing.
     */
    public boolean isClientError() {
        return status != null && status >= 400 && status < 500;
    }

    /**
     * {@code true} if {@link #getStatus()} is {@code >= 500}. See {@link #isClientError()} for
     * the {@code null}-status handling.
     */
    public boolean isServerError() {
        return status != null && status >= 500;
    }

    /** {@code {message, status, error_code}}, matching the Laravel SDK's {@code toArray()}. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", getMessage());
        map.put("status", status);
        map.put("error_code", errorCode);
        return map;
    }

    /** JSON-serialized {@link #toMap()}. */
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(toMap());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode exception to JSON", e);
        }
    }
}
