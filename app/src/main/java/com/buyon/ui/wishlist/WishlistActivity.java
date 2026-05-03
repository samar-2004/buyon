package com.buyon.ui.wishlist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.buyon.app.BaseActivity;
import com.buyon.app.MainActivity;
import com.buyon.app.R;
import com.buyon.app.databinding.ActivityWishlistBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.model.CartItem;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.viewmodel.WishlistViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WishlistActivity extends BaseActivity {

    private ActivityWishlistBinding binding;
    private WishlistAdapter adapter;
    private final Set<String> addToCartInFlight = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWishlistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupInsets();

        AppDependencies deps = (AppDependencies) getApplication();
        WishlistViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(WishlistViewModel.class);

        adapter = new WishlistAdapter(new WishlistAdapter.Listener() {
            @Override
            public void onProductClick(com.buyon.domain.model.Product product) {
                Intent intent = new Intent(WishlistActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_OPEN_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onAddToCart(com.buyon.domain.model.Product product) {
                if (addToCartInFlight.contains(product.getId())) {
                    return;
                }
                addProductToCart(deps, product);
            }

            @Override
            public void onRemove(com.buyon.domain.model.Product product) {
                vm.removeFromWishlist(product.getId());
                Snackbar.make(binding.getRoot(), R.string.wishlist_removed, Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.listWishlist.setLayoutManager(new LinearLayoutManager(this));
        binding.listWishlist.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnStartShopping.setOnClickListener(v -> finish());

        vm.getLoading().observe(this, this::renderLoading);
        vm.getProducts().observe(this, this::renderProducts);
        vm.getError().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
                vm.clearError();
            }
        });
    }

    private void renderLoading(Boolean loading) {
        boolean show = Boolean.TRUE.equals(loading);
        binding.progress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            binding.listWishlist.setVisibility(View.GONE);
            binding.stateEmpty.setVisibility(View.GONE);
        }
    }

    private void renderProducts(List<com.buyon.domain.model.Product> products) {
        adapter.submitList(products);
        boolean isEmpty = products == null || products.isEmpty();
        binding.stateEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.listWishlist.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (!isEmpty) {
            binding.progress.setVisibility(View.GONE);
        }
    }

    private void setupInsets() {
        final int headerLeft = binding.header.getPaddingLeft();
        final int headerTop = binding.header.getPaddingTop();
        final int headerRight = binding.header.getPaddingRight();
        final int headerBottom = binding.header.getPaddingBottom();
        final int listLeft = binding.listWishlist.getPaddingLeft();
        final int listTop = binding.listWishlist.getPaddingTop();
        final int listRight = binding.listWishlist.getPaddingRight();
        final int listBottom = binding.listWishlist.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(binding.header, (v, insets) -> {
            Insets status = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(headerLeft, headerTop + status.top, headerRight, headerBottom);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(binding.listWishlist, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(listLeft, listTop, listRight, listBottom + nav.bottom);
            return insets;
        });
    }

    private void addProductToCart(
            AppDependencies deps,
            com.buyon.domain.model.Product product) {
        String uid = deps.authRepository().currentUserId();
        if (uid == null) {
            Snackbar.make(binding.getRoot(), R.string.wishlist_cart_sign_in_required, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        CartItem item =
                new CartItem(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getImageUrl(),
                        1);
        addToCartInFlight.add(product.getId());
        adapter.setAddToCartLoading(product.getId(), true);
        deps.cartRepository()
                .addOrUpdateItem(
                        uid,
                        item,
                        new DomainCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                addToCartInFlight.remove(product.getId());
                                adapter.setAddToCartLoading(product.getId(), false);
                                Snackbar.make(
                                                binding.getRoot(),
                                                R.string.wishlist_added_to_cart,
                                                Snackbar.LENGTH_SHORT)
                                        .show();
                            }

                            @Override
                            public void onError(Throwable error) {
                                addToCartInFlight.remove(product.getId());
                                adapter.setAddToCartLoading(product.getId(), false);
                                String msg = error != null && error.getMessage() != null
                                        ? error.getMessage()
                                        : getString(R.string.wishlist_add_to_cart_failed);
                                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
                            }
                        });
    }
}





