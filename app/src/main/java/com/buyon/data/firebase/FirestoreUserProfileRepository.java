package com.buyon.data.firebase;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.UserProfile;
import com.buyon.domain.repository.UserProfileRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

public final class FirestoreUserProfileRepository implements UserProfileRepository {

    private final FirebaseFirestore db;
    private ListenerRegistration reg;

    public FirestoreUserProfileRepository(FirebaseFirestore db) { this.db = db; }

    @Override
    public void startProfileListener(String userId, RepositoryListener<UserProfile> listener) {
        stopProfileListener();
        reg = db.collection("users")
                .document(userId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { listener.onError(e); return; }
                    if (snap == null || !snap.exists()) {
                        listener.onData(new UserProfile(userId, "", "", "", "", UserProfile.ROLE_USER));
                        return;
                    }
                    listener.onData(FirestoreMappers.profileFromDoc(snap, userId));
                });
    }

    @Override
    public void stopProfileListener() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    @Override
    public void saveProfile(UserProfile profile, DomainCallback<Void> callback) {
        db.collection("users")
                .document(profile.getUid())
                .set(FirestoreMappers.profileToMap(profile), SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void fetchRole(String userId, DomainCallback<String> callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || !snap.exists()) {
                        callback.onSuccess(UserProfile.ROLE_USER);
                        return;
                    }
                    String role = snap.getString("role");
                    callback.onSuccess(role != null ? role : UserProfile.ROLE_USER);
                })
                .addOnFailureListener(callback::onError);
    }
}
