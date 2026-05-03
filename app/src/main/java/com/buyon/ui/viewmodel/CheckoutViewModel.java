package com.buyon.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.buyon.core.resource.Resource;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.CartItem;
import com.buyon.domain.model.OrderLine;
import com.buyon.domain.model.PaymentMethod;
import com.buyon.domain.model.PaymentStatus;
import com.buyon.domain.model.UserProfile;
import com.buyon.domain.repository.AuthRepository;
import com.buyon.domain.repository.CartRepository;
import com.buyon.domain.repository.OrderRepository;
import com.buyon.domain.repository.UserProfileRepository;
import com.buyon.ui.pricing.OrderPricing;

import java.util.ArrayList;
import java.util.List;

public final class CheckoutViewModel extends ViewModel {

    private final CartRepository         cartRepository;
    private final OrderRepository        orderRepository;
    private final UserProfileRepository  userProfileRepository;
    private final AuthRepository         authRepository;

    private final MutableLiveData<UserProfile>         profile          = new MutableLiveData<>();
    private final MutableLiveData<List<CartItem>>      cartItems        = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>>    placeOrderResult = new MutableLiveData<>();
    private final MutableLiveData<String>              errorMessage     = new MutableLiveData<>();

    public CheckoutViewModel(
            CartRepository cartRepository,
            OrderRepository orderRepository,
            UserProfileRepository userProfileRepository,
            AuthRepository authRepository) {
        this.cartRepository        = cartRepository;
        this.orderRepository       = orderRepository;
        this.userProfileRepository = userProfileRepository;
        this.authRepository        = authRepository;

        String uid = authRepository.currentUserId();
        if (uid != null) {
            userProfileRepository.startProfileListener(uid, new RepositoryListener<UserProfile>() {
                @Override public void onData(UserProfile data) { profile.postValue(data); }
                @Override public void onError(Throwable error) { errorMessage.postValue(error.getMessage()); }
            });
            cartRepository.startCartListener(uid, new RepositoryListener<List<CartItem>>() {
                @Override public void onData(List<CartItem> data) { cartItems.postValue(data); }
                @Override public void onError(Throwable error) { errorMessage.postValue(error.getMessage()); }
            });
        }
    }

    public LiveData<UserProfile>        getProfile()          { return profile; }
    public LiveData<List<CartItem>>     getCartItems()        { return cartItems; }
    public LiveData<Resource<String>>   getPlaceOrderResult() { return placeOrderResult; }
    public LiveData<String>             getErrorMessage()     { return errorMessage; }
    public double                       getShippingFee()      { return OrderPricing.SHIPPING_FEE_USD; }

    /**
     * Place a COD order directly, or — for online methods — this should only be called
     * after the PaymentViewModel has confirmed payment (the Fragment navigates to
     * PaymentFragment for online methods instead of calling this).
     */
    public void placeOrder(String shippingAddress, String promoCode, PaymentMethod method) {
        String uid   = authRepository.currentUserId();
        List<CartItem> items = cartItems.getValue();
        if (uid == null || items == null || items.isEmpty()) {
            placeOrderResult.setValue(Resource.error(new IllegalStateException("Cart is empty")));
            return;
        }
        String address = (shippingAddress != null && !shippingAddress.isEmpty())
                ? shippingAddress
                : (profile.getValue() != null ? profile.getValue().getDefaultShippingAddress() : "");
        if (address == null || address.isEmpty()) {
            placeOrderResult.setValue(Resource.error(new IllegalStateException("Please enter a shipping address")));
            return;
        }
        List<OrderLine> lines = new ArrayList<>();
        for (CartItem c : items) {
            lines.add(new OrderLine(c.getProductId(), c.getName(), c.getUnitPrice(), c.getQuantity()));
        }
        // For COD, payment status is PENDING (collect on delivery).
        PaymentStatus pStatus = method.isOnline() ? PaymentStatus.PAID : PaymentStatus.PENDING;

        placeOrderResult.setValue(Resource.loading());
        UserProfile p = profile.getValue();
        String custName = p != null && p.getDisplayName() != null ? p.getDisplayName().trim() : "";
        String custPhone = p != null && p.getPhone() != null ? p.getPhone().trim() : "";
        String custEmail = "";
        if (p != null && p.getEmail() != null && !p.getEmail().trim().isEmpty()) {
            custEmail = p.getEmail().trim();
        } else {
            String authMail = authRepository.currentUserEmail();
            custEmail = authMail != null ? authMail.trim() : "";
        }

        orderRepository.placeOrder(
                uid, lines, address,
                promoCode != null ? promoCode : "",
                OrderPricing.SHIPPING_FEE_USD, method, pStatus, "",
                custName, custEmail, custPhone,
                new DomainCallback<String>() {
                    @Override public void onSuccess(String orderId) {
                        cartRepository.clearCart(uid, new DomainCallback<Void>() {
                            @Override public void onSuccess(Void r)     { placeOrderResult.postValue(Resource.success(orderId)); }
                            @Override public void onError(Throwable e)  { placeOrderResult.postValue(Resource.success(orderId)); }
                        });
                    }
                    @Override public void onError(Throwable error) {
                        placeOrderResult.postValue(Resource.error(error));
                    }
                });
    }

    /** Convenience overload — defaults to COD (backward-compat). */
    public void placeOrder(String shippingAddress, String promoCode) {
        placeOrder(shippingAddress, promoCode, PaymentMethod.COD);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userProfileRepository.stopProfileListener();
        cartRepository.stopCartListener();
    }
}
