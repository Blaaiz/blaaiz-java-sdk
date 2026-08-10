package com.blaaiz.sdk.examples;

import com.blaaiz.sdk.Blaaiz;
import com.blaaiz.sdk.BlaaizException;
import com.blaaiz.sdk.BlaaizResponse;

import java.util.Map;

/**
 * Creates a customer, reads it back, and lists the customers with a filter.
 *
 * <p>The API wraps the created customer in a second {@code data} key, so the identifier is at
 * {@code data.data.id}. {@link #extractCustomerId} shows how to read it.
 */
public final class CreateCustomerExample {

    private CreateCustomerExample() {
    }

    public static void main(String[] args) {
        Blaaiz blaaiz = ExampleClient.create();

        try {
            BlaaizResponse created = blaaiz.customers().create(Map.of(
                    "first_name", "John",
                    "last_name", "Doe",
                    "type", "individual",
                    "email", "john.doe@example.com",
                    "country", "NG",
                    "id_type", "passport",
                    "id_number", "A12345678"));

            String customerId = extractCustomerId(created);
            System.out.println("Created customer: " + customerId);

            BlaaizResponse fetched = blaaiz.customers().get(customerId);
            System.out.println("Fetched customer: " + fetched.getData());

            BlaaizResponse updated = blaaiz.customers().update(customerId, Map.of(
                    "first_name", "Jane"));
            System.out.println("Updated customer: " + updated.getStatus());

            // Filters are optional. Pass null to list every customer.
            BlaaizResponse verified = blaaiz.customers().list(Map.of(
                    "verification_status", "VERIFIED",
                    "type", "individual",
                    "paginate", true));
            System.out.println("Verified customers: " + verified.getData());

            BlaaizResponse beneficiaries = blaaiz.customers().listBeneficiaries(customerId);
            System.out.println("Beneficiaries: " + beneficiaries.getData());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid input: " + e.getMessage());
        } catch (BlaaizException e) {
            System.err.println("Request failed: " + e.getMessage());
            System.err.println("Status: " + e.getStatus() + " code: " + e.getErrorCode());
        }
    }

    /** Reads {@code data.data.id} from a customer-creation response. */
    @SuppressWarnings("unchecked")
    static String extractCustomerId(BlaaizResponse response) {
        Map<String, Object> body = (Map<String, Object>) response.getData();
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return String.valueOf(data.get("id"));
    }
}
