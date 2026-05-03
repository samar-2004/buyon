package com.buyon.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.buyon.domain.model.Product;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.buyon.app.R;
import com.buyon.app.databinding.FragmentAdminProductsBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.viewmodel.AdminViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class AdminProductsFragment extends Fragment {

    private FragmentAdminProductsBinding binding;
    private AdminViewModel vm;
    private AdminProductAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminProductsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        vm = new ViewModelProvider(requireActivity(), new BuyonViewModelFactory(deps))
                .get(AdminViewModel.class);

        adapter = new AdminProductAdapter(
                product -> {
                    Bundle args = new Bundle();
                    args.putString("productId", product.getId());
                    Navigation.findNavController(view)
                            .navigate(R.id.action_adminProducts_to_addProduct, args);
                },
                product -> new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Product")
                        .setMessage("Are you sure you want to delete \"" + product.getName() + "\"?")
                        .setPositiveButton("Delete", (d, w) -> deleteWithUndo(view, product))
                        .setNegativeButton("Cancel", null)
                        .show());

        binding.listProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listProducts.setAdapter(adapter);

        binding.btnAddProduct.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_adminProducts_to_addProduct));

        if (binding.btnEmptyAddProduct != null) {
            binding.btnEmptyAddProduct.setOnClickListener(v ->
                    Navigation.findNavController(view)
                            .navigate(R.id.action_adminProducts_to_addProduct));
        }

        vm.getProducts().observe(getViewLifecycleOwner(), products -> {
            boolean empty = products == null || products.isEmpty();
            binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.listProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (binding.btnEmptyAddProduct != null) {
                binding.btnEmptyAddProduct.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
            if (!empty) {
                adapter.submitList(products);
            }
        });

        vm.getDeleteProductState().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.getStatus() == Resource.Status.ERROR) {
                AppErrorHandler.showError(view,
                        AppErrorHandler.getFirebaseErrorMessage(res.getError()));
            }
        });

        vm.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                AppErrorHandler.showError(view,
                        AppErrorHandler.getFirebaseErrorMessage(new Exception(msg)));
            }
        });
    }

    private void deleteWithUndo(View view, Product product) {
        vm.deleteProduct(product.getId());
        Snackbar.make(view, "\"" + product.getName() + "\" deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> vm.saveProduct(product))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
