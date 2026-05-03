package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.core.resource.Resource;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.UserProfile;
import com.buyon.domain.repository.AuthRepository;
import com.buyon.domain.repository.UserProfileRepository;

public final class ProfileViewModel extends ViewModel {

    private final UserProfileRepository userProfileRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> saveState = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProfileViewModel(UserProfileRepository userProfileRepository, AuthRepository authRepository) {
        this.userProfileRepository = userProfileRepository;
        this.authRepository = authRepository;
        String uid = authRepository.currentUserId();
        if (uid != null) {
            userProfileRepository.startProfileListener(
                    uid,
                    new RepositoryListener<UserProfile>() {
                        @Override
                        public void onData(UserProfile data) {
                            profile.postValue(data);
                        }

                        @Override
                        public void onError(Throwable error) {
                            errorMessage.postValue(error.getMessage());
                        }
                    });
        }
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public LiveData<Resource<Void>> getSaveState() {
        return saveState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void save(String displayName, String phone, String address) {
        String uid = authRepository.currentUserId();
        if (uid == null) {
            return;
        }
        UserProfile current = profile.getValue();
        String email = authRepository.currentUserEmail();
        if (email == null || email.isEmpty()) {
            email = current != null ? current.getEmail() : "";
        }
        UserProfile p =
                new UserProfile(uid, displayName, email, phone, address);
        saveState.setValue(Resource.loading());
        userProfileRepository.saveProfile(
                p,
                new DomainCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        saveState.postValue(Resource.success(null));
                    }

                    @Override
                    public void onError(Throwable error) {
                        saveState.postValue(Resource.error(error));
                    }
                });
    }

    public void signOut() {
        authRepository.signOut();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userProfileRepository.stopProfileListener();
    }
}
