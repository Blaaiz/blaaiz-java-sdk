package com.blaaiz.sdk;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlaaizResponseTest {

    @Test
    void exposesDataStatusAndHeadersVerbatim() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "cust_123");
        Map<String, List<String>> headers = Map.of("Content-Type", List.of("application/json"));

        BlaaizResponse response = new BlaaizResponse(data, 200, headers);

        assertSame(data, response.getData());
        assertEquals(200, response.getStatus());
        assertEquals(List.of("application/json"), response.getHeaders().get("Content-Type"));
    }

    @Test
    void nullHeadersBecomeEmptyMapRatherThanNull() {
        BlaaizResponse response = new BlaaizResponse("ok", 204, null);

        assertTrue(response.getHeaders().isEmpty());
    }

    @Test
    void headersMapIsUnmodifiable() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Test", List.of("1"));
        BlaaizResponse response = new BlaaizResponse(null, 200, headers);

        assertThrows(UnsupportedOperationException.class,
                () -> response.getHeaders().put("X-New", List.of("2")));
    }

    @Test
    void mutatingSourceHeadersMapAfterConstructionDoesNotAffectResponse() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Test", List.of("1"));
        BlaaizResponse response = new BlaaizResponse(null, 200, headers);

        headers.put("X-Late", List.of("late"));

        assertTrue(response.getHeaders().containsKey("X-Late"),
                "BlaaizResponse wraps rather than copies the headers map, matching a "
                        + "lightweight envelope with no defensive copy in any source SDK");
    }

    @Test
    void dataCanBeNullMirroringAnEmptyOrNonJsonApiBody() {
        BlaaizResponse response = new BlaaizResponse(null, 204, Map.of());

        assertNull(response.getData());
        assertEquals(204, response.getStatus());
    }

    @Test
    void dataCanBeAnyJsonShapeNotJustAnObject() {
        // The Blaaiz API's payloads sometimes double-wrap themselves in their own "data"
        // key beyond this envelope's own "data" field (e.g. { data: { data: { id: ... } } }).
        // BlaaizResponse never unwraps this itself -- getData() returns whatever was parsed,
        // verbatim, and composite facade methods reach into data.data.id on their own.
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("id", "txn_1");
        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("data", inner);

        BlaaizResponse response = new BlaaizResponse(outer, 200, Map.of());

        @SuppressWarnings("unchecked")
        Map<String, Object> outerData = (Map<String, Object>) response.getData();
        @SuppressWarnings("unchecked")
        Map<String, Object> innerData = (Map<String, Object>) outerData.get("data");
        assertEquals("txn_1", innerData.get("id"));
    }

    @Test
    void dataCanBeAListOrScalar() {
        BlaaizResponse listResponse = new BlaaizResponse(List.of("a", "b"), 200, Map.of());
        assertEquals(List.of("a", "b"), listResponse.getData());

        BlaaizResponse boolResponse = new BlaaizResponse(Boolean.TRUE, 200, Map.of());
        assertEquals(Boolean.TRUE, boolResponse.getData());
    }

    @Test
    void statusIsAPlainIntNotBoxed() {
        BlaaizResponse response = new BlaaizResponse(null, 404, Map.of());

        int status = response.getStatus();
        assertEquals(404, status);
    }
}
