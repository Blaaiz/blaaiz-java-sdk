package com.blaaiz.sdk;

import okhttp3.OkHttpClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Top-level facade for the Blaaiz Java SDK.
 *
 * <p>Builds one {@link BlaaizClient} and one instance of every service, and adds composite
 * multi-call workflows ({@link #createCompletePayout}, {@link #createCompleteCollection}) plus a
 * handful of one-line convenience delegations on top.
 */
public class Blaaiz {

    private final BlaaizClient client;

    private final CustomerService customers;
    private final CollectionService collections;
    private final PayoutService payouts;
    private final WalletService wallets;
    private final VirtualBankAccountService virtualBankAccounts;
    private final TransactionService transactions;
    private final BankService banks;
    private final CurrencyService currencies;
    private final FeesService fees;
    private final FileService files;
    private final WebhookService webhooks;
    private final RateService rates;
    private final SwapService swaps;

    public Blaaiz(BlaaizClientOptions options) {
        this(options, null);
    }

    /** Package-private constructor allowing tests to inject a mock/fake transport. */
    Blaaiz(BlaaizClientOptions options, OkHttpClient httpClient) {
        this.client = new BlaaizClient(options, httpClient);

        this.customers = new CustomerService(client);
        this.collections = new CollectionService(client);
        this.payouts = new PayoutService(client);
        this.wallets = new WalletService(client);
        this.virtualBankAccounts = new VirtualBankAccountService(client);
        this.transactions = new TransactionService(client);
        this.banks = new BankService(client);
        this.currencies = new CurrencyService(client);
        this.fees = new FeesService(client);
        this.files = new FileService(client);
        this.webhooks = new WebhookService(client);
        this.rates = new RateService(client);
        this.swaps = new SwapService(client);
    }

    public CustomerService customers() {
        return customers;
    }

    public CollectionService collections() {
        return collections;
    }

    public PayoutService payouts() {
        return payouts;
    }

    public WalletService wallets() {
        return wallets;
    }

    public VirtualBankAccountService virtualBankAccounts() {
        return virtualBankAccounts;
    }

    public TransactionService transactions() {
        return transactions;
    }

    public BankService banks() {
        return banks;
    }

    public CurrencyService currencies() {
        return currencies;
    }

    public FeesService fees() {
        return fees;
    }

    public FileService files() {
        return files;
    }

    public WebhookService webhooks() {
        return webhooks;
    }

    /**
     * FX rate lookups. See {@link RateService}'s Javadoc for the Laravel-only parity caveat.
     */
    public RateService rates() {
        return rates;
    }

    /**
     * Business-wallet swaps. See {@link SwapService}'s Javadoc for the Laravel-only parity caveat.
     */
    public SwapService swaps() {
        return swaps;
    }

    /** {@code true} if {@link CurrencyService#list()} succeeds; swallows any exception, never throws. */
    public boolean testConnection() {
        try {
            currencies.list();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a customer (if {@code config.customerData} is present and {@code config.payoutData}
     * has no {@code customer_id}), fetches a fee breakdown, then initiates the payout.
     *
     * <p><b>Known quirk, preserved for 1:1 parity with all three source SDKs:</b> the fee
     * breakdown request always forwards {@code from_amount}, never {@code to_amount}, even if
     * the caller supplied only {@code to_amount} on {@code payoutData}. In that case
     * {@link FeesService#getBreakdown} will itself reject the call (neither amount present in
     * the forwarded request). This is a latent bug inherited from the Laravel/Node.js/Python
     * SDKs, not a Java-specific issue -- fix at the source-of-truth level (all four SDKs) if it
     * is ever addressed.
     */
    public CompletePayoutResult createCompletePayout(CompletePayoutConfig config) {
        Map<String, Object> customerData = config != null ? config.getCustomerData() : null;
        Map<String, Object> payoutData = config != null ? config.getPayoutData() : null;
        if (payoutData == null) {
            throw new IllegalArgumentException("payoutData is required");
        }

        try {
            String customerId = stringOrNull(payoutData.get("customer_id"));
            if ((customerId == null || customerId.isEmpty()) && customerData != null) {
                customerId = extractNestedId(customers.create(customerData));
            }

            Map<String, Object> feeRequest = new LinkedHashMap<>();
            feeRequest.put("from_currency_id", payoutData.get("from_currency_id"));
            feeRequest.put("to_currency_id", payoutData.get("to_currency_id"));
            feeRequest.put("from_amount", payoutData.get("from_amount"));
            BlaaizResponse feeBreakdown = fees.getBreakdown(feeRequest);

            Map<String, Object> payoutRequest = new LinkedHashMap<>(payoutData);
            payoutRequest.put("customer_id", customerId);
            BlaaizResponse payoutResult = payouts.initiate(payoutRequest);

            return new CompletePayoutResult(customerId, payoutResult.getData(), feeBreakdown.getData());
        } catch (RuntimeException e) {
            throw wrapAsFailure("Complete payout failed: ", e);
        }
    }

    /**
     * Creates a customer (if {@code config.customerData} is present and {@code
     * config.collectionData} has no {@code customer_id}), optionally creates a virtual bank
     * account, then initiates the collection.
     */
    public CompleteCollectionResult createCompleteCollection(CompleteCollectionConfig config) {
        Map<String, Object> customerData = config != null ? config.getCustomerData() : null;
        Map<String, Object> collectionData = config != null ? config.getCollectionData() : null;
        boolean createVba = config != null && config.isCreateVba();
        if (collectionData == null) {
            throw new IllegalArgumentException("collectionData is required");
        }

        try {
            String customerId = stringOrNull(collectionData.get("customer_id"));
            if ((customerId == null || customerId.isEmpty()) && customerData != null) {
                customerId = extractNestedId(customers.create(customerData));
            }

            Object virtualAccountData = null;
            if (createVba) {
                Map<String, Object> vbaRequest = new LinkedHashMap<>();
                vbaRequest.put("wallet_id", collectionData.get("wallet_id"));
                vbaRequest.put("account_name", accountNameFor(customerData));
                virtualAccountData = virtualBankAccounts.create(vbaRequest).getData();
            }

            Map<String, Object> collectionRequest = new LinkedHashMap<>(collectionData);
            collectionRequest.put("customer_id", customerId);
            BlaaizResponse collectionResult = collections.initiate(collectionRequest);

            return new CompleteCollectionResult(customerId, collectionResult.getData(), virtualAccountData);
        } catch (RuntimeException e) {
            throw wrapAsFailure("Complete collection failed: ", e);
        }
    }

    public BlaaizResponse getCustomerById(String customerId) {
        return customers.get(customerId);
    }

    public BlaaizResponse getTransactionById(String transactionId) {
        return transactions.get(transactionId);
    }

    public BlaaizResponse getWalletById(String walletId) {
        return wallets.get(walletId);
    }

    public BlaaizResponse getAllCurrencies() {
        return currencies.list();
    }

    public BlaaizResponse getAllBanks() {
        return banks.list();
    }

    public BlaaizResponse calculateFees(String fromCurrencyId, String toCurrencyId, Object fromAmount) {
        Map<String, Object> feeRequest = new LinkedHashMap<>();
        feeRequest.put("from_currency_id", fromCurrencyId);
        feeRequest.put("to_currency_id", toCurrencyId);
        feeRequest.put("from_amount", fromAmount);
        return fees.getBreakdown(feeRequest);
    }

    private static String accountNameFor(Map<String, Object> customerData) {
        if (customerData == null) {
            return "Customer Account";
        }
        return customerData.get("first_name") + " " + customerData.get("last_name");
    }

    @SuppressWarnings("unchecked")
    private static String extractNestedId(BlaaizResponse customerResponse) {
        Object data = customerResponse.getData();
        if (data instanceof Map) {
            Object nested = ((Map<String, Object>) data).get("data");
            if (nested instanceof Map) {
                Object id = ((Map<String, Object>) nested).get("id");
                if (id != null) {
                    return String.valueOf(id);
                }
            }
        }
        throw new BlaaizException("Unexpected customer creation response shape: missing data.data.id");
    }

    private static String stringOrNull(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * Wraps {@code cause} in a new {@link BlaaizException} whose message is prefixed with
     * {@code prefix}, preserving {@code status}/{@code errorCode} when {@code cause} already
     * carried them (i.e. it was itself a {@link BlaaizException}).
     */
    private static BlaaizException wrapAsFailure(String prefix, RuntimeException cause) {
        if (cause instanceof BlaaizException) {
            BlaaizException be = (BlaaizException) cause;
            return new BlaaizException(prefix + be.getMessage(), be.getStatus(), be.getErrorCode());
        }
        return new BlaaizException(prefix + cause.getMessage());
    }
}
