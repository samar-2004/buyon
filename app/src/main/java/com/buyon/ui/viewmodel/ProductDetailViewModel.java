package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.core.resource.Resource;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.CartItem;
import com.buyon.domain.model.Product;
import com.buyon.domain.repository.AuthRepository;
import com.buyon.domain.repository.CartRepository;
import com.buyon.domain.repository.ProductRepository;

public final class ProductDetailViewModel extends ViewModel {

    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final AuthRepository authRepository;
    private final String productId;
    private final MutableLiveData<Product> product = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> cartAction = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProductDetailViewModel(
            ProductRepository productRepository,
            CartRepository cartRepository,
            AuthRepository authRepository,
            String productId) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.authRepository = authRepository;
        this.productId = productId;
        load();
    }

    private void load() {
        productRepository.startProductListener(
                productId,
                new RepositoryListener<Product>() {
                    @Override
                    public void onData(Product data) {
                        product.postValue(data);
                    }

                    @Override
                    public void onError(Throwable error) {
                        errorMessage.postValue(error.getMessage());
                    }
                });
    }

    public LiveData<Product> getProduct() {
        return product;
    }

    public LiveData<Resource<Void>> getCartAction() {
        return cartAction;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void addToCart(int quantity) {
        String uid = authRepository.currentUserId();
        if (uid == null) {
            cartAction.setValue(Resource.error(new IllegalStateException("Sign in required")));
            return;
        }
        Product p = product.getValue();
        if (p == null) {
            return;
        }
        cartAction.setValue(Resource.loading());
        CartItem item =
                new CartItem(
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getImageUrl(),
                        quantity);
        cartRepository.addOrUpdateItem(
                uid,
                item,
                new DomainCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        cartAction.postValue(Resource.success(null));
                    }

                    @Override
                    public void onError(Throwable error) {
                        cartAction.postValue(Resource.error(error));
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        productRepository.stopProductListener();
    }
}
