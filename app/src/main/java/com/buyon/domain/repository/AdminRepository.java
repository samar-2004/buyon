package com.buyon.domain.repository;

import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Order;
import com.buyon.domain.model.Product;
import com.buyon.domain.model.UserProfile;

import java.util.List;

public interface AdminRepository {
    void listenAllProducts(RepositoryListener<List<Product>> listener);
    void listenAllOrders(RepositoryListener<List<Order>> listener);
    void listenAllUsers(RepositoryListener<List<UserProfile>> listener);
    void saveProduct(Product product, com.buyon.domain.callback.DomainCallback<String> callback);
    void deleteProduct(String productId, com.buyon.domain.callback.DomainCallback<Void> callback);
    void updateOrderStatus(
            String orderId,
            String status,
            String cancellationReason,
            com.buyon.domain.callback.DomainCallback<Void> callback);
    void stopListening();
}
