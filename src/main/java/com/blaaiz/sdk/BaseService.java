package com.blaaiz.sdk;

import java.util.Map;
import java.util.Objects;

/**
 * Shared plumbing for the per-resource service classes: holds the {@link BlaaizClient} and
 * provides the local input-validation helpers used throughout.
 *
 * <p>Mirrors the Laravel SDK's {@code BaseService}, except validation failures here raise a
 * plain unchecked {@link IllegalArgumentException} rather than {@code BlaaizException} -- see
 * {@link BlaaizException}'s class Javadoc for the rationale (Node.js/Python precedent).
 */
abstract class BaseService {

    protected final BlaaizClient client;

    BaseService(BlaaizClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    /**
     * Throws {@link IllegalArgumentException} for the first field in {@code fields} that is
     * missing or "empty" in {@code data}. A value counts as empty using the same falsy rule as
     * the Laravel ({@code empty()}), Node.js ({@code !value}), and Python ({@code not value})
     * source SDKs: {@code null}, an empty string, the number zero, or {@code false}.
     */
    static void requireFields(Map<String, Object> data, String... fields) {
        for (String field : fields) {
            if (data == null || isBlank(data.get(field))) {
                throw new IllegalArgumentException(field + " is required");
            }
        }
    }

    static void requireNonBlank(String value, String message) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() == 0.0;
        }
        if (value instanceof Boolean) {
            return !((Boolean) value);
        }
        return false;
    }
}
