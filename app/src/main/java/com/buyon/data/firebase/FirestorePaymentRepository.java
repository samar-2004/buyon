package com.buyon.data.firebase;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.model.PaymentMethod;
import com.buyon.domain.model.PaymentStatus;
import com.buyon.domain.repository.PaymentRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore structure:
 *   payments/{paymentId}:
 *     userId        : String
 *     amount        : Double
 *     method        : String  ("JAZZCASH" | "EASYPAISA")
 *     status        : String  ("PENDING" | "PAID" | "FAILED")
 *     transactionRef: String
 *     createdAt     : Timestamp
 *     updatedAt     : Timestamp
 *
 * ⚠️  Security note: in production, payment confirmation (confirmPayment) must be
 * performed server-side via a Cloud Function that verifies the gateway signature.
 * The client only calls confirmPayment after receiving a deep-link / WebView redirect
 * that includes the gateway's signed response.
 */
public final class FirestorePaymentRepository implements PaymentRepository {

    private static final String COLLECTION = "payments";

    private final FirebaseFirestore db;

    public FirestorePaymentRepository(FirebaseFirestore db) { this.db = db; }

    @Override
    public void initiatePayment(
            String userId,
            double amount,
            PaymentMethod method,
            DomainCallback<String> callback) {

        Map<String, Object> data = new HashMap<>();
        data.put("userId",         userId);
        data.put("amount",         amount);
        data.put("method",         method.name());
        data.put("status",         PaymentStatus.PENDING.name());
        data.put("transactionRef", "");
        data.put("createdAt",      Timestamp.now());
        data.put("updatedAt",      Timestamp.now());

        db.collection(COLLECTION)
                .add(data)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void confirmPayment(
            String paymentId,
            String gatewayTransactionRef,
            DomainCallback<String> callback) {

        Map<String, Object> update = new HashMap<>();
        update.put("status",         PaymentStatus.PAID.name());
        update.put("transactionRef", gatewayTransactionRef);
        update.put("updatedAt",      Timestamp.now());

        db.collection(COLLECTION)
                .document(paymentId)
                .update(update)
                .addOnSuccessListener(unused -> callback.onSuccess(gatewayTransactionRef))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void failPayment(String paymentId, DomainCallback<Void> callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("status",    PaymentStatus.FAILED.name());
        update.put("updatedAt", Timestamp.now());

        db.collection(COLLECTION)
                .document(paymentId)
                .update(update)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
