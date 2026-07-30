package com.blaaiz.sdk;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Result of a successful {@link BlaaizClient#makeRequest} call. */
public final class BlaaizResponse<T> {

    private final T data;
    private final int status;
    private final Map<String, List<String>> headers;

    public BlaaizResponse(T data, int status, Map<String, List<String>> headers) {
        this.data = data;
        this.status = status;
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
    }

    public T getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}
