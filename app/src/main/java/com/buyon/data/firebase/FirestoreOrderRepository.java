package com.buyon.data.firebase;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Order;
import com.buyon.domain.model.OrderLine;
import com.buyon.domain.model.PaymentMethod;
import com.buyon.domain.model.PaymentStatus;
import com.buyon.domain.repository.OrderRepository;
import com.buyon.ui.pricing.OrderPricing;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirestoreOrderRepository implements OrderRepository {

    private static final String COLLECTION = "orders";

    private final FirebaseFirestore db;
    private ListenerRegistration reg;

    public FirestoreOrderRepository(FirebaseFirestore db) { this.db = db; }

    // ── Real-time listener ───────────────────────────────────────────────

    @Override
    public void startOrdersListener(String userId, RepositoryListener<List<Order>> listener) {
        stopOrdersListener();
        reg = db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { listener.onError(e); return; }
                    if (snap == null) { listener.onData(new ArrayList<>()); return; }
                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(FirestoreMappers.orderFromDoc(doc));
                    }
                    Collections.sort(list,
                            (a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    listener.onData(list);
                });
    }

    @Override
    public void stopOrdersListener() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    // ── Place order ──────────────────────────────────────────────────────

    @Override
    public void placeOrder(
            String userId,
            List<OrderLine> lines,
            String shippingAddress,
            String promoCode,
            double shippingFee,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            String paymentReference,
            String customerDisplayName,
            String customerEmail,
            String customerPhone,
            DomainCallback<String> callback) {

        double subtotal = 0;
        for (OrderLine line : lines) subtotal += line.getUnitPrice() * line.getQuantity();

        double discount = OrderPricing.discount(promoCode != null ? promoCode : "", subtotal);
        double total = subtotal - discount + shippingFee;

        Map<String, Object> data = new HashMap<>();
        data.put("userId",           userId);
        data.put("createdAt",        Timestamp.now());
        data.put("subtotal",         subtotal);
        data.put("discount",         discount);
        data.put("shippingFee",      shippingFee);
        data.put("total",            total);
        data.put("shippingAddress",  shippingAddress != null ? shippingAddress : "");
        data.put("promoCode",        promoCode != null ? promoCode : "");
        data.put("status",           Order.Status.PENDING.name());
        data.put("lines",            FirestoreMappers.orderLinesToMaps(lines));

        data.put("customerDisplayName", customerDisplayName != null ? customerDisplayName : "");
        data.put("customerEmail",       customerEmail != null ? customerEmail : "");
        data.put("customerPhone",       customerPhone != null ? customerPhone : "");

        // ── Payment fields ──────────────────────────────────────────────
        data.put("paymentMethod",    paymentMethod  != null ? paymentMethod.name()  : PaymentMethod.COD.name());
        data.put("paymentStatus",    paymentStatus  != null ? paymentStatus.name()  : PaymentStatus.PENDING.name());
        data.put("paymentReference", paymentReference != null ? paymentReference : "");

        db.collection(COLLECTION)
                .add(data)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }
}
