package com.buyon.domain.repository;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.model.PaymentMethod;

public interface PaymentRepository {

    /**
     * Records an initiated payment attempt in Firestore and returns a paymentId.
     * This is called *before* redirecting the user to the gateway.
     */
    void initiatePayment(
            String userId,
            double amount,
            PaymentMethod method,
            DomainCallback<String> callback);  // returns paymentId

    /**
     * Called after the gateway signals success (deep-link / WebView redirect).
     * Marks the Firestore payment record as PAID and returns the transaction reference.
     * <p>
     * ⚠️  In production this verification MUST happen server-side via a Cloud Function.
     * The client-side call here is a placeholder for the gateway callback.
     */
    void confirmPayment(
            String paymentId,
            String gatewayTransactionRef,
            DomainCallback<String> callback);  // returns transactionRef

    /** Marks the payment record as FAILED. */
    void failPayment(String paymentId, DomainCallback<Void> callback);
}
