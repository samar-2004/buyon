package com.buyon.domain.repository;

import com.buyon.domain.callback.DomainCallback;

public interface AuthRepository {
    boolean isSignedIn();

    String currentUserId();

    String currentUserEmail();

    void signInWithEmail(String email, String password, DomainCallback<Void> callback);

    void registerWithEmail(String email, String password, DomainCallback<Void> callback);

    void signInWithGoogle(String idToken, DomainCallback<Void> callback);

    void signOut();

    void sendPasswordReset(String email, DomainCallback<Void> callback);
}
