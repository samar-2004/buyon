package com.buyon.domain.repository;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.UserProfile;

public interface UserProfileRepository {
    void startProfileListener(String userId, RepositoryListener<UserProfile> listener);

    void stopProfileListener();

    void saveProfile(UserProfile profile, DomainCallback<Void> callback);

    /** One-shot read of {@code users/{uid}.role} for routing (splash, post-login). */
    void fetchRole(String userId, DomainCallback<String> callback);
}
