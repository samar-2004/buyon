package com.buyon.domain.repository;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Product;

import java.util.List;

public interface WishlistRepository {
    /** Starts a real-time listener that emits the list of wishlisted product IDs. */
    void startWishlistListener(String userId, RepositoryListener<List<String>> listener);
    void stopWishlistListener();

    void addToWishlist(String userId, Product product, DomainCallback<Void> callback);
    void removeFromWishlist(String userId, String productId, DomainCallback<Void> callback);

    /** One-shot check — used by ProductDetailViewModel to set initial heart state. */
    void isWishlisted(String userId, String productId, DomainCallback<Boolean> callback);
}
