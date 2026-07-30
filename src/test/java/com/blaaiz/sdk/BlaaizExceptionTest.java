package com.blaaiz.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlaaizExceptionTest {

    @Test
    void constructorStoresMessageStatusAndErrorCodeVerbatim() {
        BlaaizException exception = new BlaaizException("boom", 400, "BAD_REQUEST");

        assertEquals("boom", exception.getMessage());
        assertEquals(400, exception.getStatus());
        assertEquals("BAD_REQUEST", exception.getErrorCode());
    }

    @Test
    void singleArgConstructorLeavesStatusAndErrorCodeNull() {
        BlaaizException exception = new BlaaizException("no request was ever made");

        assertNull(exception.getStatus());
        assertNull(exception.getErrorCode());
    }

    @Test
    void isRuntimeExceptionUnchecked() {
        assertTrue(RuntimeException.class.isAssignableFrom(BlaaizException.class),
                "BlaaizException must be unchecked so it doesn't force try/catch or throws "
                        + "clauses onto every service method, matching Node/Python's plain "
                        + "Error/Exception behavior");
    }

    @Test
    void isClientErrorTrueFor4xx() {
        assertTrue(new BlaaizException("bad request", 400, null).isClientError());
        assertTrue(new BlaaizException("not found", 404, null).isClientError());
        assertTrue(new BlaaizException("edge", 499, null).isClientError());
    }

    @Test
    void isClientErrorFalseOutside4xxRange() {
        assertFalse(new BlaaizException("ok-ish", 399, null).isClientError());
        assertFalse(new BlaaizException("server", 500, null).isClientError());
        assertFalse(new BlaaizException("success", 200, null).isClientError());
    }

    @Test
    void isServerErrorTrueFor5xxAndAbove() {
        assertTrue(new BlaaizException("server", 500, null).isServerError());
        assertTrue(new BlaaizException("gateway", 502, null).isServerError());
        assertTrue(new BlaaizException("way up", 599, null).isServerError());
    }

    @Test
    void isServerErrorFalseBelow500() {
        assertFalse(new BlaaizException("client", 499, null).isServerError());
        assertFalse(new BlaaizException("ok", 200, null).isServerError());
    }

    @Test
    void nullStatusIsNeitherClientNorServerError() {
        // Mirrors the Laravel SDK's PHP semantics, where comparing a null status with >= / <
        // coerces to false rather than raising a TypeError; a transport failure that never got
        // an HTTP response (e.g. OAuth plumbing, DNS failure) should not misreport as either.
        BlaaizException exception = new BlaaizException("no response", null, "REQUEST_ERROR");

        assertFalse(exception.isClientError());
        assertFalse(exception.isServerError());
    }

    @Test
    void toMapContainsMessageStatusAndErrorCodeUnderSnakeCaseKey() {
        BlaaizException exception = new BlaaizException("boom", 404, "NOT_FOUND");

        Map<String, Object> map = exception.toMap();

        assertEquals("boom", map.get("message"));
        assertEquals(404, map.get("status"));
        assertEquals("NOT_FOUND", map.get("error_code"));
        assertEquals(3, map.size());
    }

    @Test
    void toMapKeepsStatusAndErrorCodeNullWhenAbsent() {
        BlaaizException exception = new BlaaizException("boom");

        Map<String, Object> map = exception.toMap();

        assertEquals("boom", map.get("message"));
        assertNull(map.get("status"));
        assertNull(map.get("error_code"));
        assertTrue(map.containsKey("status"));
        assertTrue(map.containsKey("error_code"));
    }

    @Test
    void toJsonProducesTheSerializedToMapShape() throws Exception {
        BlaaizException exception = new BlaaizException("boom", 400, "BAD_REQUEST");

        String json = exception.toJson();

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        assertEquals("boom", parsed.get("message"));
        assertEquals(400, parsed.get("status"));
        assertEquals("BAD_REQUEST", parsed.get("error_code"));
    }

    @Test
    void toJsonHandlesNullStatusAndErrorCode() throws Exception {
        BlaaizException exception = new BlaaizException("no request");

        String json = exception.toJson();

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(json, Map.class);
        assertEquals("no request", parsed.get("message"));
        assertNull(parsed.get("status"));
        assertNull(parsed.get("error_code"));
    }
}
