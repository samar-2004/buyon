package com.buyon.data.firebase;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Product;
import com.buyon.domain.repository.WishlistRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore structure:
 *   Preferred: users/{userId}/wishlist/{productId}
 *   Legacy:    wishlist/{userId}/items/{productId}
 */
public final class FirestoreWishlistRepository implements WishlistRepository {

    private static final String USERS = "users";
    private static final String SUB = "wishlist";
    private static final String LEGACY_COLLECTION = "wishlist";
    private static final String LEGACY_SUB = "items";

    private final FirebaseFirestore db;
    private ListenerRegistration reg;

    public FirestoreWishlistRepository(FirebaseFirestore db) { this.db = db; }

    @Override
    public void startWishlistListener(String userId, RepositoryListener<List<String>> listener) {
        stopWishlistListener();
        reg = modernWishlistCollection(userId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        if (isPermissionDenied(e)) {
                            startLegacyWishlistListener(userId, listener);
                        } else {
                            listener.onError(e);
                        }
                        return;
                    }
                    List<String> ids = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) ids.add(doc.getId());
                    }
                    listener.onData(Collections.unmodifiableList(ids));
                });
    }

    @Override
    public void stopWishlistListener() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    @Override
    public void addToWishlist(String userId, Product product, DomainCallback<Void> callback) {
        if (product == null || product.getId() == null || product.getId().isEmpty()) {
            callback.onError(new IllegalArgumentException("Invalid product for wishlist."));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getId());
        data.put("name", product.getName() != null ? product.getName() : "");
        data.put("price", product.getPrice());
        data.put("imageUrl", product.getImageUrl() != null ? product.getImageUrl() : "");
        data.put("timestamp", com.google.firebase.Timestamp.now());
        modernWishlistCollection(userId)
                .document(product.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(
                        error -> {
                            if (!isPermissionDenied(error)) {
                                callback.onError(error);
                                return;
                            }
                            legacyWishlistCollection(userId)
                                    .document(product.getId())
                                    .set(data, SetOptions.merge())
                                    .addOnSuccessListener(unused -> callback.onSuccess(null))
                                    .addOnFailureListener(callback::onError);
                        });
    }

    @Override
    public void removeFromWishlist(String userId, String productId, DomainCallback<Void> callback) {
        modernWishlistCollection(userId)
                .document(productId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(
                        error -> {
                            if (!isPermissionDenied(error)) {
                                callback.onError(error);
                                return;
                            }
                            legacyWishlistCollection(userId)
                                    .document(productId)
                                    .delete()
                                    .addOnSuccessListener(unused -> callback.onSuccess(null))
                                    .addOnFailureListener(callback::onError);
                        });
    }

    @Override
    public void isWishlisted(String userId, String productId, DomainCallback<Boolean> callback) {
        modernWishlistCollection(userId)
                .document(productId)
                .get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.exists()))
                .addOnFailureListener(
                        error -> {
                            if (!isPermissionDenied(error)) {
                                callback.onError(error);
                                return;
                            }
                            legacyWishlistCollection(userId)
                                    .document(productId)
                                    .get()
                                    .addOnSuccessListener(doc -> callback.onSuccess(doc.exists()))
                                    .addOnFailureListener(callback::onError);
                        });
    }

    private void startLegacyWishlistListener(String userId, RepositoryListener<List<String>> listener) {
        stopWishlistListener();
        reg = legacyWishlistCollection(userId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    List<String> ids = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) ids.add(doc.getId());
                    }
                    listener.onData(Collections.unmodifiableList(ids));
                });
    }

    private com.google.firebase.firestore.CollectionReference modernWishlistCollection(String userId) {
        return db.collection(USERS).document(userId).collection(SUB);
    }

    private com.google.firebase.firestore.CollectionReference legacyWishlistCollection(String userId) {
        return db.collection(LEGACY_COLLECTION).document(userId).collection(LEGACY_SUB);
    }

    private static boolean isPermissionDenied(Throwable error) {
        return error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode()
                == FirebaseFirestoreException.Code.PERMISSION_DENIED;
    }
}
