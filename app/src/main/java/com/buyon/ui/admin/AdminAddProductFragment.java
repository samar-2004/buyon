package com.buyon.ui.admin;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.buyon.app.databinding.DialogAddCategoryBinding;
import com.buyon.app.databinding.FragmentAdminAddProductBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.domain.model.Category;
import com.buyon.domain.model.Product;
import com.buyon.domain.repository.CategoryRepository;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.upload.UploadViewModel;
import com.buyon.ui.util.CategoryIconMapper;
import com.buyon.ui.viewmodel.AdminViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AdminAddProductFragment extends Fragment {

    private FragmentAdminAddProductBinding binding;
    private AdminViewModel vm;
    private UploadViewModel uploadVm;
    private CategoryRepository categoryRepository;
    private String uploadedImageUrl = "";
    private String editingProductId = null;
    private String selectedCategoryId = "";
    private String pendingCategorySelectionId = null;
    private boolean defaultCategorySeedAttempted = false;
    private final List<Category> categories = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed(() -> uploadImage(uri), 100);
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminAddProductBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        vm = new ViewModelProvider(requireActivity(), new BuyonViewModelFactory(deps))
                .get(AdminViewModel.class);
        uploadVm = new ViewModelProvider(this).get(UploadViewModel.class);
        categoryRepository = deps.categoryRepository();

        Bundle args = getArguments();
        if (args != null) {
            editingProductId = args.getString("productId");
        }

        if (editingProductId != null) {
            binding.title.setText("Edit Product");
            loadProductForEditing();
        }

        setupCategoryUi();

        // Clear field errors on text change
        addTextChangeListeners();

        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        binding.cardImage.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        binding.btnSave.setOnClickListener(v -> {
            if (validateForm()) {
                saveProduct();
            }
        });

        binding.btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        vm.getSaveProductState().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            boolean loading = res.getStatus() == Resource.Status.LOADING;
            binding.progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!loading);
            binding.btnSave.setText(loading ? "" : "Save Product");

            if (res.getStatus() == Resource.Status.SUCCESS) {
                AppErrorHandler.showSuccess(view, "Product saved \u2713");
                vm.resetSaveProductState();
                Navigation.findNavController(view).navigateUp();
            }
            if (res.getStatus() == Resource.Status.ERROR) {
                AppErrorHandler.showError(view,
                        AppErrorHandler.getFirebaseErrorMessage(res.getError()));
                vm.resetSaveProductState();
            }
        });

        uploadVm.getUploadState().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;

            boolean loading = res.getStatus() == Resource.Status.LOADING;
            binding.uploadProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!loading);
            if (loading) {
                binding.uploadIcon.setVisibility(View.GONE);
                return;
            }

            if (res.getStatus() == Resource.Status.SUCCESS && res.getData() != null) {
                uploadedImageUrl = res.getData();
                binding.productImagePreview.setVisibility(View.VISIBLE);
                binding.uploadIcon.setVisibility(View.GONE);
                Glide.with(binding.productImagePreview.getContext())
                        .load(uploadedImageUrl)
                        .centerCrop()
                        .into(binding.productImagePreview);
                uploadVm.clearUploadState();
                return;
            }

            if (res.getStatus() == Resource.Status.ERROR) {
                binding.uploadIcon.setVisibility(View.VISIBLE);
                View root = getView();
                if (root != null) {
                    AppErrorHandler.showError(root, AppErrorHandler.getFirebaseErrorMessage(res.getError()));
                }
                uploadVm.clearUploadState();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────

    private boolean validateForm() {
        boolean valid = true;

        String name = getText(binding.inputName);
        if (name.isEmpty()) {
            binding.layoutName.setError("Product name is required");
            valid = false;
        } else {
            binding.layoutName.setError(null);
        }

        String priceStr = getText(binding.inputPrice);
        if (priceStr.isEmpty()) {
            binding.layoutPrice.setError("Price is required");
            valid = false;
        } else {
            try {
                double price = Double.parseDouble(priceStr);
                if (price < 0) {
                    binding.layoutPrice.setError("Price cannot be negative");
                    valid = false;
                } else {
                    binding.layoutPrice.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.layoutPrice.setError("Enter a valid price");
                valid = false;
            }
        }

        if (selectedCategoryId == null || selectedCategoryId.trim().isEmpty()) {
            binding.layoutCategory.setError("Please select a category");
            valid = false;
        } else {
            binding.layoutCategory.setError(null);
        }

        String stockStr = getText(binding.inputStock);
        if (!stockStr.isEmpty()) {
            try {
                int stock = Integer.parseInt(stockStr);
                if (stock < 0) {
                    binding.layoutStock.setError("Stock cannot be negative");
                    valid = false;
                } else {
                    binding.layoutStock.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.layoutStock.setError("Enter a valid whole number");
                valid = false;
            }
        }

        return valid;
    }

    private void addTextChangeListeners() {
        addClearError(binding.inputName,        binding.layoutName);
        addClearError(binding.inputPrice,       binding.layoutPrice);
        addClearError(binding.inputCategory,    binding.layoutCategory);
        addClearError(binding.inputStock,       binding.layoutStock);
        addClearError(binding.inputDescription, binding.layoutDescription);
    }

    private void addClearError(
            android.widget.TextView field,
            com.google.android.material.textfield.TextInputLayout layout) {
        field.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding != null) layout.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Edit — populate form with existing product data
    // ─────────────────────────────────────────────────────────────────────

    private void loadProductForEditing() {
        vm.getProducts().observe(getViewLifecycleOwner(), products -> {
            if (products == null || editingProductId == null) return;
            for (Product p : products) {
                if (p.getId().equals(editingProductId)) {
                    binding.inputName.setText(p.getName());
                    binding.inputPrice.setText(String.valueOf(p.getPrice()));
                    pendingCategorySelectionId = p.getCategoryId();
                    binding.inputStock.setText(String.valueOf(p.getSoldCount()));
                    binding.inputDescription.setText(p.getDescription());
                    uploadedImageUrl = p.getImageUrl() != null ? p.getImageUrl() : "";
                    if (!uploadedImageUrl.isEmpty()) {
                        binding.uploadIcon.setVisibility(View.GONE);
                        binding.productImagePreview.setVisibility(View.VISIBLE);
                        Glide.with(binding.productImagePreview.getContext())
                                .load(uploadedImageUrl)
                                .centerCrop()
                                .into(binding.productImagePreview);
                    }
                    break;
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Image upload
    // ─────────────────────────────────────────────────────────────────────

    private void uploadImage(Uri uri) {
        if (binding == null) return;
        // Show local preview immediately while upload runs.
        binding.productImagePreview.setVisibility(View.VISIBLE);
        binding.uploadIcon.setVisibility(View.GONE);
        Glide.with(binding.productImagePreview.getContext())
                .load(uri)
                .centerCrop()
                .into(binding.productImagePreview);
        uploadVm.uploadImage(uri);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Save
    // ─────────────────────────────────────────────────────────────────────

    private void saveProduct() {
        String name        = getText(binding.inputName);
        double price       = Double.parseDouble(getText(binding.inputPrice));
        String stockStr    = getText(binding.inputStock);
        String description = getText(binding.inputDescription);
        int stock = stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr);

        Product product = new Product(
                editingProductId != null ? editingProductId : "",
                name,
                description,
                price,
                uploadedImageUrl,
                selectedCategoryId,
                4.5,
                stock,
                "");

        vm.saveProduct(product);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private String getText(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void setupCategoryUi() {
        binding.inputCategory.setOnClickListener(v -> binding.inputCategory.showDropDown());
        binding.inputCategory.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < categories.size()) {
                selectedCategoryId = categories.get(position).getId();
                binding.layoutCategory.setError(null);
                updateSelectedCategoryPreview(categories.get(position));
            }
        });

        categoryRepository.startCategoriesListener(new com.buyon.domain.callback.RepositoryListener<List<Category>>() {
            @Override
            public void onData(List<Category> data) {
                categories.clear();
                if (data != null) {
                    categories.addAll(data);
                }
                if (categories.isEmpty() && !defaultCategorySeedAttempted) {
                    defaultCategorySeedAttempted = true;
                    seedDefaultCategories();
                    return;
                }
                bindCategoryDropdown();
            }

            @Override
            public void onError(Throwable error) {
                View root = getView();
                if (root != null) {
                    AppErrorHandler.showError(root, AppErrorHandler.getFirebaseErrorMessage(error));
                }
            }
        });
    }

    private void bindCategoryDropdown() {
        List<String> names = new ArrayList<>();
        for (Category c : categories) {
            names.add(c.getName());
        }
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, names);
        binding.inputCategory.setAdapter(adapter);
        applyPendingCategorySelection();
    }

    private void applyPendingCategorySelection() {
        if (pendingCategorySelectionId == null || pendingCategorySelectionId.trim().isEmpty()) {
            return;
        }
        for (Category c : categories) {
            if (pendingCategorySelectionId.equals(c.getId())) {
                selectedCategoryId = c.getId();
                binding.inputCategory.setText(c.getName(), false);
                pendingCategorySelectionId = null;
                updateSelectedCategoryPreview(c);
                return;
            }
        }
        // For legacy products that used free-text category names.
        selectedCategoryId = pendingCategorySelectionId;
        binding.inputCategory.setText(pendingCategorySelectionId, false);
        updateSelectedCategoryPreview(
                new Category(selectedCategoryId, pendingCategorySelectionId, "tag"));
        pendingCategorySelectionId = null;
    }

    private void seedDefaultCategories() {
        for (Category c : defaultCategories()) {
            categoryRepository.createCategory(c, new com.buyon.domain.callback.DomainCallback<String>() {
                @Override
                public void onSuccess(String result) {}

                @Override
                public void onError(Throwable error) {}
            });
        }
    }

    private List<Category> defaultCategories() {
        List<Category> list = new ArrayList<>();
        list.add(new Category("electronics", "Electronics", "devices"));
        list.add(new Category("fashion", "Fashion", "style"));
        list.add(new Category("home-kitchen", "Home & Kitchen", "home"));
        list.add(new Category("beauty", "Beauty", "sparkles"));
        list.add(new Category("sports", "Sports", "fitness"));
        list.add(new Category("books", "Books", "book"));
        list.add(new Category("toys", "Toys", "toys"));
        list.add(new Category("grocery", "Grocery", "basket"));
        return list;
    }

    private void showAddCategoryDialog() {
        DialogAddCategoryBinding d = DialogAddCategoryBinding.inflate(getLayoutInflater());
        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Add new category")
                        .setView(d.getRoot())
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton("Save", null)
                        .show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = d.inputName.getText() != null ? d.inputName.getText().toString().trim() : "";
                    String icon = d.inputIcon.getText() != null ? d.inputIcon.getText().toString().trim() : "";
                    if (name.length() < 2) {
                        d.layoutName.setError("Enter a valid category name");
                        return;
                    }
                    d.layoutName.setError(null);

                    String id = slug(name);
                    for (Category c : categories) {
                        if (c.getId().equals(id) || c.getName().equalsIgnoreCase(name)) {
                            selectedCategoryId = c.getId();
                            binding.inputCategory.setText(c.getName(), false);
                            updateSelectedCategoryPreview(c);
                            dialog.dismiss();
                            return;
                        }
                    }

                    categoryRepository.createCategory(
                            new Category(id, name, icon.isEmpty() ? "tag" : icon),
                            new com.buyon.domain.callback.DomainCallback<String>() {
                                @Override
                                public void onSuccess(String result) {
                                    selectedCategoryId = result;
                                    binding.inputCategory.setText(name, false);
                                    updateSelectedCategoryPreview(new Category(result, name, icon));
                                    dialog.dismiss();
                                }

                                @Override
                                public void onError(Throwable error) {
                                    View root = getView();
                                    if (root != null) {
                                        AppErrorHandler.showError(
                                                root,
                                                AppErrorHandler.getFirebaseErrorMessage(error));
                                    }
                                }
                            });
                });
    }

    private String slug(String name) {
        String s = name.toLowerCase(Locale.US).trim();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s.isEmpty() ? "category" : s;
    }

    private void updateSelectedCategoryPreview(Category category) {
        if (binding == null || category == null) {
            return;
        }
        binding.selectedCategoryChip.setVisibility(View.VISIBLE);
        binding.selectedCategoryLabel.setText(category.getName());
        binding.selectedCategoryIcon.setImageResource(
                CategoryIconMapper.resolveIconRes(category.getId(), category.getIconName()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (categoryRepository != null) {
            categoryRepository.stopCategoriesListener();
        }
        binding = null;
    }
}
