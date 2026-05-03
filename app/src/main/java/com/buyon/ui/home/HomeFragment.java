package com.buyon.ui.home;

import android.content.Intent;
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

import com.buyon.core.di.AppDependencies;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.app.R;
import com.buyon.ui.adapters.CategoryAdapter;
import com.buyon.ui.adapters.ProductAdapter;
import com.buyon.app.databinding.FragmentHomeBinding;
import com.buyon.ui.viewmodel.HomeViewModel;
import com.buyon.ui.viewmodel.WishlistViewModel;
import com.buyon.ui.wishlist.WishlistActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;
    private final Set<String> localWishlistIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        HomeViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(HomeViewModel.class);
        WishlistViewModel wishlistVm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(WishlistViewModel.class);

        // Search bar taps navigate to search
        if (binding.searchInput != null) {
            binding.searchInput.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.searchFragment));
            binding.searchLayout.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.searchFragment));
        }
        if (binding.btnNotification != null) {
            binding.btnNotification.setOnClickListener(
                    v -> startActivity(new Intent(requireContext(), WishlistActivity.class)));
        }

        productAdapter = new ProductAdapter(new ProductAdapter.Listener() {
            @Override
            public void onProductClick(com.buyon.domain.model.Product p) {
                Bundle b = new Bundle();
                b.putString("productId", p.getId());
                Navigation.findNavController(view)
                        .navigate(R.id.action_homeFragment_to_productDetailFragment, b);
            }

            @Override
            public void onWishlistToggle(com.buyon.domain.model.Product p, boolean currentlyWishlisted) {
                boolean targetWishlisted = !currentlyWishlisted;
                applyLocalWishlistState(p.getId(), targetWishlisted);
                showWishlistSnack(view, targetWishlisted);
                wishlistVm.setWishlisted(
                        p,
                        targetWishlisted,
                        new com.buyon.domain.callback.DomainCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {}

                            @Override
                            public void onError(Throwable error) {
                                // Roll back optimistic toggle when write fails.
                                applyLocalWishlistState(p.getId(), currentlyWishlisted);
                            }
                        });
            }
        });
        binding.listProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listProducts.setAdapter(productAdapter);

        categoryAdapter = new CategoryAdapter(categoryId -> {
            categoryAdapter.setSelected(categoryId);
            vm.setCategoryFilter(categoryId);
        });
        binding.listCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.listCategories.setAdapter(categoryAdapter);

        binding.refresh.setOnRefreshListener(() -> {
            vm.refresh();
            binding.refresh.setRefreshing(false);
        });

        // Show shimmer while loading
        showShimmer(true);

        vm.getProducts().observe(getViewLifecycleOwner(), products -> {
            showShimmer(false);
            if (products != null) {
                productAdapter.submitList(products);
                productAdapter.setWishlistedIds(localWishlistIds);
            }
        });
        wishlistVm.getWishlistIds().observe(getViewLifecycleOwner(), ids -> {
            syncLocalWishlist(ids);
            productAdapter.setWishlistedIds(localWishlistIds);
        });
        wishlistVm.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show();
                wishlistVm.clearError();
            }
        });
        vm.getCategories().observe(getViewLifecycleOwner(), categoryAdapter::submit);
        vm.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                showShimmer(false);
                AppErrorHandler.showError(view,
                        AppErrorHandler.getFirebaseErrorMessage(new Exception(msg)));
            }
        });
    }

    private void syncLocalWishlist(List<String> ids) {
        localWishlistIds.clear();
        if (ids != null) {
            localWishlistIds.addAll(ids);
        }
    }

    private void applyLocalWishlistState(String productId, boolean wishlisted) {
        if (wishlisted) {
            localWishlistIds.add(productId);
        } else {
            localWishlistIds.remove(productId);
        }
        productAdapter.setWishlisted(productId, wishlisted);
    }

    private void showWishlistSnack(View view, boolean added) {
        int msgRes = added ? R.string.wishlist_added : R.string.wishlist_removed;
        Snackbar.make(view, msgRes, Snackbar.LENGTH_SHORT).show();
    }

    private void showShimmer(boolean show) {
        if (binding.shimmerProducts == null) return;
        if (show) {
            binding.shimmerProducts.setVisibility(View.VISIBLE);
            binding.shimmerProducts.startShimmer();
            binding.listProducts.setVisibility(View.GONE);
        } else {
            binding.shimmerProducts.stopShimmer();
            binding.shimmerProducts.setVisibility(View.GONE);
            binding.listProducts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
