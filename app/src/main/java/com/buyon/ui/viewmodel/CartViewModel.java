package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.core.resource.Resource;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.CartItem;
import com.buyon.domain.repository.AuthRepository;
import com.buyon.domain.repository.CartRepository;

import java.util.List;

public final class CartViewModel extends ViewModel {

    private final CartRepository cartRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<List<CartItem>> items = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> action = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public CartViewModel(CartRepository cartRepository, AuthRepository authRepository) {
        this.cartRepository = cartRepository;
        this.authRepository = authRepository;
        String uid = authRepository.currentUserId();
        if (uid != null) {
            start(uid);
        }
    }

    public void refresh() {
        String uid = authRepository.currentUserId();
        cartRepository.stopCartListener();
        if (uid != null) {
            start(uid);
        } else {
            items.postValue(null);
        }
    }

    private void start(String uid) {
        cartRepository.startCartListener(
                uid,
                new RepositoryListener<List<CartItem>>() {
                    @Override
                    public void onData(List<CartItem> data) {
                        items.postValue(data);
                    }

                    @Override
                    public void onError(Throwable error) {
                        errorMessage.postValue(error.getMessage());
                    }
                });
    }

    public LiveData<List<CartItem>> getItems() {
        return items;
    }

    public LiveData<Resource<Void>> getAction() {
        return action;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void updateQuantity(CartItem item, int newQty) {
        String uid = authRepository.currentUserId();
        if (uid == null || newQty < 1) {
            return;
        }
        action.setValue(Resource.loading());
        CartItem updated =
                new CartItem(
                        item.getProductId(),
                        item.getName(),
                        item.getUnitPrice(),
                        item.getImageUrl(),
                        newQty);
        cartRepository.addOrUpdateItem(
                uid,
                updated,
                new DomainCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        action.postValue(Resource.success(null));
                    }

                    @Override
                    public void onError(Throwable error) {
                        action.postValue(Resource.error(error));
                    }
                });
    }

    public void remove(CartItem item) {
        String uid = authRepository.currentUserId();
        if (uid == null) {
            return;
        }
        cartRepository.removeItem(
                uid,
                item.getProductId(),
                new DomainCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        action.postValue(Resource.success(null));
                    }

                    @Override
                    public void onError(Throwable error) {
                        action.postValue(Resource.error(error));
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cartRepository.stopCartListener();
    }
}
