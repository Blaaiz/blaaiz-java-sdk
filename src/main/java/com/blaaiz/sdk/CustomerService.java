package com.blaaiz.sdk;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Customer (KYC subject) management, including the S3-backed document upload flow. */
public class CustomerService extends BaseService {

    /**
     * The four supported file categories. Laravel's SDK supports all four (including
     * {@code identity_back}); Node.js and Python support only the other three. The larger set
     * is adopted here per the canonical resolution -- confirm the live API actually accepts
     * {@code id_file_back} before relying on {@code identity_back} in production.
     */
    private static final Set<String> FILE_CATEGORIES = Set.of(
            "identity", "identity_back", "proof_of_address", "liveness_check");

    private static final Map<String, String> FILE_FIELD_BY_CATEGORY;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("identity", "id_file");
        m.put("identity_back", "id_file_back");
        m.put("liveness_check", "liveness_check_file");
        m.put("proof_of_address", "proof_of_address_file");
        FILE_FIELD_BY_CATEGORY = Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> EXTENSION_TO_MIME;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("jpg", "image/jpeg");
        m.put("jpeg", "image/jpeg");
        m.put("png", "image/png");
        m.put("gif", "image/gif");
        m.put("webp", "image/webp");
        m.put("bmp", "image/bmp");
        m.put("tiff", "image/tiff");
        m.put("tif", "image/tiff");
        m.put("pdf", "application/pdf");
        m.put("txt", "text/plain");
        m.put("doc", "application/msword");
        m.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        EXTENSION_TO_MIME = Collections.unmodifiableMap(m);
    }

    private static final Pattern DATA_URL_CONTENT_TYPE = Pattern.compile("data:([^;]+)");

    public CustomerService(BlaaizClient client) {
        super(client);
    }

    public BlaaizResponse create(Map<String, Object> customerData) {
        requireFields(customerData, "type", "email", "country", "id_type", "id_number");

        Object type = customerData.get("type");
        if ("individual".equals(type)) {
            if (isBlank(customerData.get("first_name"))) {
                throw new IllegalArgumentException("first_name is required when type is individual");
            }
            if (isBlank(customerData.get("last_name"))) {
                throw new IllegalArgumentException("last_name is required when type is individual");
            }
        } else if ("business".equals(type) && isBlank(customerData.get("business_name"))) {
            throw new IllegalArgumentException("business_name is required when type is business");
        }

        return client.makeRequest("POST", "/api/external/customer", customerData, null);
    }

    public BlaaizResponse list(Map<String, Object> filters) {
        return client.makeRequest("GET", "/api/external/customer", filters, null);
    }

    public BlaaizResponse get(String customerId) {
        requireNonBlank(customerId, "Customer ID is required");
        return client.makeRequest("GET", "/api/external/customer/" + customerId, null, null);
    }

    public BlaaizResponse update(String customerId, Map<String, Object> updateData) {
        requireNonBlank(customerId, "Customer ID is required");
        return client.makeRequest("PUT", "/api/external/customer/" + customerId, updateData, null);
    }

    public BlaaizResponse addKyc(String customerId, Map<String, Object> kycData) {
        requireNonBlank(customerId, "Customer ID is required");
        return client.makeRequest("POST", "/api/external/customer/" + customerId + "/kyc-data", kycData, null);
    }

    public BlaaizResponse uploadFiles(String customerId, Map<String, Object> fileData) {
        requireNonBlank(customerId, "Customer ID is required");
        return client.makeRequest("PUT", "/api/external/customer/" + customerId + "/files", fileData, null);
    }

    public BlaaizResponse listBeneficiaries(String customerId) {
        requireNonBlank(customerId, "Customer ID is required");
        return client.makeRequest("GET", "/api/external/customer/" + customerId + "/beneficiary", null, null);
    }

    public BlaaizResponse getBeneficiary(String customerId, String beneficiaryId) {
        requireNonBlank(customerId, "Customer ID is required");
        requireNonBlank(beneficiaryId, "Beneficiary ID is required");
        return client.makeRequest("GET",
                "/api/external/customer/" + customerId + "/beneficiary/" + beneficiaryId, null, null);
    }

    /**
     * End-to-end KYC document upload: requests a presigned S3 URL, uploads the file content to
     * it, then associates the resulting {@code file_id} with the customer.
     *
     * <p>{@code fileOptions} keys: {@code file} (required; a {@code byte[]}, a {@code data:...}
     * URL string, an {@code http(s)://} URL string to download from, or a plain base64 string),
     * {@code file_category} (required; one of {@link #FILE_CATEGORIES}), {@code filename}
     * (optional), {@code content_type} (optional; auto-detected from magic bytes or filename
     * extension when omitted).
     */
    public UploadFileCompleteResult uploadFileComplete(String customerId, Map<String, Object> fileOptions) {
        requireNonBlank(customerId, "Customer ID is required");
        if (fileOptions == null || fileOptions.isEmpty()) {
            throw new IllegalArgumentException("File options are required");
        }

        Object file = fileOptions.get("file");
        Object fileCategoryObj = fileOptions.get("file_category");
        String fileCategory = fileCategoryObj != null ? String.valueOf(fileCategoryObj) : null;
        String filename = (String) fileOptions.get("filename");
        String contentType = (String) fileOptions.get("content_type");

        if (file == null) {
            throw new IllegalArgumentException("File is required");
        }
        if (fileCategory == null || fileCategory.isEmpty()) {
            throw new IllegalArgumentException("file_category is required");
        }
        if (!FILE_CATEGORIES.contains(fileCategory)) {
            throw new IllegalArgumentException(
                    "file_category must be one of: identity, identity_back, proof_of_address, liveness_check");
        }

        try {
            Map<String, Object> presignedRequest = new LinkedHashMap<>();
            presignedRequest.put("customer_id", customerId);
            presignedRequest.put("file_category", fileCategory);
            BlaaizResponse presignedResponse =
                    client.makeRequest("POST", "/api/external/file/get-presigned-url", presignedRequest, null);

            String[] presigned = extractPresignedUrlAndFileId(presignedResponse);
            String presignedUrl = presigned[0];
            String fileId = presigned[1];

            FileInput input = processFileInput(file, contentType, filename);
            contentType = input.contentType;
            filename = input.filename;

            if (contentType == null) {
                contentType = detectContentTypeFromBytes(input.content);
            }
            if (contentType == null && filename != null) {
                contentType = contentTypeFromFilename(filename);
            }
            if (contentType == null) {
                throw new IllegalArgumentException(
                        "Could not determine file content type. Please provide a content_type "
                                + "(e.g., \"image/jpeg\", \"image/png\", \"application/pdf\") in fileOptions.");
            }

            client.uploadFile(presignedUrl, input.content, contentType, filename);

            String fileFieldName = FILE_FIELD_BY_CATEGORY.get(fileCategory);
            Map<String, Object> associationBody = new LinkedHashMap<>();
            associationBody.put(fileFieldName, fileId);
            BlaaizResponse associationResponse = client.makeRequest(
                    "POST", "/api/external/customer/" + customerId + "/files", associationBody, null);

            return new UploadFileCompleteResult(associationResponse, fileId, presignedUrl);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (BlaaizException e) {
            if (e.getMessage() != null && e.getMessage().contains("File upload failed:")) {
                throw e;
            }
            throw new BlaaizException("File upload failed: " + e.getMessage(), e.getStatus(), e.getErrorCode());
        }
    }

    @SuppressWarnings("unchecked")
    private static String[] extractPresignedUrlAndFileId(BlaaizResponse response) {
        Object data = response.getData();
        if (data instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) data;
            Object url = body.get("url");
            Object fileId = body.get("file_id");
            if (url != null && fileId != null) {
                return new String[] {String.valueOf(url), String.valueOf(fileId)};
            }
            Object nested = body.get("data");
            if (nested instanceof Map) {
                Map<String, Object> nestedMap = (Map<String, Object>) nested;
                Object nestedUrl = nestedMap.get("url");
                Object nestedFileId = nestedMap.get("file_id");
                if (nestedUrl != null && nestedFileId != null) {
                    return new String[] {String.valueOf(nestedUrl), String.valueOf(nestedFileId)};
                }
            }
        }
        throw new BlaaizException(
                "Invalid presigned URL response structure. Expected 'url' and 'file_id' keys. Got: " + data);
    }

    private FileInput processFileInput(Object file, String contentType, String filename) {
        if (file instanceof byte[]) {
            return new FileInput((byte[]) file, contentType, filename);
        }
        if (!(file instanceof String)) {
            throw new IllegalArgumentException("file must be a byte[], a data: URL, an http(s):// URL, or a base64 string");
        }

        String fileStr = (String) file;
        if (fileStr.startsWith("data:")) {
            int comma = fileStr.indexOf(',');
            String base64Part = comma >= 0 ? fileStr.substring(comma + 1) : "";
            if (base64Part.isEmpty()) {
                throw new IllegalArgumentException("Invalid data URL: no base64 data found after the comma");
            }
            byte[] content = decodeBase64(base64Part,
                    "The base64 portion of the data URL does not appear to be valid base64. "
                            + "Ensure the string after the comma contains only valid base64 characters.");

            String detectedContentType = contentType;
            if (detectedContentType == null) {
                Matcher matcher = DATA_URL_CONTENT_TYPE.matcher(fileStr);
                if (matcher.find()) {
                    detectedContentType = matcher.group(1);
                }
            }
            return new FileInput(content, detectedContentType, filename);
        }

        if (fileStr.startsWith("http://") || fileStr.startsWith("https://")) {
            DownloadResult downloaded = client.downloadFile(fileStr);
            String resolvedContentType = contentType != null ? contentType : downloaded.getContentType();
            String resolvedFilename = filename != null ? filename : downloaded.getFilename();
            return new FileInput(downloaded.getContent(), resolvedContentType, resolvedFilename);
        }

        byte[] content = decodeBase64(fileStr,
                "The file string does not appear to be valid base64. "
                        + "If you meant to pass a file path or URL, use the appropriate format instead.");
        return new FileInput(content, contentType, filename);
    }

    private static byte[] decodeBase64(String value, String errorMessage) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static String detectContentTypeFromBytes(byte[] content) {
        if (content == null || content.length < 4) {
            return null;
        }
        int b0 = content[0] & 0xFF;
        int b1 = content[1] & 0xFF;
        int b2 = content[2] & 0xFF;
        int b3 = content.length > 3 ? content[3] & 0xFF : -1;

        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) {
            return "image/jpeg";
        }
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) {
            return "image/png";
        }
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) {
            return "image/gif";
        }
        if (b0 == 0x25 && b1 == 0x50 && b2 == 0x44 && b3 == 0x46) {
            return "application/pdf";
        }
        if (content.length >= 12
                && b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
                && (content[8] & 0xFF) == 0x57 && (content[9] & 0xFF) == 0x45
                && (content[10] & 0xFF) == 0x42 && (content[11] & 0xFF) == 0x50) {
            return "image/webp";
        }
        if (b0 == 0x42 && b1 == 0x4D) {
            return "image/bmp";
        }
        if ((b0 == 0x49 && b1 == 0x49 && b2 == 0x2A && b3 == 0x00)
                || (b0 == 0x4D && b1 == 0x4D && b2 == 0x00 && b3 == 0x2A)) {
            return "image/tiff";
        }
        return null;
    }

    private static String contentTypeFromFilename(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        String ext = filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
        return EXTENSION_TO_MIME.get(ext);
    }

    private static final class FileInput {
        final byte[] content;
        final String contentType;
        final String filename;

        FileInput(byte[] content, String contentType, String filename) {
            this.content = content;
            this.contentType = contentType;
            this.filename = filename;
        }
    }
}
