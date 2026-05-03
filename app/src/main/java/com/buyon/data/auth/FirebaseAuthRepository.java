package com.buyon.data.auth;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.repository.AuthRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public final class FirebaseAuthRepository implements AuthRepository {

    private final FirebaseAuth auth;

    public FirebaseAuthRepository(FirebaseAuth auth) {
        this.auth = auth;
        this.auth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
    }

    @Override
    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    @Override
    public String currentUserId() {
        FirebaseUser u = auth.getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    @Override
    public String currentUserEmail() {
        FirebaseUser u = auth.getCurrentUser();
        return u != null ? u.getEmail() : null;
    }

    @Override
    public void signInWithEmail(String email, String password, DomainCallback<Void> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void registerWithEmail(String email, String password, DomainCallback<Void> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void signInWithGoogle(String idToken, DomainCallback<Void> callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(r -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void signOut() {
        auth.signOut();
    }

    @Override
    public void sendPasswordReset(String email, DomainCallback<Void> callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
