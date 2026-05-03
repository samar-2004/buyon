package com.buyon.ui.util;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public final class UiAnimations {

    private UiAnimations() {}

    public static void staggerRecyclerChildren(RecyclerView recyclerView) {
        recyclerView.post(() -> {
            LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (lm == null) return;
            for (int i = 0; i < lm.getChildCount(); i++) {
                View child = lm.getChildAt(i);
                if (child == null) continue;
                child.setAlpha(0f);
                child.setTranslationY(36f);
                child.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(320)
                        .setStartDelay(i * 55L)
                        .setInterpolator(new DecelerateInterpolator(1.4f))
                        .start();
            }
        });
    }

    public static void staggerVerticalSections(ViewGroup parent) {
        parent.post(() -> {
            int n = parent.getChildCount();
            for (int i = 0; i < n; i++) {
                View v = parent.getChildAt(i);
                v.setAlpha(0f);
                v.setTranslationY(40f);
                v.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(380)
                        .setStartDelay(60L + i * 65L)
                        .setInterpolator(new DecelerateInterpolator(1.5f))
                        .start();
            }
        });
    }

    public static void slideUpReveal(View view) {
        view.setTranslationY(140f);
        view.setAlpha(0f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .start();
    }

    public static void pulseScale(View view) {
        view.animate().cancel();
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(90)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(160)
                                .start())
                .start();
    }

    public static void shakeHorizontal(View view) {
        ObjectAnimator anim =
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, 14f, -14f, 10f, -10f, 6f, -6f, 0f);
        anim.setDuration(420);
        anim.start();
    }

    public static void buttonPress(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(80)
                .withEndAction(
                        () ->
                                view.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(130)
                                        .start())
                .start();
    }

    public static void animateButtonPressCompat(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(80)
                .withEndAction(
                        () -> view.animate().scaleX(1f).scaleY(1f).setDuration(130).start())
                .start();
    }

    public static void fadePopIn(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.92f);
        view.setScaleY(0.92f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator(1.4f))
                .start();
    }
}
