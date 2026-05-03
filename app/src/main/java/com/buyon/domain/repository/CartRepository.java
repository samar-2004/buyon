package com.buyon.domain.repository;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.CartItem;

import java.util.List;

public interface CartRepository {
    void startCartListener(String userId, RepositoryListener<List<CartItem>> listener);

    void stopCartListener();

    void addOrUpdateItem(String userId, CartItem item, DomainCallback<Void> callback);

    void removeItem(String userId, String productId, DomainCallback<Void> callback);

    void clearCart(String userId, DomainCallback<Void> callback);
}
