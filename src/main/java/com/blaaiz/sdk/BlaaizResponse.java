package com.blaaiz.sdk;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generic response envelope returned by {@link BlaaizClient#makeRequest} and, in turn, by
 * every service method built on top of it.
 *
 * <p>Mirrors the {@code {data, status, headers}} shape returned as a plain
 * array/dict/object by the Laravel, Node.js, and Python SDKs respectively -- there is no
 * dedicated response type in any of the three source SDKs, so this class exists purely to
 * give that same shape a concrete, typed home in Java.
 *
 * <p>{@link #getData()} is intentionally {@code Object}, not a generic type parameter: the
 * parsed JSON body can be a {@code Map}, a {@code List}, a boxed primitive, or {@code null},
 * exactly as in the source SDKs, and nothing in this class attempts to unwrap or validate
 * its shape. In particular, this envelope is never unwrapped automatically -- callers that
 * need a nested {@code data.data.id} (the API frequently double-wraps its own payload in a
 * {@code data} key, independent of this envelope's own {@code data} field) must reach into
 * {@link #getData()} themselves, exactly as the composite facade methods in all three source
 * SDKs do.
 */
public final class BlaaizResponse {

    private final Object data;
    private final int status;
    private final Map<String, List<String>> headers;

    public BlaaizResponse(Object data, int status, Map<String, List<String>> headers) {
        this.data = data;
        this.status = status;
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
    }

    public Object getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}
