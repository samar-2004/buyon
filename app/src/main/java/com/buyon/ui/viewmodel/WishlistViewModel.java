package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Product;
import com.buyon.domain.repository.AuthRepository;
import com.buyon.domain.repository.ProductRepository;
import com.buyon.domain.repository.WishlistRepository;

import java.util.ArrayList;
import java.util.List;

public final class WishlistViewModel extends ViewModel {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository  productRepository;
    private final AuthRepository     authRepository;

    private final MutableLiveData<List<String>>  wishlistIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Product>> products    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>       loading     = new MutableLiveData<>(true);
    private final MutableLiveData<String>        error       = new MutableLiveData<>();

    public WishlistViewModel(WishlistRepository wishlistRepository,
                             ProductRepository  productRepository,
                             AuthRepository     authRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository  = productRepository;
        this.authRepository     = authRepository;

        String uid = authRepository.currentUserId();
        if (uid != null) startListening(uid);
    }

    public LiveData<List<String>>  getWishlistIds() { return wishlistIds; }
    public LiveData<List<Product>> getProducts()    { return products; }
    public LiveData<Boolean>       getLoading()     { return loading; }
    public LiveData<String>        getError()       { return error; }

    public void clearError() {
        error.setValue(null);
    }

    public void setWishlisted(Product product, boolean shouldWishlist, DomainCallback<Void> callback) {
        String uid = authRepository.currentUserId();
        if (uid == null) {
            IllegalStateException e = new IllegalStateException("Please sign in to use wishlist.");
            error.postValue(e.getMessage());
            callback.onError(e);
            return;
        }
        if (product == null || product.getId() == null || product.getId().isEmpty()) {
            IllegalArgumentException e = new IllegalArgumentException("Invalid product.");
            error.postValue(e.getMessage());
            callback.onError(e);
            return;
        }

        DomainCallback<Void> forwarding = new DomainCallback<Void>() {
            @Override public void onSuccess(Void r) { callback.onSuccess(r); }
            @Override public void onError(Throwable e) {
                error.postValue(e.getMessage());
                callback.onError(e);
            }
        };

        if (shouldWishlist) {
            wishlistRepository.addToWishlist(uid, product, forwarding);
        } else {
            wishlistRepository.removeFromWishlist(uid, product.getId(), forwarding);
        }
    }

    public void removeFromWishlist(String productId) {
        String uid = authRepository.currentUserId();
        if (uid == null) return;
        wishlistRepository.removeFromWishlist(uid, productId, new DomainCallback<Void>() {
            @Override public void onSuccess(Void r) {}
            @Override public void onError(Throwable e) { error.postValue(e.getMessage()); }
        });
    }

    private void startListening(String uid) {
        loading.postValue(true);
        wishlistRepository.startWishlistListener(uid, new RepositoryListener<List<String>>() {
            @Override public void onData(List<String> data) {
                wishlistIds.postValue(data);
                loadProducts(data);
            }
            @Override public void onError(Throwable e) {
                error.postValue(e.getMessage());
                loading.postValue(false);
            }
        });
    }

    private void loadProducts(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            products.postValue(new ArrayList<>());
            loading.postValue(false);
            return;
        }
        loading.postValue(true);
        productRepository.fetchProductsByIds(ids, new DomainCallback<List<Product>>() {
            @Override public void onSuccess(List<Product> result) {
                products.postValue(result);
                loading.postValue(false);
            }

            @Override public void onError(Throwable e) {
                error.postValue(e.getMessage());
                loading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        wishlistRepository.stopWishlistListener();
    }
}
