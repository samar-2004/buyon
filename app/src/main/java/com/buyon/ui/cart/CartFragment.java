package com.buyon.ui.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.buyon.app.R;
import com.buyon.app.databinding.FragmentCartBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.domain.model.CartItem;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.adapters.CartAdapter;
import com.buyon.ui.pricing.OrderPricing;
import com.buyon.ui.util.UiAnimations;
import com.buyon.ui.viewmodel.CartViewModel;

import java.util.List;
import java.util.Locale;

public final class CartFragment extends Fragment {

    private static final double DISCOUNT_THRESHOLD = 0.001;

    private FragmentCartBinding binding;
    private CartAdapter adapter;
    private CartViewModel vm;

    private boolean didAnimateBar;
    private boolean didStaggerList;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        vm = new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(CartViewModel.class);

        adapter =
                new CartAdapter(
                        true,
                        new CartAdapter.Listener() {
                            @Override
                            public void onPlus(CartItem item) {
                                vm.updateQuantity(item, item.getQuantity() + 1);
                            }

                            @Override
                            public void onMinus(CartItem item) {
                                int q = item.getQuantity() - 1;
                                if (q < 1) {
                                    vm.remove(item);
                                } else {
                                    vm.updateQuantity(item, q);
                                }
                            }
                        });
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.btnCheckout.setOnClickListener(
                v -> {
                    UiAnimations.animateButtonPressCompat(v);
                    Navigation.findNavController(view)
                            .navigate(R.id.action_cartFragment_to_checkoutFragment);
                });

        if (binding.btnStartShopping != null) {
            binding.btnStartShopping.setOnClickListener(
                    v -> Navigation.findNavController(view).navigate(R.id.homeFragment));
        }

        vm.getItems()
                .observe(
                        getViewLifecycleOwner(),
                        items -> {
                            if (items == null || items.isEmpty()) {
                                didAnimateBar = false;
                                didStaggerList = false;
                                binding.empty.setVisibility(View.VISIBLE);
                                binding.list.setVisibility(View.GONE);
                                binding.barTotal.setVisibility(View.GONE);
                                binding.bannerPromo.setVisibility(View.GONE);
                                UiAnimations.fadePopIn(binding.emptyIcon);
                            } else {
                                binding.empty.setVisibility(View.GONE);
                                binding.list.setVisibility(View.VISIBLE);
                                binding.barTotal.setVisibility(View.VISIBLE);
                                binding.bannerPromo.setVisibility(View.VISIBLE);
                                adapter.submit(items);
                                updateTotalsUi(items);
                                if (!didStaggerList) {
                                    UiAnimations.staggerRecyclerChildren(binding.list);
                                    didStaggerList = true;
                                }
                                if (!didAnimateBar) {
                                    UiAnimations.slideUpReveal(binding.barTotal);
                                    didAnimateBar = true;
                                }
                            }
                        });
    }

    private void updateTotalsUi(@NonNull List<CartItem> items) {
        double sub = OrderPricing.subtotal(items);
        double discount = OrderPricing.discount(OrderPricing.PROMO_BUYON20, sub);
        double ship = OrderPricing.SHIPPING_FEE_USD;
        double total = sub - discount + ship;

        int units = 0;
        for (CartItem c : items) {
            units += c.getQuantity();
        }
        binding.textCartSubtitle.setText(
                getString(R.string.cart_subtitle_format, units, getString(R.string.cart_promo_banner)));

        binding.cartValueSubtotal.setText(money(sub));
        binding.cartValueDiscount.setText("- " + money(discount));
        binding.cartValueShipping.setText(money(ship));
        binding.total.setText(money(total));

        binding.cartRowDiscount.setVisibility(discount > DISCOUNT_THRESHOLD ? View.VISIBLE : View.GONE);
        if (discount > DISCOUNT_THRESHOLD) {
            binding.textCartSavings.setVisibility(View.VISIBLE);
            binding.textCartSavings.setText(getString(R.string.cart_savings_line, money(discount)));
        } else {
            binding.textCartSavings.setVisibility(View.GONE);
        }
    }

    private static String money(double v) {
        return String.format(Locale.US, "$ %.2f", v);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) {
            vm.refresh();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
