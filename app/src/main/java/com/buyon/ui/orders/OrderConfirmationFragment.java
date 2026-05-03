package com.buyon.ui.orders;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.buyon.app.R;
import com.buyon.app.databinding.FragmentOrderConfirmationBinding;
import com.buyon.domain.model.PaymentMethod;

public final class OrderConfirmationFragment extends Fragment {

    private FragmentOrderConfirmationBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderConfirmationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args      = getArguments();
        String orderId   = args != null ? args.getString("orderId",       "") : "";
        String method    = args != null ? args.getString("paymentMethod", PaymentMethod.COD.name()) : PaymentMethod.COD.name();
        String status    = args != null ? args.getString("orderStatus",   "Processing") : "Processing";

        String displayId = orderId.length() > 8
                ? "#" + orderId.substring(0, 8).toUpperCase()
                : "#" + orderId.toUpperCase();
        binding.tvOrderId.setText(displayId);
        binding.tvPaymentMethod.setText(PaymentMethod.fromString(method).getDisplayName());
        binding.tvStatus.setText(status);

        binding.btnTrack.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_orderConfirmationFragment_to_ordersFragment));

        binding.btnContinue.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_orderConfirmationFragment_to_homeFragment);
        });

        playEntranceAnimation();
    }

    private void playEntranceAnimation() {
        // 1. Circle pops in
        ObjectAnimator csx = ObjectAnimator.ofFloat(binding.circleSuccess, "scaleX", 0f, 1f);
        ObjectAnimator csy = ObjectAnimator.ofFloat(binding.circleSuccess, "scaleY", 0f, 1f);
        csx.setInterpolator(new OvershootInterpolator(3f));
        csy.setInterpolator(new OvershootInterpolator(3f));
        csx.setDuration(500);
        csy.setDuration(500);

        // 2. Title fades + slides up
        binding.tvTitle.setTranslationY(30f);
        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(binding.tvTitle, "alpha", 0f, 1f);
        ObjectAnimator titleTY    = ObjectAnimator.ofFloat(binding.tvTitle, "translationY", 30f, 0f);
        titleAlpha.setDuration(400); titleAlpha.setStartDelay(400);
        titleTY.setDuration(400);    titleTY.setStartDelay(400);
        titleTY.setInterpolator(new DecelerateInterpolator());

        // 3. Subtitle
        binding.tvSubtitle.setTranslationY(20f);
        ObjectAnimator subAlpha = ObjectAnimator.ofFloat(binding.tvSubtitle, "alpha", 0f, 1f);
        ObjectAnimator subTY    = ObjectAnimator.ofFloat(binding.tvSubtitle, "translationY", 20f, 0f);
        subAlpha.setDuration(400); subAlpha.setStartDelay(550);
        subTY.setDuration(400);    subTY.setStartDelay(550);
        subTY.setInterpolator(new DecelerateInterpolator());

        // 4. Card
        ObjectAnimator cardAlpha = ObjectAnimator.ofFloat(binding.cardDetails, "alpha", 0f, 1f);
        cardAlpha.setDuration(400); cardAlpha.setStartDelay(700);

        // 5. Buttons
        ObjectAnimator btnTrackAlpha    = ObjectAnimator.ofFloat(binding.btnTrack,    "alpha", 0f, 1f);
        ObjectAnimator btnContinueAlpha = ObjectAnimator.ofFloat(binding.btnContinue, "alpha", 0f, 1f);
        btnTrackAlpha.setDuration(350);    btnTrackAlpha.setStartDelay(850);
        btnContinueAlpha.setDuration(350); btnContinueAlpha.setStartDelay(950);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(csx, csy, titleAlpha, titleTY, subAlpha, subTY,
                cardAlpha, btnTrackAlpha, btnContinueAlpha);
        set.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
