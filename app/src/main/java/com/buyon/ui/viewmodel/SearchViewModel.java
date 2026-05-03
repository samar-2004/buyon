package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Category;
import com.buyon.domain.model.Product;
import com.buyon.domain.repository.CategoryRepository;
import com.buyon.domain.repository.ProductRepository;

import java.util.List;

public final class SearchViewModel extends ViewModel {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MutableLiveData<List<Product>> products = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private String selectedCategoryId = "";
    private String currentQuery = "";

    public SearchViewModel(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        categoryRepository.startCategoriesListener(
                new RepositoryListener<List<Category>>() {
                    @Override
                    public void onData(List<Category> data) {
                        categories.postValue(data);
                    }

                    @Override
                    public void onError(Throwable error) {
                        errorMessage.postValue(error.getMessage());
                    }
                });
        loadAllProducts();
    }

    private void loadAllProducts() {
        isLoading.postValue(true);
        productRepository.searchProducts(
                "",
                new DomainCallback<List<Product>>() {
                    @Override
                    public void onSuccess(List<Product> result) {
                        isLoading.postValue(false);
                        products.postValue(result);
                    }

                    @Override
                    public void onError(Throwable error) {
                        isLoading.postValue(false);
                        errorMessage.postValue(error.getMessage());
                    }
                });
    }

    private void reloadProducts() {
        isLoading.postValue(true);
        productRepository.stopCategoryListener();
        productRepository.searchProducts(
                currentQuery,
                new DomainCallback<List<Product>>() {
                    @Override
                    public void onSuccess(List<Product> result) {
                        isLoading.postValue(false);
                        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) {
                            products.postValue(result);
                            return;
                        }
                        java.util.List<Product> filtered = new java.util.ArrayList<>();
                        if (result != null) {
                            for (Product p : result) {
                                if (selectedCategoryId.equals(p.getCategoryId())) {
                                    filtered.add(p);
                                }
                            }
                        }
                        products.postValue(filtered);
                    }

                    @Override
                    public void onError(Throwable error) {
                        isLoading.postValue(false);
                        errorMessage.postValue(error.getMessage());
                    }
                });
    }

    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getLoading() { return isLoading; }

    public void setCategoryFilter(String categoryId) {
        selectedCategoryId = categoryId != null ? categoryId : "";
        reloadProducts();
    }

    public void search(String query) {
        currentQuery = query != null ? query : "";
        reloadProducts();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        productRepository.stopCategoryListener();
        categoryRepository.stopCategoriesListener();
    }
}
