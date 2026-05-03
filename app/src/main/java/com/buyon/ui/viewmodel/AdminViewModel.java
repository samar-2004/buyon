package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.core.resource.Resource;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.model.Order;
import com.buyon.domain.model.Product;
import com.buyon.domain.model.UserProfile;
import com.buyon.domain.repository.AdminRepository;

import java.util.List;

public final class AdminViewModel extends ViewModel {

    private final AdminRepository adminRepository;

    private final MutableLiveData<List<Product>> products = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>();
    private final MutableLiveData<List<UserProfile>> users = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> saveProductState = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> deleteProductState = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> updateOrderState = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AdminViewModel(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
        startListeners();
    }

    private void startListeners() {
        adminRepository.listenAllProducts(new com.buyon.domain.callback.RepositoryListener<List<Product>>() {
            @Override
            public void onData(List<Product> data) {
                products.postValue(data);
            }

            @Override
            public void onError(Throwable error) {
                errorMessage.postValue(error.getMessage());
            }
        });

        adminRepository.listenAllOrders(new com.buyon.domain.callback.RepositoryListener<List<Order>>() {
            @Override
            public void onData(List<Order> data) {
                orders.postValue(data);
            }

            @Override
            public void onError(Throwable error) {
                errorMessage.postValue(error.getMessage());
            }
        });

        adminRepository.listenAllUsers(new com.buyon.domain.callback.RepositoryListener<List<UserProfile>>() {
            @Override
            public void onData(List<UserProfile> data) {
                users.postValue(data);
            }

            @Override
            public void onError(Throwable error) {
                errorMessage.postValue(error.getMessage());
            }
        });
    }

    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<List<Order>> getOrders() { return orders; }
    public LiveData<List<UserProfile>> getUsers() { return users; }
    public LiveData<Resource<String>> getSaveProductState() { return saveProductState; }

    public void resetSaveProductState() {
        saveProductState.setValue(null);
    }

    public LiveData<Resource<Void>> getDeleteProductState() { return deleteProductState; }
    public LiveData<Resource<Void>> getUpdateOrderState() { return updateOrderState; }

    public void clearUpdateOrderState() {
        updateOrderState.setValue(null);
    }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void saveProduct(Product product) {
        saveProductState.setValue(Resource.loading());
        adminRepository.saveProduct(product, new DomainCallback<String>() {
            @Override
            public void onSuccess(String result) {
                saveProductState.postValue(Resource.success(result));
            }

            @Override
            public void onError(Throwable error) {
                saveProductState.postValue(Resource.error(error));
            }
        });
    }

    public void deleteProduct(String productId) {
        deleteProductState.setValue(Resource.loading());
        adminRepository.deleteProduct(productId, new DomainCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                deleteProductState.postValue(Resource.success(null));
            }

            @Override
            public void onError(Throwable error) {
                deleteProductState.postValue(Resource.error(error));
            }
        });
    }

    public void updateOrderStatus(String orderId, String status) {
        updateOrderStatus(orderId, status, null);
    }

    public void updateOrderStatus(String orderId, String status, String cancellationReason) {
        updateOrderState.setValue(Resource.loading());
        adminRepository.updateOrderStatus(orderId, status, cancellationReason, new DomainCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateOrderState.postValue(Resource.success(null));
            }

            @Override
            public void onError(Throwable error) {
                updateOrderState.postValue(Resource.error(error));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        adminRepository.stopListening();
    }
}
