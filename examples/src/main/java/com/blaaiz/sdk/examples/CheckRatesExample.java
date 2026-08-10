package com.blaaiz.sdk.examples;

import com.blaaiz.sdk.Blaaiz;
import com.blaaiz.sdk.BlaaizException;
import com.blaaiz.sdk.BlaaizResponse;

/**
 * Checks the connection, then reads the currencies and the FX rates.
 *
 * <p>Run it with:
 * {@code mvn -f examples/pom.xml exec:java -Dexec.mainClass=com.blaaiz.sdk.examples.CheckRatesExample}
 */
public final class CheckRatesExample {

    private CheckRatesExample() {
    }

    public static void main(String[] args) {
        Blaaiz blaaiz = ExampleClient.create();

        if (!blaaiz.testConnection()) {
            System.err.println("Cannot reach the Blaaiz API. Check your credentials and base URL.");
            return;
        }
        System.out.println("Connected to the Blaaiz API.");

        try {
            BlaaizResponse currencies = blaaiz.currencies().list();
            System.out.println("Currencies: " + currencies.getData());

            // Pass null to list every rate, or a currency code to filter the list.
            BlaaizResponse allRates = blaaiz.rates().list(null);
            System.out.println("All rates: " + allRates.getData());

            BlaaizResponse ngnRates = blaaiz.rates().list("NGN");
            System.out.println("NGN rates: " + ngnRates.getData());
        } catch (BlaaizException e) {
            System.err.println("Request failed: " + e.getMessage());
            System.err.println("Status: " + e.getStatus() + " code: " + e.getErrorCode());
        }
    }
}
