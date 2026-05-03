package com.buyon.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.buyon.core.di.AppDependencies;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.app.R;
import com.buyon.app.databinding.FragmentSplashBinding;
import com.buyon.ui.admin.AdminActivity;
import com.buyon.ui.viewmodel.SplashViewModel;

public final class SplashFragment extends Fragment {

    private FragmentSplashBinding binding;
    private AnimatorSet entryAnimators;
    private final List<Animator> dotAnimators = new ArrayList<>();
    private boolean routeNavigated = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSplashBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        startEntryAnimations();

        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        SplashViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(SplashViewModel.class);

        vm.getRoute().observe(getViewLifecycleOwner(), route -> {
            if (route == null || routeNavigated) return;
            routeNavigated = true;

            // Short pause so the user sees the settled state, then exit-animate and navigate.
            view.postDelayed(() -> {
                if (!isAdded()) return;
                playExitAndNavigate(view, route);
            }, 250);
        });

        vm.scheduleRoute();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Entry animations
    // ─────────────────────────────────────────────────────────────────────

    private void startEntryAnimations() {
        View glow    = binding.glowBg;
        View logo    = binding.logoImage;
        View tagline = binding.taglineText;
        View loader  = binding.loadingIndicator;
        View dot1    = binding.dot1;
        View dot2    = binding.dot2;
        View dot3    = binding.dot3;

        // ── Initial state ──────────────────────────────────────────────
        glow.setAlpha(0f);
        glow.setScaleX(0.2f);
        glow.setScaleY(0.2f);

        logo.setAlpha(0f);
        logo.setScaleX(0.15f);
        logo.setScaleY(0.15f);
        logo.setTranslationY(20f);

        tagline.setAlpha(0f);
        tagline.setTranslationY(28f);

        loader.setAlpha(0f);
        loader.setTranslationY(12f);

        // ── Phase 1: Glow orb blooms in (0 ms) ────────────────────────
        AnimatorSet glowIn = new AnimatorSet();
        glowIn.playTogether(
                ObjectAnimator.ofFloat(glow, "alpha",  0f, 0.28f),
                ObjectAnimator.ofFloat(glow, "scaleX", 0.2f, 1f),
                ObjectAnimator.ofFloat(glow, "scaleY", 0.2f, 1f)
        );
        glowIn.setDuration(1100);
        glowIn.setInterpolator(new AccelerateDecelerateInterpolator());

        // ── Phase 2: Logo dramatic entrance (150 ms) ──────────────────
        AnimatorSet logoIn = new AnimatorSet();
        logoIn.playTogether(
                ObjectAnimator.ofFloat(logo, "alpha",        0f,   1f),
                ObjectAnimator.ofFloat(logo, "scaleX",       0.15f, 1f),
                ObjectAnimator.ofFloat(logo, "scaleY",       0.15f, 1f),
                ObjectAnimator.ofFloat(logo, "translationY", 20f,   0f)
        );
        logoIn.setDuration(850);
        logoIn.setInterpolator(new OvershootInterpolator(1.6f));
        logoIn.setStartDelay(150);

        // ── Phase 3: Logo heartbeat pulse (plays after entry settles) ──
        AnimatorSet logoPulse = new AnimatorSet();
        logoPulse.playTogether(
                ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.07f, 1f),
                ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.07f, 1f)
        );
        logoPulse.setDuration(1000);
        logoPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        logoPulse.setStartDelay(1100);

        // ── Phase 4: Tagline slides up (700 ms, right after logo lands) ─
        AnimatorSet tagIn = new AnimatorSet();
        tagIn.playTogether(
                ObjectAnimator.ofFloat(tagline, "alpha",        0f,  1f),
                ObjectAnimator.ofFloat(tagline, "translationY", 28f, 0f)
        );
        tagIn.setDuration(500);
        tagIn.setInterpolator(new DecelerateInterpolator(2f));
        tagIn.setStartDelay(700);

        // ── Play entry phases (glow / logo / tagline) ─────────────────
        entryAnimators = new AnimatorSet();
        entryAnimators.playTogether(glowIn, logoIn, logoPulse, tagIn);
        entryAnimators.start();

        // ── Loading dots — run independently so cancel() can't hide them
        // Fade the row in at 500 ms, then start the wave immediately after.
        AnimatorSet loaderIn = new AnimatorSet();
        loaderIn.playTogether(
                ObjectAnimator.ofFloat(loader, "alpha",        0f,  1f),
                ObjectAnimator.ofFloat(loader, "translationY", 12f, 0f)
        );
        loaderIn.setDuration(350);
        loaderIn.setInterpolator(new DecelerateInterpolator());
        loaderIn.setStartDelay(500);
        loaderIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (binding != null) startDotWave(dot1, dot2, dot3);
            }
        });
        loaderIn.start();
    }

    /** Repeating 3-dot wave: each dot bobs up in turn, giving a "loading" feel. */
    private void startDotWave(View d1, View d2, View d3) {
        View[] dots = {d1, d2, d3};
        for (int i = 0; i < dots.length; i++) {
            final View dot = dots[i];
            ObjectAnimator bounce = ObjectAnimator.ofFloat(dot, "translationY", 0f, -9f, 0f);
            bounce.setDuration(600);
            bounce.setInterpolator(new AccelerateDecelerateInterpolator());
            bounce.setStartDelay(i * 150L);
            bounce.setRepeatCount(ValueAnimator.INFINITE);
            bounce.setRepeatMode(ValueAnimator.RESTART);
            bounce.start();
            dotAnimators.add(bounce);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Exit animation → navigate
    // ─────────────────────────────────────────────────────────────────────

    private void playExitAndNavigate(View view, SplashViewModel.Route route) {
        if (binding == null) return;

        // Stop entry/pulse animations so exit takes full control.
        if (entryAnimators != null) {
            entryAnimators.cancel();
        }

        View logo    = binding.logoImage;
        View tagline = binding.taglineText;
        View loader  = binding.loadingIndicator;
        View glow    = binding.glowBg;

        AnimatorSet exit = new AnimatorSet();
        exit.playTogether(
                ObjectAnimator.ofFloat(logo,    "alpha",  logo.getAlpha(),    0f),
                ObjectAnimator.ofFloat(logo,    "scaleX", logo.getScaleX(),   1.12f),
                ObjectAnimator.ofFloat(logo,    "scaleY", logo.getScaleY(),   1.12f),
                ObjectAnimator.ofFloat(tagline, "alpha",  tagline.getAlpha(), 0f),
                ObjectAnimator.ofFloat(tagline, "translationY", 0f,          -10f),
                ObjectAnimator.ofFloat(loader,  "alpha",  loader.getAlpha(),  0f),
                ObjectAnimator.ofFloat(glow,    "alpha",  glow.getAlpha(),    0f),
                ObjectAnimator.ofFloat(glow,    "scaleX", glow.getScaleX(),   1.3f),
                ObjectAnimator.ofFloat(glow,    "scaleY", glow.getScaleY(),   1.3f)
        );
        exit.setDuration(380);
        exit.setInterpolator(new AccelerateInterpolator(1.5f));
        exit.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isAdded()) return;
                navigateTo(view, route);
            }
        });
        exit.start();
    }

    private void navigateTo(View view, SplashViewModel.Route route) {
        switch (route) {
            case ONBOARDING:
                Navigation.findNavController(view)
                        .navigate(R.id.action_splashFragment_to_onboardingFragment);
                break;
            case LOGIN:
                Navigation.findNavController(view)
                        .navigate(R.id.action_splashFragment_to_loginFragment);
                break;
            case MAIN:
                Navigation.findNavController(view)
                        .navigate(R.id.action_splashFragment_to_homeFragment);
                break;
            case ADMIN:
                startActivity(new Intent(requireActivity(), AdminActivity.class));
                requireActivity().finish();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (entryAnimators != null) {
            entryAnimators.cancel();
            entryAnimators = null;
        }
        for (Animator a : dotAnimators) a.cancel();
        dotAnimators.clear();
        binding = null;
    }
}
