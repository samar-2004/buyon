package com.buyon.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.buyon.app.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public final class MainActivity extends BaseActivity {

    public static final String EXTRA_OPEN_PRODUCT_ID = "openProductId";

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Manifest uses Theme.Buyon.Splash for Android 12+ system splash only;
        // inflate the UI with the real app theme.
        setTheme(R.style.Theme_Buyon);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowInsets();
        setupNavigation();
        handleIntentNavigation(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentNavigation(intent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Window insets — applied ONCE here; fragments must NOT apply them again.
    // ─────────────────────────────────────────────────────────────────────────

    private void setupWindowInsets() {

        // navHostFragment: do NOT consume the status bar inset here — let each
        // fragment's root handle it via android:fitsSystemWindows="true" so the
        // header/AppBar background extends seamlessly behind the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment, (v, insets) -> insets);

        // bottomNav: apply navigation-bar height as extra paddingBottom so the
        // nav items sit above the gesture / button bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav, (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    sysBars.bottom);
            return insets;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    private void setupNavigation() {
        NavHostFragment navHost =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
        if (navHost == null) return;

        NavController navController = navHost.getNavController();
        this.navController = navController;
        BottomNavigationView bottomNav = binding.bottomNav;
        NavigationUI.setupWithNavController(bottomNav, navController);

        navController.addOnDestinationChangedListener(
                (controller, destination, arguments) -> {
                    int id = destination.getId();
                    boolean hideNav =
                            id == R.id.splashFragment
                                    || id == R.id.onboardingFragment
                                    || id == R.id.loginFragment
                                    || id == R.id.signupFragment
                                    || id == R.id.productDetailFragment
                                    || id == R.id.checkoutFragment
                                    || id == R.id.paymentFragment
                                    || id == R.id.orderConfirmationFragment;

                    if (hideNav && bottomNav.getVisibility() == View.VISIBLE) {
                        animateBottomNavOut(bottomNav);
                    } else if (!hideNav && bottomNav.getVisibility() != View.VISIBLE) {
                        animateBottomNavIn(bottomNav);
                    }
                });
    }

    private void handleIntentNavigation(Intent intent) {
        if (intent == null || navController == null) {
            return;
        }
        String productId = intent.getStringExtra(EXTRA_OPEN_PRODUCT_ID);
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }

        Bundle args = new Bundle();
        args.putString("productId", productId);

        int destinationId = navController.getCurrentDestination() != null
                ? navController.getCurrentDestination().getId()
                : -1;
        if (destinationId != R.id.homeFragment) {
            binding.bottomNav.setSelectedItemId(R.id.homeFragment);
        }
        binding.navHostFragment.post(
                () -> {
                    try {
                        navController.navigate(R.id.productDetailFragment, args);
                    } catch (IllegalArgumentException ignore) {
                        // If graph state is not ready yet, skip this one-shot navigation.
                    }
                });

        intent.removeExtra(EXTRA_OPEN_PRODUCT_ID);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BottomNav show / hide animations
    // ─────────────────────────────────────────────────────────────────────────

    private void animateBottomNavOut(View view) {
        view.animate()
                .translationY(view.getHeight() + 200f)
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> view.setVisibility(View.GONE))
                .start();
    }

    private void animateBottomNavIn(View view) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(view.getHeight() + 200f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
