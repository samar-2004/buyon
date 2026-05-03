package com.buyon.ui.checkout;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.buyon.app.R;
import com.buyon.app.databinding.FragmentCheckoutBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.domain.model.CartItem;
import com.buyon.domain.model.PaymentMethod;
import com.buyon.domain.model.UserProfile;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.adapters.CartAdapter;
import com.buyon.ui.pricing.OrderPricing;
import com.buyon.ui.util.UiAnimations;
import com.buyon.ui.viewmodel.CheckoutViewModel;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private CartAdapter adapter;
    private CheckoutViewModel vm;

    private PaymentMethod selectedMethod = PaymentMethod.COD;
    private List<CartItem> lastCartItems;
    private boolean didStaggerCheckoutList;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AtomicReference<AppDependencies> deps = new AtomicReference<>((AppDependencies) requireActivity().getApplication());
        vm = new ViewModelProvider(this, new BuyonViewModelFactory(deps.get())).get(CheckoutViewModel.class);

        binding.textCheckoutSubtitle.setText(R.string.checkout_subtitle);

        if (binding.btnBack != null) {
            binding.btnBack.setOnClickListener(
                    v -> {
                        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70)
                                .withEndAction(
                                        () -> v.animate()
                                                .scaleX(1f)
                                                .scaleY(1f)
                                                .setDuration(110)
                                                .withEndAction(
                                                        () -> Navigation.findNavController(view).navigateUp())
                                                .start())
                                .start();
                    });
        }

        if (savedInstanceState == null) {
            binding.inputPromo.setText(OrderPricing.PROMO_BUYON20);
        }

        adapter =
                new CartAdapter(
                        false,
                        new CartAdapter.Listener() {
                            @Override
                            public void onPlus(CartItem item) {}

                            @Override
                            public void onMinus(CartItem item) {}
                        });
        binding.listItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listItems.setAdapter(adapter);

        setupPaymentMethodSelector();

        vm.getProfile()
                .observe(
                        getViewLifecycleOwner(),
                        p -> {
                            if (p == null) {
                                applyContactPlaceholders(null);
                                return;
                            }
                            if (p.getDefaultShippingAddress() != null
                                    && !p.getDefaultShippingAddress().isEmpty()) {
                                if (binding.inputAddress.getText() == null
                                        || binding.inputAddress.getText().toString().trim().isEmpty()) {
                                    binding.inputAddress.setText(p.getDefaultShippingAddress());
                                }
                            }
                            applyContactPlaceholders(p);
                        });

        vm.getCartItems()
                .observe(
                        getViewLifecycleOwner(),
                        items -> {
                            lastCartItems = items;
                            adapter.submit(items);
                            updateItemsHeaderCount(items);
                            updateSummary(items);
                            if (!didStaggerCheckoutList) {
                                UiAnimations.staggerRecyclerChildren(binding.listItems);
                                didStaggerCheckoutList = true;
                            }
                        });

        binding.inputAddress.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (binding != null) {
                            binding.layoutAddress.setError(null);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

        binding.inputPromo.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (binding != null) {
                            binding.layoutPromo.setError(null);
                            updateSummary(lastCartItems);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

        binding.btnApplyPromo.setOnClickListener(
                v -> {
                    animateButtonPress(v);
                    String code =
                            binding.inputPromo.getText() != null
                                    ? binding.inputPromo.getText().toString().trim()
                                    : "";
                    if (code.isEmpty()) {
                        binding.layoutPromo.setError(getString(R.string.checkout_promo_empty));
                    } else if (OrderPricing.PROMO_BUYON20.equalsIgnoreCase(code)) {
                        binding.layoutPromo.setError(null);
                        binding.textPromoStatus.setVisibility(View.VISIBLE);
                        binding.textPromoStatus.setText(R.string.checkout_promo_applied_msg);
                        UiAnimations.pulseScale(binding.cardSummary);
                        updateSummary(lastCartItems);
                    } else {
                        binding.layoutPromo.setError(getString(R.string.checkout_promo_invalid));
                        UiAnimations.shakeHorizontal(binding.inputPromo);
                    }
                });

        binding.btnOrder.setOnClickListener(
                v -> {
                    animateButtonPress(v);
                    String addr =
                            binding.inputAddress.getText() != null
                                    ? binding.inputAddress.getText().toString().trim()
                                    : "";
                    String promo =
                            binding.inputPromo.getText() != null
                                    ? binding.inputPromo.getText().toString()
                                    : "";

                    if (addr.isEmpty()) {
                        binding.layoutAddress.setError("Shipping address is required");
                        binding.inputAddress.requestFocus();
                        InputMethodManager imm = (InputMethodManager)
                                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(binding.inputAddress, InputMethodManager.SHOW_IMPLICIT);
                        }
                        return;
                    }
                    binding.layoutAddress.setError(null);

                    if (selectedMethod.isOnline()) {
                        deps.set((AppDependencies) requireActivity().getApplication());
                        UserProfile prof = vm.getProfile().getValue();
                        String custName =
                                prof != null && prof.getDisplayName() != null
                                        ? prof.getDisplayName().trim()
                                        : "";
                        String custPhone =
                                prof != null && prof.getPhone() != null
                                        ? prof.getPhone().trim()
                                        : "";
                        String custEmail = "";
                        if (prof != null
                                && prof.getEmail() != null
                                && !prof.getEmail().trim().isEmpty()) {
                            custEmail = prof.getEmail().trim();
                        } else {
                            String ae = deps.get().authRepository().currentUserEmail();
                            custEmail = ae != null ? ae.trim() : "";
                        }
                        Bundle args = new Bundle();
                        args.putString("shippingAddress", addr);
                        args.putString("promoCode", promo);
                        args.putString("paymentMethod", selectedMethod.name());
                        args.putString("customerDisplayName", custName);
                        args.putString("customerEmail", custEmail);
                        args.putString("customerPhone", custPhone);
                        Navigation.findNavController(view)
                                .navigate(R.id.action_checkoutFragment_to_paymentFragment, args);
                    } else {
                        vm.placeOrder(addr, promo, PaymentMethod.COD);
                    }
                });

        vm.getPlaceOrderResult()
                .observe(
                        getViewLifecycleOwner(),
                        res -> {
                            if (res == null) return;
                            if (res.getStatus() == Resource.Status.LOADING) {
                                setOrderButtonLoading(true);
                            } else {
                                setOrderButtonLoading(false);
                            }
                            if (res.getStatus() == Resource.Status.SUCCESS && res.getData() != null) {
                                showSuccessAnimation(() -> {
                                    if (!isAdded() || binding == null) return;
                                    Navigation.findNavController(view)
                                            .navigate(R.id.action_checkoutFragment_to_ordersFragment);
                                });
                            }
                            if (res.getStatus() == Resource.Status.ERROR) {
                                String errMsg = AppErrorHandler.getFirebaseErrorMessage(res.getError());
                                AppErrorHandler.showError(view, errMsg);
                            }
                        });

        UiAnimations.staggerVerticalSections(binding.checkoutSections);
        UiAnimations.slideUpReveal(binding.barPlaceOrder);
        refreshPromoStatusUi();
    }

    private void applyContactPlaceholders(@Nullable UserProfile p) {
        if (binding == null) return;
        if (p == null) {
            binding.textContactName.setText("—");
            binding.textContactEmail.setText("—");
            binding.textContactPhone.setText(R.string.profile_phone_missing);
            return;
        }
        binding.textContactName.setText(nonEmpty(p.getDisplayName(), "—"));
        binding.textContactEmail.setText(nonEmpty(p.getEmail(), "—"));
        String phone = p.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            binding.textContactPhone.setText(R.string.profile_phone_missing);
        } else {
            binding.textContactPhone.setText(phone.trim());
        }
    }

    private static String nonEmpty(String s, String fallback) {
        if (s == null || s.trim().isEmpty()) return fallback;
        return s.trim();
    }

    private void updateItemsHeaderCount(@Nullable List<CartItem> items) {
        if (binding == null) return;
        if (items == null || items.isEmpty()) {
            binding.textItemsCount.setVisibility(View.GONE);
            return;
        }
        int n = 0;
        for (CartItem c : items) {
            n += c.getQuantity();
        }
        binding.textItemsCount.setVisibility(View.VISIBLE);
        binding.textItemsCount.setText(getString(R.string.cart_items_format, n));
    }

    private String promoText() {
        if (binding == null || binding.inputPromo.getText() == null) return "";
        return binding.inputPromo.getText().toString();
    }

    private void refreshPromoStatusUi() {
        if (binding == null) return;
        String trimmed = promoText().trim();
        if (trimmed.isEmpty()) {
            binding.textPromoStatus.setVisibility(View.GONE);
            return;
        }
        binding.textPromoStatus.setVisibility(View.VISIBLE);
        if (OrderPricing.PROMO_BUYON20.equalsIgnoreCase(trimmed)) {
            binding.textPromoStatus.setText(R.string.checkout_promo_applied_msg);
            binding.textPromoStatus.setTextColor(requireContext().getColor(R.color.buyon_success));
        } else {
            binding.textPromoStatus.setText(R.string.checkout_promo_not_applied);
            binding.textPromoStatus.setTextColor(requireContext().getColor(R.color.buyon_text_secondary));
        }
    }

    private void setupPaymentMethodSelector() {
        selectMethod(PaymentMethod.COD);
        // Announce initial selection for accessibility services
        binding.cardPayCod.post(() -> {
            if (binding != null) {
                binding.cardPayCod.announceForAccessibility("Cash on Delivery selected");
            }
        });

        binding.cardPayCod.setOnClickListener(
                v -> {
                    animateCardSelect(binding.cardPayCod);
                    selectMethod(PaymentMethod.COD);
                });
        binding.cardPayJazzcash.setOnClickListener(
                v -> {
                    animateCardSelect(binding.cardPayJazzcash);
                    selectMethod(PaymentMethod.JAZZCASH);
                });
        binding.cardPayEasypaisa.setOnClickListener(
                v -> {
                    animateCardSelect(binding.cardPayEasypaisa);
                    selectMethod(PaymentMethod.EASYPAISA);
                });
    }

    private void selectMethod(PaymentMethod method) {
        selectedMethod = method;

        applyCardStyle(binding.cardPayCod, false);
        applyCardStyle(binding.cardPayJazzcash, false);
        applyCardStyle(binding.cardPayEasypaisa, false);

        binding.icCodCheck.setVisibility(View.INVISIBLE);
        binding.icJazzcashCheck.setVisibility(View.INVISIBLE);
        binding.icEasypaisaCheck.setVisibility(View.INVISIBLE);

        switch (method) {
            case COD:
                applyCardStyle(binding.cardPayCod, true);
                binding.icCodCheck.setVisibility(View.VISIBLE);
                break;
            case JAZZCASH:
                applyCardStyle(binding.cardPayJazzcash, true);
                binding.icJazzcashCheck.setVisibility(View.VISIBLE);
                break;
            case EASYPAISA:
                applyCardStyle(binding.cardPayEasypaisa, true);
                binding.icEasypaisaCheck.setVisibility(View.VISIBLE);
                break;
        }

        updateOrderButtonLabel();
    }

    private void updateOrderButtonLabel() {
        if (binding == null) return;
        binding.btnOrder.setText(selectedMethod.isOnline() ? "Proceed to Pay" : "Place Order");
    }

    private void setOrderButtonLoading(boolean loading) {
        if (binding == null) return;
        binding.progressOrder.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnOrder.setEnabled(!loading);
        if (loading) {
            binding.btnOrder.setText("");
        } else {
            updateOrderButtonLabel();
        }
    }

    private void applyCardStyle(MaterialCardView card, boolean selected) {
        if (selected) {
            card.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_stroke_selected));
            card.setCardBackgroundColor(requireContext().getColor(R.color.buyon_primary_ultra_light));
        } else {
            card.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_stroke_default));
            card.setCardBackgroundColor(requireContext().getColor(R.color.buyon_card));
        }
    }

    private void animateCardSelect(View card) {
        card.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(80)
                .withEndAction(
                        () -> card.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                .start();
    }

    private void showSuccessAnimation(Runnable onFinished) {
        if (binding == null) return;
        binding.overlaySuccess.setVisibility(View.VISIBLE);

        ObjectAnimator overlayFade = ObjectAnimator.ofFloat(binding.overlaySuccess, "alpha", 0f, 1f);
        overlayFade.setDuration(300);

        ObjectAnimator cardScaleX = ObjectAnimator.ofFloat(binding.cardSuccess, "scaleX", 0.5f, 1f);
        ObjectAnimator cardScaleY = ObjectAnimator.ofFloat(binding.cardSuccess, "scaleY", 0.5f, 1f);
        cardScaleX.setInterpolator(new OvershootInterpolator(2.5f));
        cardScaleY.setInterpolator(new OvershootInterpolator(2.5f));
        cardScaleX.setDuration(450);
        cardScaleY.setDuration(450);
        cardScaleX.setStartDelay(200);
        cardScaleY.setStartDelay(200);

        ObjectAnimator checkX = ObjectAnimator.ofFloat(binding.circleCheck, "scaleX", 0f, 1f);
        ObjectAnimator checkY = ObjectAnimator.ofFloat(binding.circleCheck, "scaleY", 0f, 1f);
        checkX.setInterpolator(new OvershootInterpolator(4f));
        checkY.setInterpolator(new OvershootInterpolator(4f));
        checkX.setDuration(400);
        checkY.setDuration(400);
        checkX.setStartDelay(500);
        checkY.setStartDelay(500);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(overlayFade, cardScaleX, cardScaleY, checkX, checkY);
        set.start();

        new Handler(Looper.getMainLooper()).postDelayed(onFinished, 1600);
    }

    private void animateButtonPress(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(80)
                .withEndAction(
                        () -> view.animate().scaleX(1f).scaleY(1f).setDuration(130).start())
                .start();
    }

    private void updateSummary(@Nullable List<CartItem> items) {
        if (items == null || binding == null || vm == null) return;
        double sub = OrderPricing.subtotal(items);
        double ship = vm.getShippingFee();
        double discount = OrderPricing.discount(promoText(), sub);
        double total = sub - discount + ship;

        binding.summaryValueSubtotal.setText(money(sub));
        binding.summaryValueShipping.setText(money(ship));
        binding.summaryValueTotal.setText(money(total));
        binding.barTotal.setText(money(total));

        if (discount > 0.001) {
            binding.layoutDiscountRow.setVisibility(View.VISIBLE);
            binding.summaryValueDiscount.setText("- " + money(discount));
        } else {
            binding.layoutDiscountRow.setVisibility(View.GONE);
        }
        refreshPromoStatusUi();
    }

    private String money(double v) {
        return String.format(Locale.US, "$ %.2f", v);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
