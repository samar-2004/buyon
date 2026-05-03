package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.core.resource.Resource;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.repository.AuthRepository;

public final class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;

    /** Drives the sign-in / Google sign-in loading + result UI. */
    private final MutableLiveData<Resource<Void>> signInState = new MutableLiveData<>();

    /** Drives the password-reset flow independently so observers don't clash. */
    private final MutableLiveData<Resource<Void>> resetPasswordState = new MutableLiveData<>();

    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Resource<Void>> getSignInState() {
        return signInState;
    }

    public LiveData<Resource<Void>> getResetPasswordState() {
        return resetPasswordState;
    }

    public void signIn(String email, String password) {
        signInState.setValue(Resource.loading());
        authRepository.signInWithEmail(
                email,
                password,
                new DomainCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        signInState.postValue(Resource.success(null));
                    }

                    @Override
                    public void onError(Throwable error) {
                        signInState.postValue(Resource.error(error));
                    }
                });
    }

    public void resetPassword(String email) {
        resetPasswordState.setValue(Resource.loading());
        authRepository.sendPasswordReset(
                email,
                new DomainCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        resetPasswordState.postValue(Resource.success(null));
                    }

                    @Override
                    public void onError(Throwable error) {
                        resetPasswordState.postValue(Resource.error(error));
                    }
                });
    }

    public void signInWithGoogle(String idToken) {
        signInState.setValue(Resource.loading());
        authRepository.signInWithGoogle(
                idToken,
                new DomainCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        signInState.postValue(Resource.success(null));
                    }

                    @Override
                    public void onError(Throwable error) {
                        signInState.postValue(Resource.error(error));
                    }
                });
    }
}
