package com.buyon.data.firebase;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Category;
import com.buyon.domain.repository.CategoryRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public final class FirestoreCategoryRepository implements CategoryRepository {

    private static final String COLLECTION = "categories";

    private final FirebaseFirestore db;
    private ListenerRegistration reg;

    public FirestoreCategoryRepository(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void startCategoriesListener(RepositoryListener<List<Category>> listener) {
        stopCategoriesListener();
        reg =
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
                                    List<Category> list = new ArrayList<>();
                                    for (DocumentSnapshot doc : snap.getDocuments()) {
                                        list.add(FirestoreMappers.categoryFromDoc(doc));
                                    }
                                    listener.onData(list);
                                });
    }

    @Override
    public void createCategory(Category category, DomainCallback<String> callback) {
        if (category == null
                || category.getId() == null
                || category.getId().trim().isEmpty()
                || category.getName() == null
                || category.getName().trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Category name is required."));
            return;
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("name", category.getName().trim());
        data.put("iconName", category.getIconName() != null ? category.getIconName() : "");
        db.collection(COLLECTION)
                .document(category.getId().trim())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(category.getId().trim()))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void stopCategoriesListener() {
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }
}
