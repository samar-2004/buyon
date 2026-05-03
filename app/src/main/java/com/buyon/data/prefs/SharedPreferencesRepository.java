package com.buyon.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.buyon.domain.repository.PreferencesRepository;

public final class SharedPreferencesRepository implements PreferencesRepository {

    private static final String PREFS = "buyon_prefs";
    private static final String KEY_ONBOARDING = "onboarding_done";
    private static final String KEY_DARK_MODE = "dark_mode";

    private final SharedPreferences prefs;

    public SharedPreferencesRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING, false);
    }

    @Override
    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_ONBOARDING, completed).apply();
    }

    @Override
    public boolean isDarkMode() {
        // Default true: dark mode on first install
        return prefs.getBoolean(KEY_DARK_MODE, true);
    }

    @Override
    public void setDarkMode(boolean dark) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply();
    }
}
