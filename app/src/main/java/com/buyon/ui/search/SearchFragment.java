package com.buyon.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.buyon.core.di.AppDependencies;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.app.R;
import com.buyon.ui.adapters.CategoryAdapter;
import com.buyon.ui.adapters.ProductAdapter;
import com.buyon.app.databinding.FragmentSearchBinding;
import com.buyon.ui.viewmodel.SearchViewModel;
import com.buyon.ui.viewmodel.WishlistViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;
    private final Set<String> localWishlistIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyStatusBarPadding();
        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        SearchViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(SearchViewModel.class);
        WishlistViewModel wishlistVm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(WishlistViewModel.class);

        productAdapter =
                new ProductAdapter(
                        new ProductAdapter.Listener() {
                            @Override
                            public void onProductClick(com.buyon.domain.model.Product p) {
                                Bundle b = new Bundle();
                                b.putString("productId", p.getId());
                                Navigation.findNavController(view)
                                        .navigate(R.id.action_searchFragment_to_productDetailFragment, b);
                            }

                            @Override
                            public void onWishlistToggle(
                                    com.buyon.domain.model.Product p,
                                    boolean currentlyWishlisted) {
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
                                                applyLocalWishlistState(p.getId(), currentlyWishlisted);
                                            }
                                        });
                            }
                        });
        binding.listProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listProducts.setAdapter(productAdapter);

        categoryAdapter =
                new CategoryAdapter(
                        categoryId -> {
                            categoryAdapter.setSelected(categoryId);
                            vm.setCategoryFilter(categoryId);
                        });
        binding.listCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.listCategories.setAdapter(categoryAdapter);

        binding.inputSearch.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        CharSequence q = binding.inputSearch.getText();
                        vm.search(q != null ? q.toString() : "");
                        return true;
                    }
                    return false;
                });

        binding.layoutSearch.setEndIconOnClickListener(
                v -> {
                    CharSequence q = binding.inputSearch.getText();
                    vm.search(q != null ? q.toString() : "");
                });

        // Issue 53: personalise greeting with user display name
        String currentName = deps.authRepository().currentUserEmail();
        if (binding.labelGreeting != null && currentName != null && !currentName.isEmpty()) {
            String displayName = currentName.contains("@")
                    ? currentName.substring(0, currentName.indexOf('@')) : currentName;
            binding.labelGreeting.setText(getString(R.string.search_greeting_format, displayName));
            binding.labelGreeting.setVisibility(View.VISIBLE);
        }

        vm.getProducts().observe(getViewLifecycleOwner(), products -> {
            if (binding == null) return;
            boolean empty = products == null || products.isEmpty();
            productAdapter.submitList(products);
            productAdapter.setWishlistedIds(localWishlistIds);
            if (binding.emptySearch != null) {
                binding.emptySearch.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
            if (binding.listProducts != null) {
                binding.listProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        vm.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) return;
            boolean isLoading = Boolean.TRUE.equals(loading);
            if (binding.progressSearch != null) {
                binding.progressSearch.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            if (isLoading && binding.emptySearch != null) {
                binding.emptySearch.setVisibility(View.GONE);
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
        vm.getErrorMessage()
                .observe(
                        getViewLifecycleOwner(),
                        msg -> {
                            if (msg != null && !msg.isEmpty()) {
                                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show();
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

    private void applyStatusBarPadding() {
        int basePaddingTop = binding.header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(binding.header, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    basePaddingTop + bars.top,
                    v.getPaddingRight(),
                    v.getPaddingBottom());
            return insets;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
