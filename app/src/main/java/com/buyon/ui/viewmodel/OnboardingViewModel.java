package com.buyon.ui.viewmodel;

import androidx.lifecycle.ViewModel;

import com.buyon.domain.repository.PreferencesRepository;

public final class OnboardingViewModel extends ViewModel {

    private final PreferencesRepository preferencesRepository;

    public OnboardingViewModel(PreferencesRepository preferencesRepository) {
        this.preferencesRepository = preferencesRepository;
    }

    public void completeOnboarding() {
        preferencesRepository.setOnboardingCompleted(true);
    }
}
