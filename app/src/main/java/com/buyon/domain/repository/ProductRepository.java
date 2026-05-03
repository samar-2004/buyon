package com.buyon.domain.repository;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Product;

import java.util.List;

public interface ProductRepository {
    void startProductsListener(RepositoryListener<List<Product>> listener);

    void stopProductsListener();

    void startProductListener(String productId, RepositoryListener<Product> listener);

    void stopProductListener();

    void searchProducts(String query, DomainCallback<List<Product>> callback);

    void startProductsByCategoryListener(String categoryId, RepositoryListener<List<Product>> listener);

    void stopCategoryListener();

    /** Fetches a specific set of products by their document IDs — used by the wishlist screen. */
    void fetchProductsByIds(List<String> productIds, DomainCallback<List<Product>> callback);
}
