package com.blaaiz.sdk;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Signa ID release ({@code /api/external/signa-id}). A person who is already verified releases
 * their data to your business in a popup. Your server creates a release request, your page opens
 * the popup, and your server exchanges the code for the data.
 *
 * <p>The release methods need an OAuth access token with the {@code signa-id:release} scope. API
 * keys cannot call them. The default scope list of the SDK does not include this scope, so set it
 * with {@link BlaaizClientOptions#oauthScope(String)}, preferably on a dedicated credential.
 * {@link #getWalletStatus} is public and needs no scope.
 *
 * <p><b>Warning:</b> released data is personal data. Do not log it and do not cache it.
 */
public class SignaIdService extends BaseService {

    private static final String BASE_PATH = "/api/external/signa-id";

    private static final List<String> RELEASE_SCOPES = Collections.unmodifiableList(List.of(
            "identity", "id_document", "address", "document_images"));

    public SignaIdService(BlaaizClient client) {
        super(client);
    }

    /**
     * @param requestData must contain {@code idempotency_key}, {@code purpose}, {@code origin}
     *                    (the exact origin of your page), and a non-empty {@code scopes} list
     *                    with values from {@code identity}, {@code id_document}, {@code address},
     *                    {@code document_images}. Optional {@code reference} is forwarded as is.
     */
    public BlaaizResponse createReleaseRequest(Map<String, Object> requestData) {
        validateReleaseRequestData(requestData);
        return client.makeRequest("POST", BASE_PATH + "/release-requests", requestData, null);
    }

    /** Exchanges the one-time code from the popup for the released data. The code lasts 5 minutes. */
    public BlaaizResponse exchangeReleaseCode(String code) {
        requireNonBlank(code, "Release code is required");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        return client.makeRequest("POST", BASE_PATH + "/releases/exchange", body, null);
    }

    /** Reads a release again during its access window. */
    public BlaaizResponse getRelease(String releaseId) {
        requireNonBlank(releaseId, "Release ID is required");
        return client.makeRequest("GET", BASE_PATH + "/releases/" + encodePathSegment(releaseId), null, null);
    }

    /** Returns a 15-minute download link for one released document image. */
    public BlaaizResponse getReleaseDocument(String releaseId, String documentId) {
        requireNonBlank(releaseId, "Release ID is required");
        requireNonBlank(documentId, "Document ID is required");
        return client.makeRequest(
                "GET", BASE_PATH + "/releases/" + encodePathSegment(releaseId) + "/documents/"
                        + encodePathSegment(documentId), null, null);
    }

    public BlaaizResponse getWalletStatus(String address) {
        return getWalletStatus(address, null);
    }

    /**
     * Checks if a wallet belongs to a verified Signa ID. The endpoint needs no authentication and
     * returns no personal data. The response is at the root of the body.
     *
     * @param chainId optional chain filter; sent as {@code chain_id} only when not {@code null}.
     */
    public BlaaizResponse getWalletStatus(String address, Integer chainId) {
        requireNonBlank(address, "Wallet address is required");
        String path = "/api/v1/signa-id/public/wallets/" + encodePathSegment(address) + "/status";
        if (chainId != null) {
            path += "?chain_id=" + chainId;
        }
        return client.makeRequest("GET", path, null, null);
    }

    private static void validateReleaseRequestData(Map<String, Object> requestData) {
        if (requestData == null) {
            throw new IllegalArgumentException("Release request data is required");
        }
        requireFields(requestData, "idempotency_key", "purpose", "scopes", "origin");

        Object scopes = requestData.get("scopes");
        if (!(scopes instanceof List) || ((List<?>) scopes).isEmpty()) {
            throw new IllegalArgumentException("scopes must be a non-empty array");
        }
        for (Object scope : (List<?>) scopes) {
            if (!(scope instanceof String) || !RELEASE_SCOPES.contains(scope)) {
                throw new IllegalArgumentException("scopes must contain only: " + String.join(", ", RELEASE_SCOPES));
            }
        }
    }

    /** Mirrors {@code encodeURIComponent}: {@code URLEncoder} encodes a space as {@code +}, not {@code %20}. */
    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
