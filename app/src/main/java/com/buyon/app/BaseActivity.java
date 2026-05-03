package com.buyon.app;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

/**
 * All activities must extend this class.
 *
 * Edge-to-edge contract (one source of truth):
 *  - Enables drawing behind status bar and navigation bar.
 *  - Each subclass is responsible for applying WindowInsets to its own views.
 *  - Fragments must NOT add their own status-bar compensation.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }
}
