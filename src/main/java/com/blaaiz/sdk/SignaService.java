package com.blaaiz.sdk;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Signa merchant KYC/KYB verification sessions ({@code /api/external/compliance/kyc/sessions}).
 *
 * <p>A session collects one or more requirements ({@code DOCUMENTS}, {@code SELFIE},
 * {@code FACE_MATCH}, {@code PROOF_OF_ADDRESS}) for a business's own customer, either through a
 * hosted verification link or headlessly via server-side document uploads.
 */
public class SignaService extends BaseService {

    private static final String BASE_PATH = "/api/external/compliance/kyc/sessions";

    private static final List<String> REQUIREMENTS = Collections.unmodifiableList(List.of(
            "DOCUMENTS", "SELFIE", "FACE_MATCH", "PROOF_OF_ADDRESS"));

    private static final List<String> DOCUMENT_TYPES = Collections.unmodifiableList(List.of(
            "PASSPORT", "ID_CARD", "DRIVERS", "RESIDENCE_PERMIT", "UTILITY_BILL", "BANK_STATEMENT", "SELFIE"));

    private static final List<String> CONTENT_TYPES = Collections.unmodifiableList(List.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"));

    public SignaService(BlaaizClient client) {
        super(client);
    }

    /**
     * @param sessionData must contain {@code customer_reference} (max 100), {@code idempotency_key}
     *                    (max 100; a repeated key replays the same session), and a non-empty
     *                    {@code requirements} list (1-4 distinct values from {@code DOCUMENTS},
     *                    {@code SELFIE}, {@code FACE_MATCH}, {@code PROOF_OF_ADDRESS}). Optional
     *                    {@code fulfilment_mode} ({@code HOSTED} or {@code HEADLESS}) and
     *                    {@code applicant} are forwarded verbatim for the API to validate.
     *                    Optional {@code redirect_url} (an https URL on your site) is also forwarded
     *                    as is. A Blaaiz-hosted verification page sends the person there with
     *                    {@code session_id} added.
     */
    public BlaaizResponse createSession(Map<String, Object> sessionData) {
        validateSessionData(sessionData);
        return client.makeRequest("POST", BASE_PATH, sessionData, null);
    }

    /**
     * @param filters optional query parameters; recognised keys are {@code limit} and
     *                {@code offset}. Sent as query parameters when non-empty.
     */
    public BlaaizResponse listSessions(Map<String, Object> filters) {
        Map<String, Object> params = (filters == null || filters.isEmpty()) ? null : filters;
        return client.makeRequest("GET", BASE_PATH, params, null);
    }

    public BlaaizResponse listSessions() {
        return listSessions(null);
    }

    public BlaaizResponse getSession(String sessionId) {
        requireNonBlank(sessionId, "Session ID is required");
        return client.makeRequest("GET", BASE_PATH + "/" + encodePathSegment(sessionId), null, null);
    }

    /** Submits the session for verification. Only works on a {@code HEADLESS} session. Takes no body. */
    public BlaaizResponse submitSession(String sessionId) {
        requireNonBlank(sessionId, "Session ID is required");
        return client.makeRequest(
                "POST", BASE_PATH + "/" + encodePathSegment(sessionId) + "/submit", null, null);
    }

    /** Takes no body. */
    public BlaaizResponse cancelSession(String sessionId) {
        requireNonBlank(sessionId, "Session ID is required");
        return client.makeRequest(
                "POST", BASE_PATH + "/" + encodePathSegment(sessionId) + "/cancel", null, null);
    }

    /**
     * Requests a short-lived URL for a staged document upload. Only works on a {@code HEADLESS}
     * session. Send the {@code headers} map from the response data unchanged with the {@code PUT},
     * because they are part of the URL's signature.
     *
     * @param uploadData must contain {@code file_name} (max 128; letters, numbers, spaces,
     *                   dashes, underscores; must end in {@code .jpg}, {@code .jpeg},
     *                   {@code .png}, {@code .webp}, or {@code .pdf}) and {@code id_doc_type}
     *                   (one of {@code PASSPORT}, {@code ID_CARD}, {@code DRIVERS},
     *                   {@code RESIDENCE_PERMIT}, {@code UTILITY_BILL}, {@code BANK_STATEMENT},
     *                   {@code SELFIE}).
     */
    public BlaaizResponse createDocumentUploadUrl(String sessionId, Map<String, Object> uploadData) {
        requireNonBlank(sessionId, "Session ID is required");
        validateDocumentUploadUrlData(uploadData);
        return client.makeRequest(
                "POST", BASE_PATH + "/" + encodePathSegment(sessionId) + "/documents/upload-url", uploadData, null);
    }

    /**
     * Registers a document with the session, either inline or staged via
     * {@link #createDocumentUploadUrl}. Only works on a {@code HEADLESS} session.
     *
     * @param documentData must contain {@code filename} (max 191), {@code content_type}
     *                     ({@code image/jpeg}, {@code image/png}, {@code image/webp}, or
     *                     {@code application/pdf}), {@code id_doc_type} (as in
     *                     {@link #createDocumentUploadUrl}), and {@code country} (ISO 3166-1
     *                     alpha-3). Exactly one of {@code file_name} (the staged name returned by
     *                     {@link #createDocumentUploadUrl}) or {@code content_base64} (inline;
     *                     very small files only, because the API can reject request bodies over
     *                     roughly 8 KB) must be set.
     */
    public BlaaizResponse uploadSessionDocument(String sessionId, Map<String, Object> documentData) {
        requireNonBlank(sessionId, "Session ID is required");
        validateDocumentData(documentData);
        return client.makeRequest(
                "POST", BASE_PATH + "/" + encodePathSegment(sessionId) + "/documents", documentData, null);
    }

    /**
     * Issues or rotates the customer-facing verification link. Only works on a {@code HOSTED}
     * session. Takes no body.
     */
    public BlaaizResponse issueVerificationLink(String sessionId) {
        requireNonBlank(sessionId, "Session ID is required");
        return client.makeRequest(
                "POST", BASE_PATH + "/" + encodePathSegment(sessionId) + "/verification-link", null, null);
    }

    /**
     * Issues a web SDK access token for a {@code HOSTED} session, so a page can start the session
     * in a popup. The token is valid for 30 minutes. A new call returns the same token while more
     * than 10 minutes remain. Otherwise it returns a new token, and the previous token and
     * verification link stop working. Takes no body.
     *
     * <p><b>Warning:</b> the token is a bearer credential. Do not put it in a URL and do not log it.
     */
    public BlaaizResponse issueAccessToken(String sessionId) {
        requireNonBlank(sessionId, "Session ID is required");
        return client.makeRequest(
                "POST", BASE_PATH + "/" + encodePathSegment(sessionId) + "/access-token", null, null);
    }

    /**
     * Returns the applicant data captured for the session: name, date of birth, nationality,
     * address, and the presented identity document. The response data is {@code null} when the
     * session has a verdict but no applicant data was captured. Requires the
     * {@code compliance-kyc:pii:read} scope, which Blaaiz grants to a credential only on request.
     * Returns 409 until the session reaches {@code APPROVED} or {@code REJECTED}.
     *
     * <p><b>Warning:</b> the response carries personal data. The API sends
     * {@code Cache-Control: no-store}; do not log or cache the response body.
     */
    public BlaaizResponse getSessionApplicantData(String sessionId) {
        requireNonBlank(sessionId, "Session ID is required");
        return client.makeRequest(
                "GET", BASE_PATH + "/" + encodePathSegment(sessionId) + "/applicant-data", null, null);
    }

    /**
     * Lists the documents captured for the session: kind, side, content type, and whether the
     * bytes are still retained. Requires the {@code compliance-kyc:pii:read} scope. Returns 409
     * until the session reaches {@code APPROVED} or {@code REJECTED}.
     */
    public BlaaizResponse listSessionDocuments(String sessionId) {
        requireNonBlank(sessionId, "Session ID is required");
        return client.makeRequest(
                "GET", BASE_PATH + "/" + encodePathSegment(sessionId) + "/documents", null, null);
    }

    /**
     * Returns a 15-minute download link for one document -- never the bytes themselves.
     * Requires the {@code compliance-kyc:pii:read} scope. Returns 409 until the session reaches
     * {@code APPROVED} or {@code REJECTED}, and 410 once the document is no
     * longer retained. Rate limited to 30 requests per minute and 600 per hour per business
     * (429 above that).
     *
     * <p><b>Warning:</b> the response carries personal data. The API sends
     * {@code Cache-Control: no-store}; do not log or cache the response body.
     */
    public BlaaizResponse getSessionDocument(String sessionId, String documentId) {
        requireNonBlank(sessionId, "Session ID is required");
        requireNonBlank(documentId, "Document ID is required");
        return client.makeRequest(
                "GET", BASE_PATH + "/" + encodePathSegment(sessionId) + "/documents/"
                        + encodePathSegment(documentId), null, null);
    }

    // Short aliases mirror the create/list/get style used by the other SDK resources.

    public BlaaizResponse create(Map<String, Object> sessionData) {
        return createSession(sessionData);
    }

    public BlaaizResponse list(Map<String, Object> filters) {
        return listSessions(filters);
    }

    public BlaaizResponse list() {
        return listSessions();
    }

    public BlaaizResponse get(String sessionId) {
        return getSession(sessionId);
    }

    public BlaaizResponse submit(String sessionId) {
        return submitSession(sessionId);
    }

    public BlaaizResponse cancel(String sessionId) {
        return cancelSession(sessionId);
    }

    public BlaaizResponse uploadDocument(String sessionId, Map<String, Object> documentData) {
        return uploadSessionDocument(sessionId, documentData);
    }

    private static void validateSessionData(Map<String, Object> sessionData) {
        if (sessionData == null) {
            throw new IllegalArgumentException("Session data is required");
        }
        requireFields(sessionData, "customer_reference", "idempotency_key", "requirements");

        Object requirements = sessionData.get("requirements");
        if (!(requirements instanceof List) || ((List<?>) requirements).isEmpty()) {
            throw new IllegalArgumentException("requirements must be a non-empty array");
        }
        for (Object requirement : (List<?>) requirements) {
            if (!(requirement instanceof String)
                    || !REQUIREMENTS.contains(((String) requirement).toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("requirements must contain only: " + String.join(", ", REQUIREMENTS));
            }
        }
    }

    private static void validateDocumentUploadUrlData(Map<String, Object> uploadData) {
        validateDocumentShape(uploadData, "file_name", "id_doc_type");
        requireVocabulary(uploadData.get("id_doc_type"), DOCUMENT_TYPES, true, "id_doc_type");
    }

    private static void validateDocumentData(Map<String, Object> documentData) {
        validateDocumentShape(documentData, "filename", "content_type", "id_doc_type", "country");
        requireVocabulary(documentData.get("content_type"), CONTENT_TYPES, false, "content_type");
        requireVocabulary(documentData.get("id_doc_type"), DOCUMENT_TYPES, true, "id_doc_type");

        boolean hasStagedFile = isNonEmptyString(documentData.get("file_name"));
        boolean hasInlineContent = isNonEmptyString(documentData.get("content_base64"));
        if (hasStagedFile == hasInlineContent) {
            throw new IllegalArgumentException("Provide exactly one of file_name or content_base64");
        }
    }

    private static void validateDocumentShape(Map<String, Object> data, String... fields) {
        if (data == null) {
            throw new IllegalArgumentException("Document data is required");
        }
        requireFields(data, fields);
    }

    private static void requireVocabulary(Object value, List<String> vocabulary, boolean upperCase, String fieldName) {
        String normalized = value instanceof String
                ? (upperCase ? ((String) value).toUpperCase(Locale.ROOT) : ((String) value).toLowerCase(Locale.ROOT))
                : null;
        if (normalized == null || !vocabulary.contains(normalized)) {
            throw new IllegalArgumentException(fieldName + " must be one of: " + String.join(", ", vocabulary));
        }
    }

    private static boolean isNonEmptyString(Object value) {
        return value instanceof String && !((String) value).isEmpty();
    }

    /** Mirrors {@code encodeURIComponent}: {@code URLEncoder} encodes a space as {@code +}, not {@code %20}. */
    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
