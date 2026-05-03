package com.buyon.data.firebase;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.CartItem;
import com.buyon.domain.repository.CartRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public final class FirestoreCartRepository implements CartRepository {

    private final FirebaseFirestore db;
    private ListenerRegistration reg;

    public FirestoreCartRepository(FirebaseFirestore db) {
        this.db = db;
    }

    private com.google.firebase.firestore.CollectionReference cartCol(String userId) {
        return db.collection("users").document(userId).collection("cart");
    }

    @Override
    public void startCartListener(String userId, RepositoryListener<List<CartItem>> listener) {
        stopCartListener();
        reg =
                cartCol(userId)
                        .addSnapshotListener(
                                (snap, e) -> {
                                    if (e != null) {
                                        listener.onError(e);
                                        return;
                                    }
                                    if (snap == null) {
                                        listener.onData(new ArrayList<>());
                                        return;
                                    }
                                    List<CartItem> list = new ArrayList<>();
                                    for (DocumentSnapshot doc : snap.getDocuments()) {
                                        list.add(FirestoreMappers.cartItemFromDoc(doc));
                                    }
                                    listener.onData(list);
                                });
    }

    @Override
    public void stopCartListener() {
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }

    @Override
    public void addOrUpdateItem(String userId, CartItem item, DomainCallback<Void> callback) {
        cartCol(userId)
                .document(item.getProductId())
                .set(FirestoreMappers.cartItemToMap(item))
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void removeItem(String userId, String productId, DomainCallback<Void> callback) {
        cartCol(userId)
                .document(productId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void clearCart(String userId, DomainCallback<Void> callback) {
        cartCol(userId)
                .get()
                .addOnSuccessListener(
                        snap -> {
                            if (snap.isEmpty()) {
                                callback.onSuccess(null);
                                return;
                            }
                            List<com.google.android.gms.tasks.Task<Void>> tasks = new ArrayList<>();
                            for (DocumentSnapshot d : snap.getDocuments()) {
                                tasks.add(cartCol(userId).document(d.getId()).delete());
                            }
                            com.google.android.gms.tasks.Tasks.whenAll(tasks)
                                    .addOnSuccessListener(unused -> callback.onSuccess(null))
                                    .addOnFailureListener(callback::onError);
                        })
                .addOnFailureListener(callback::onError);
    }
}
