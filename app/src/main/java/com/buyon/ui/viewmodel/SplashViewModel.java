package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.model.UserProfile;
import com.buyon.domain.repository.AuthRepository;
import com.buyon.domain.repository.PreferencesRepository;
import com.buyon.domain.repository.UserProfileRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SplashViewModel extends ViewModel {

    public enum Route {
        ONBOARDING,
        LOGIN,
        /** Consumer home (bottom nav). */
        MAIN,
        /** Admin dashboard activity. */
        ADMIN
    }

    private final AuthRepository authRepository;
    private final PreferencesRepository preferencesRepository;
    private final UserProfileRepository userProfileRepository;
    private final MutableLiveData<Route> route = new MutableLiveData<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public SplashViewModel(
            AuthRepository authRepository,
            PreferencesRepository preferencesRepository,
            UserProfileRepository userProfileRepository) {
        this.authRepository = authRepository;
        this.preferencesRepository = preferencesRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public LiveData<Route> getRoute() {
        return route;
    }

    public void scheduleRoute() {
        executor.schedule(
                () -> {
                    if (!preferencesRepository.isOnboardingCompleted()) {
                        route.postValue(Route.ONBOARDING);
                        return;
                    }
                    if (!authRepository.isSignedIn()) {
                        route.postValue(Route.LOGIN);
                        return;
                    }
                    String uid = authRepository.currentUserId();
                    if (uid == null) {
                        route.postValue(Route.LOGIN);
                        return;
                    }
                    userProfileRepository.fetchRole(uid, new DomainCallback<String>() {
                        @Override
                        public void onSuccess(String role) {
                            // Keep splash routing aligned with Firestore rules: admin role is required.
                            if (UserProfile.ROLE_ADMIN.equals(role)) {
                                route.postValue(Route.ADMIN);
                            } else {
                                route.postValue(Route.MAIN);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            // If role lookup fails (e.g. rules/network), don't route to admin blindly.
                            route.postValue(Route.MAIN);
                        }
                    });
                },
                900,
                TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
