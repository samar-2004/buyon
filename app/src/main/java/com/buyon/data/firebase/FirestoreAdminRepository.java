package com.buyon.data.firebase;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Order;
import com.buyon.domain.model.Product;
import com.buyon.domain.model.UserProfile;
import com.buyon.domain.repository.AdminRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirestoreAdminRepository implements AdminRepository {

    private static final String PRODUCTS = "products";
    private static final String ORDERS = "orders";
    private static final String USERS = "users";

    private final FirebaseFirestore db;
    private ListenerRegistration productsReg;
    private ListenerRegistration ordersReg;
    private ListenerRegistration usersReg;

    public FirestoreAdminRepository(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void listenAllProducts(RepositoryListener<List<Product>> listener) {
        if (productsReg != null) productsReg.remove();
        productsReg = db.collection(PRODUCTS).addSnapshotListener((snap, e) -> {
            if (e != null) { listener.onError(e); return; }
            if (snap == null) { listener.onData(new ArrayList<>()); return; }
            List<Product> list = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                list.add(FirestoreMappers.productFromDoc(doc));
            }
            listener.onData(list);
        });
    }

    @Override
    public void listenAllOrders(RepositoryListener<List<Order>> listener) {
        if (ordersReg != null) ordersReg.remove();
        ordersReg = db.collection(ORDERS)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { listener.onError(e); return; }
                    if (snap == null) { listener.onData(new ArrayList<>()); return; }
                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        list.add(FirestoreMappers.orderFromDoc(doc));
                    }
                    // Sort client-side by newest first
                    list.sort((a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
                    listener.onData(list);
                });
    }

    @Override
    public void listenAllUsers(RepositoryListener<List<UserProfile>> listener) {
        if (usersReg != null) usersReg.remove();
        usersReg = db.collection(USERS).addSnapshotListener((snap, e) -> {
            if (e != null) { listener.onError(e); return; }
            if (snap == null) { listener.onData(new ArrayList<>()); return; }
            List<UserProfile> list = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                list.add(FirestoreMappers.profileFromDoc(doc, doc.getId()));
            }
            listener.onData(list);
        });
    }

    @Override
    public void saveProduct(Product product, DomainCallback<String> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", product.getName());
        data.put("description", product.getDescription());
        data.put("price", product.getPrice());
        data.put("imageUrl", product.getImageUrl() != null ? product.getImageUrl() : "");
        data.put("categoryId", product.getCategoryId() != null ? product.getCategoryId() : "");
        data.put("rating", product.getRating());
        data.put("soldCount", product.getSoldCount());
        data.put("locationLabel", product.getLocationLabel() != null ? product.getLocationLabel() : "");

        if (product.getId() != null && !product.getId().isEmpty()) {
            // Update existing
            db.collection(PRODUCTS).document(product.getId())
                    .set(data)
                    .addOnSuccessListener(v -> callback.onSuccess(product.getId()))
                    .addOnFailureListener(callback::onError);
        } else {
            // Create new
            db.collection(PRODUCTS).add(data)
                    .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                    .addOnFailureListener(callback::onError);
        }
    }

    @Override
    public void deleteProduct(String productId, DomainCallback<Void> callback) {
        db.collection(PRODUCTS).document(productId)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void updateOrderStatus(
            String orderId,
            String status,
            String cancellationReason,
            DomainCallback<Void> callback) {
        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(db.collection(ORDERS).document(orderId));
                    String currentStatus = snapshot.getString("status");
                    Order.Status from = parseOrderStatus(currentStatus);
                    Order.Status to = parseOrderStatus(status);

                    if (!isTransitionAllowed(from, to)) {
                        throw new IllegalStateException("Invalid status transition from " + from + " to " + to);
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("status", to.name());
                    if (to == Order.Status.CANCELLED) {
                        String reason = cancellationReason != null ? cancellationReason.trim() : "";
                        if (reason.isEmpty()) {
                            throw new IllegalArgumentException("Cancellation reason is required.");
                        }
                        update.put("cancellationReason", reason);
                        update.put("cancelledAt", FieldValue.serverTimestamp());
                    } else {
                        update.put("cancellationReason", "");
                        update.put("cancelledAt", null);
                    }
                    transaction.update(db.collection(ORDERS).document(orderId), update);
                    return null;
                })
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    private static Order.Status parseOrderStatus(String status) {
        if (status == null) return Order.Status.PENDING;
        try {
            return Order.Status.valueOf(status);
        } catch (IllegalArgumentException ignore) {
            return Order.Status.PENDING;
        }
    }

    private static boolean isTransitionAllowed(Order.Status from, Order.Status to) {
        if (from == to) return true;
        switch (from) {
            case PENDING:
                return to == Order.Status.PROCESSING || to == Order.Status.CANCELLED;
            case PROCESSING:
                return to == Order.Status.SHIPPED || to == Order.Status.CANCELLED;
            case SHIPPED:
                return to == Order.Status.DELIVERED || to == Order.Status.CANCELLED;
            case DELIVERED:
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }

    @Override
    public void stopListening() {
        if (productsReg != null) { productsReg.remove(); productsReg = null; }
        if (ordersReg != null) { ordersReg.remove(); ordersReg = null; }
        if (usersReg != null) { usersReg.remove(); usersReg = null; }
    }
}
