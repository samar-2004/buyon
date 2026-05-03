package com.buyon.data.firebase;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Product;
import com.buyon.domain.repository.ProductRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FirestoreProductRepository implements ProductRepository {

    private static final String COLLECTION = "products";

    private final FirebaseFirestore db;
    private ListenerRegistration productsReg;
    private ListenerRegistration productReg;
    private ListenerRegistration categoryReg;

    public FirestoreProductRepository(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void startProductsListener(RepositoryListener<List<Product>> listener) {
        stopProductsListener();
        productsReg =
                db.collection(COLLECTION)
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
                                    List<Product> list = new ArrayList<>();
                                    for (DocumentSnapshot doc : snap.getDocuments()) {
                                        list.add(FirestoreMappers.productFromDoc(doc));
                                    }
                                    listener.onData(list);
                                });
    }

    @Override
    public void stopProductsListener() {
        if (productsReg != null) {
            productsReg.remove();
            productsReg = null;
        }
    }

    @Override
    public void startProductListener(String productId, RepositoryListener<Product> listener) {
        stopProductListener();
        productReg =
                db.collection(COLLECTION)
                        .document(productId)
                        .addSnapshotListener(
                                (snap, e) -> {
                                    if (e != null) {
                                        listener.onError(e);
                                        return;
                                    }
                                    if (snap == null || !snap.exists()) {
                                        listener.onError(new IllegalStateException("Product not found"));
                                        return;
                                    }
                                    listener.onData(FirestoreMappers.productFromDoc(snap));
                                });
    }

    @Override
    public void stopProductListener() {
        if (productReg != null) {
            productReg.remove();
            productReg = null;
        }
    }

    @Override
    public void searchProducts(String query, DomainCallback<List<Product>> callback) {
        String q = query != null ? query.trim().toLowerCase(Locale.US) : "";
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(
                        snap -> {
                            List<Product> out = new ArrayList<>();
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                Product p = FirestoreMappers.productFromDoc(doc);
                                if (q.isEmpty()
                                        || p.getName().toLowerCase(Locale.US).contains(q)
                                        || p.getDescription().toLowerCase(Locale.US).contains(q)) {
                                    out.add(p);
                                }
                            }
                            callback.onSuccess(out);
                        })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void startProductsByCategoryListener(
            String categoryId, RepositoryListener<List<Product>> listener) {
        stopCategoryListener();
        Query query = db.collection(COLLECTION).whereEqualTo("categoryId", categoryId);
        categoryReg =
                query.addSnapshotListener(
                        (snap, e) -> {
                            if (e != null) {
                                listener.onError(e);
                                return;
                            }
                            if (snap == null) {
                                listener.onData(new ArrayList<>());
                                return;
                            }
                            List<Product> list = new ArrayList<>();
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                list.add(FirestoreMappers.productFromDoc(doc));
                            }
                            listener.onData(list);
                        });
    }

    @Override
    public void stopCategoryListener() {
        if (categoryReg != null) {
            categoryReg.remove();
            categoryReg = null;
        }
    }

    @Override
    public void fetchProductsByIds(List<String> productIds, DomainCallback<List<Product>> callback) {
        if (productIds == null || productIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        // Firestore "in" query supports up to 30 IDs per call.
        List<String> batch = productIds.size() > 30 ? productIds.subList(0, 30) : productIds;
        db.collection(COLLECTION)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Product> out = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            out.add(FirestoreMappers.productFromDoc(doc));
                        }
                    }
                    callback.onSuccess(out);
                })
                .addOnFailureListener(callback::onError);
    }
}
