package com.blaaiz.sdk.examples;

import com.blaaiz.sdk.Blaaiz;
import com.blaaiz.sdk.BlaaizException;
import com.blaaiz.sdk.UploadFileCompleteResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Uploads a KYC document with {@code uploadFileComplete}, which does all three steps: it gets
 * the pre-signed URL, uploads the file to S3, and attaches the file to the customer.
 *
 * <p>The {@code file} option accepts a {@code byte[]}, a base64 string, a data URL, or a public
 * {@code http(s)} URL. Each method below shows one of them.
 */
public final class UploadKycExample {

    private UploadKycExample() {
    }

    public static void main(String[] args) throws IOException {
        Blaaiz blaaiz = ExampleClient.create();
        String customerId = ExampleClient.requireEnv("BLAAIZ_TEST_CUSTOMER_ID");

        if (args.length < 1) {
            System.err.println("Usage: UploadKycExample <path-to-document>");
            return;
        }

        try {
            UploadFileCompleteResult result = fromFile(blaaiz, customerId, Path.of(args[0]));
            System.out.println("File ID: " + result.getFileId());
            System.out.println("Presigned URL: " + result.getPresignedUrl());
            System.out.println("Association status: " + result.getAssociationResponse().getStatus());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid input: " + e.getMessage());
        } catch (BlaaizException e) {
            System.err.println("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Uploads a file from disk. The SDK detects the content type from the magic bytes, so
     * {@code content_type} is optional here.
     */
    static UploadFileCompleteResult fromFile(Blaaiz blaaiz, String customerId, Path path) throws IOException {
        byte[] content = Files.readAllBytes(path);

        return blaaiz.customers().uploadFileComplete(customerId, Map.of(
                "file", content,
                "file_category", "identity",
                "filename", path.getFileName().toString()));
    }

    /** Uploads a file from a plain base64 string. */
    static UploadFileCompleteResult fromBase64(Blaaiz blaaiz, String customerId, String base64) {
        return blaaiz.customers().uploadFileComplete(customerId, Map.of(
                "file", base64,
                "file_category", "identity",
                "content_type", "image/jpeg"));
    }

    /** Uploads a file from a data URL. The SDK reads the content type from the prefix. */
    static UploadFileCompleteResult fromDataUrl(Blaaiz blaaiz, String customerId, String dataUrl) {
        return blaaiz.customers().uploadFileComplete(customerId, Map.of(
                "file", dataUrl,
                "file_category", "proof_of_address"));
    }

    /**
     * Uploads a file from a public URL, for example a pre-signed S3 link. The SDK downloads the
     * file, follows any redirect, and reads the filename from the Content-Disposition header.
     */
    static UploadFileCompleteResult fromPublicUrl(Blaaiz blaaiz, String customerId, String url) {
        return blaaiz.customers().uploadFileComplete(customerId, Map.of(
                "file", url,
                "file_category", "identity"));
    }

    /**
     * Does the same work in three separate steps, when you want to control the S3 upload
     * yourself. Step 2 is your own HTTP PUT to {@code presigned.getData()} at the {@code url}
     * key.
     */
    static void manualThreeStep(Blaaiz blaaiz, String customerId, String fileId) {
        blaaiz.files().getPresignedUrl(Map.of(
                "customer_id", customerId,
                "file_category", "identity"));

        // Step 2: PUT the file bytes to the pre-signed URL with your own HTTP client.

        blaaiz.customers().uploadFiles(customerId, Map.of("id_file", fileId));
    }
}
