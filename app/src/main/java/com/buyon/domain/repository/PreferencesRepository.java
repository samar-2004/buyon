package com.buyon.domain.repository;

public interface PreferencesRepository {
    boolean isOnboardingCompleted();
    void setOnboardingCompleted(boolean completed);

    /** Returns true = dark mode (default), false = light mode. */
    boolean isDarkMode();
    void setDarkMode(boolean dark);
}
