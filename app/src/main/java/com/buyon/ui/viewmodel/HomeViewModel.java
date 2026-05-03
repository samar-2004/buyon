package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Category;
import com.buyon.domain.model.Product;
import com.buyon.domain.repository.CategoryRepository;
import com.buyon.domain.repository.ProductRepository;

import java.util.List;

public final class HomeViewModel extends ViewModel {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MutableLiveData<List<Product>> products = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public HomeViewModel(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        loadAllProducts();
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
    }

    private void loadAllProducts() {
        productRepository.stopCategoryListener();
        productRepository.startProductsListener(
                new RepositoryListener<List<Product>>() {
                    @Override
                    public void onData(List<Product> data) {
                        products.postValue(data);
                    }

                    @Override
                    public void onError(Throwable error) {
                        errorMessage.postValue(error.getMessage());
                    }
                });
    }

    /** Triggers a manual reload of products and categories. */
    public void refresh() {
        loadAllProducts();
    }

    public void setCategoryFilter(String categoryId) {
        productRepository.stopProductsListener();
        productRepository.stopCategoryListener();
        if (categoryId == null || categoryId.isEmpty()) {
            loadAllProducts();
        } else {
            productRepository.startProductsByCategoryListener(
                    categoryId,
                    new RepositoryListener<List<Product>>() {
                        @Override
                        public void onData(List<Product> data) {
                            products.postValue(data);
                        }

                        @Override
                        public void onError(Throwable error) {
                            errorMessage.postValue(error.getMessage());
                        }
                    });
        }
    }

    public LiveData<List<Product>> getProducts() {
        return products;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        productRepository.stopProductsListener();
        productRepository.stopCategoryListener();
        categoryRepository.stopCategoriesListener();
    }
}
