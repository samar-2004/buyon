package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Order;
import com.buyon.domain.repository.AuthRepository;
import com.buyon.domain.repository.OrderRepository;

import java.util.List;

public final class OrdersViewModel extends ViewModel {

    private final OrderRepository orderRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public OrdersViewModel(OrderRepository orderRepository, AuthRepository authRepository) {
        this.orderRepository = orderRepository;
        this.authRepository = authRepository;
        String uid = authRepository.currentUserId();
        if (uid != null) {
            orderRepository.startOrdersListener(
                    uid,
                    new RepositoryListener<List<Order>>() {
                        @Override
                        public void onData(List<Order> data) {
                            orders.postValue(data);
                        }

                        @Override
                        public void onError(Throwable error) {
                            errorMessage.postValue(error.getMessage());
                        }
                    });
        }
    }

    public LiveData<List<Order>> getOrders() {
        return orders;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        orderRepository.stopOrdersListener();
    }
}
