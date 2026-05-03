package com.buyon.ui.payment;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.buyon.app.R;
import com.buyon.app.databinding.FragmentPaymentBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.domain.model.PaymentMethod;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.viewmodel.PaymentViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class PaymentFragment extends Fragment {

    private FragmentPaymentBinding binding;
    private PaymentViewModel vm;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPaymentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        vm = new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(PaymentViewModel.class);

        Bundle args         = getArguments();
        String shippingAddr = args != null ? args.getString("shippingAddress", "") : "";
        String promoCode    = args != null ? args.getString("promoCode",       "") : "";
        String methodName   = args != null ? args.getString("paymentMethod",
                PaymentMethod.JAZZCASH.name()) : PaymentMethod.JAZZCASH.name();
        PaymentMethod method = PaymentMethod.fromString(methodName);

        String uid = deps.authRepository().currentUserId();
        String custName = args != null ? args.getString("customerDisplayName", "") : "";
        String custEmail = args != null ? args.getString("customerEmail", "") : "";
        String custPhone = args != null ? args.getString("customerPhone", "") : "";
        vm.init(uid, shippingAddr, promoCode, method, custName, custEmail, custPhone);

        binding.tvGatewayName.setText(method.getDisplayName());
        binding.tvTitle.setText(method.getDisplayName() + " Payment");

        // Issue 10: inform user this is a simulated demo payment gateway
        Snackbar.make(view,
                "Demo mode: payment is simulated. No real transaction will occur.",
                Snackbar.LENGTH_LONG).show();

        vm.getTotalAmount().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                binding.tvAmount.setText(String.format(Locale.US, "$ %.2f", total));
            }
        });

        vm.getCartError().observe(getViewLifecycleOwner(), errMsg -> {
            if (errMsg != null && !errMsg.isEmpty()) {
                Snackbar.make(view, errMsg, Snackbar.LENGTH_LONG).show();
            }
        });

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        binding.btnPay.setOnClickListener(v -> {
            if (!validateForm()) return;
            animateButtonPress(v);
            setPayButtonLoading(true);
            vm.initiatePayment();
        });

        vm.getPaymentInitResult().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.getStatus() == Resource.Status.SUCCESS) {
                // Simulate gateway processing delay, then confirm
                String fakeRef = method.name() + "-"
                        + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.US);
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> vm.confirmAndPlaceOrder(fakeRef), 1500);
            }
            if (res.getStatus() == Resource.Status.ERROR) {
                setPayButtonLoading(false);
                if (res.getError() != null) {
                    Snackbar.make(view, res.getError().getMessage(), Snackbar.LENGTH_LONG).show();
                }
            }
        });

        vm.getOrderResult().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.getStatus() == Resource.Status.LOADING) {
                setPayButtonLoading(true);
                return;
            }
            setPayButtonLoading(false);

            if (res.getStatus() == Resource.Status.SUCCESS && res.getData() != null) {
                showSuccessAnimation(() ->
                        Navigation.findNavController(view)
                                .navigate(R.id.action_paymentFragment_to_ordersFragment));
            }
            if (res.getStatus() == Resource.Status.ERROR && res.getError() != null) {
                vm.markPaymentFailed();
                Snackbar.make(view, res.getError().getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setPayButtonLoading(boolean loading) {
        if (binding == null) return;
        binding.progressPay.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPay.setEnabled(!loading);
        binding.btnPay.setText(loading ? "" : getString(R.string.confirm_payment));
    }

    // ── Form validation ─────────────────────────────────────────────────

    private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

    private boolean validateForm() {
        boolean valid = true;

        String mobile = binding.inputMobile.getText() != null
                ? binding.inputMobile.getText().toString().trim() : "";
        if (mobile.isEmpty()) {
            binding.layoutMobile.setError("Mobile number is required");
            valid = false;
        } else if (!DIGITS_ONLY.matcher(mobile).matches()) {
            binding.layoutMobile.setError("Mobile number must contain digits only");
            valid = false;
        } else if (mobile.length() != 11) {
            binding.layoutMobile.setError(mobile.length() < 11
                    ? "Mobile number is too short — must be 11 digits"
                    : "Mobile number is too long — must be 11 digits");
            valid = false;
        } else {
            binding.layoutMobile.setError(null);
        }

        String cnic = binding.inputCnic.getText() != null
                ? binding.inputCnic.getText().toString().trim() : "";
        if (cnic.isEmpty()) {
            binding.layoutCnic.setError("CNIC digits are required");
            valid = false;
        } else if (!DIGITS_ONLY.matcher(cnic).matches()) {
            binding.layoutCnic.setError("CNIC must contain digits only");
            valid = false;
        } else if (cnic.length() != 6) {
            binding.layoutCnic.setError(cnic.length() < 6
                    ? "Enter all 6 digits of your CNIC (too short)"
                    : "Enter only the last 6 digits of your CNIC");
            valid = false;
        } else {
            binding.layoutCnic.setError(null);
        }

        String pin = binding.inputPin.getText() != null
                ? binding.inputPin.getText().toString().trim() : "";
        if (pin.isEmpty()) {
            binding.layoutPin.setError("mPIN / OTP is required");
            valid = false;
        } else if (pin.length() < 4) {
            binding.layoutPin.setError("mPIN must be at least 4 digits");
            valid = false;
        } else {
            binding.layoutPin.setError(null);
        }

        return valid;
    }

    // ── Animations ──────────────────────────────────────────────────────

    private void showSuccessAnimation(Runnable onFinished) {
        if (binding == null) return;
        binding.overlaySuccess.setVisibility(View.VISIBLE);

        ObjectAnimator fade = ObjectAnimator.ofFloat(binding.overlaySuccess, "alpha", 0f, 1f);
        fade.setDuration(300);

        ObjectAnimator csx = ObjectAnimator.ofFloat(binding.cardSuccess, "scaleX", 0.5f, 1f);
        ObjectAnimator csy = ObjectAnimator.ofFloat(binding.cardSuccess, "scaleY", 0.5f, 1f);
        csx.setInterpolator(new OvershootInterpolator(2.5f));
        csy.setInterpolator(new OvershootInterpolator(2.5f));
        csx.setDuration(450); csx.setStartDelay(200);
        csy.setDuration(450); csy.setStartDelay(200);

        ObjectAnimator ckx = ObjectAnimator.ofFloat(binding.circleCheck, "scaleX", 0f, 1f);
        ObjectAnimator cky = ObjectAnimator.ofFloat(binding.circleCheck, "scaleY", 0f, 1f);
        ckx.setInterpolator(new OvershootInterpolator(4f));
        cky.setInterpolator(new OvershootInterpolator(4f));
        ckx.setDuration(400); ckx.setStartDelay(500);
        cky.setDuration(400); cky.setStartDelay(500);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fade, csx, csy, ckx, cky);
        set.start();

        new Handler(Looper.getMainLooper()).postDelayed(onFinished, 1800);
    }

    private void animateButtonPress(View view) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                .withEndAction(() ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(130).start())
                .start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
