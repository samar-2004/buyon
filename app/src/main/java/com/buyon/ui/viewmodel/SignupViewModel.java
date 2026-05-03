package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.core.resource.Resource;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.model.UserProfile;
import com.buyon.domain.repository.AuthRepository;
import com.buyon.domain.repository.UserProfileRepository;

public final class SignupViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserProfileRepository userProfileRepository;
    private final MutableLiveData<Resource<Void>> state = new MutableLiveData<>();

    public SignupViewModel(AuthRepository authRepository,
                           UserProfileRepository userProfileRepository) {
        this.authRepository       = authRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public LiveData<Resource<Void>> getState() { return state; }

    public void register(String email, String password, String displayName) {
        state.setValue(Resource.loading());
        authRepository.registerWithEmail(email, password, new DomainCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                String uid = authRepository.currentUserId();
                if (uid == null) {
                    state.postValue(Resource.error(new IllegalStateException("No user after registration")));
                    return;
                }
                // Always write role:"user" on first registration.
                // Admins are promoted via the Firebase console or a Cloud Function.
                UserProfile profile = new UserProfile(
                        uid, displayName, email, "", "", UserProfile.ROLE_USER);
                userProfileRepository.saveProfile(profile, new DomainCallback<Void>() {
                    @Override public void onSuccess(Void r) {
                        state.postValue(Resource.success(null));
                    }
                    @Override public void onError(Throwable error) {
                        state.postValue(Resource.error(error));
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                state.postValue(Resource.error(error));
            }
        });
    }
}
