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
import com.buyon.domain.repository.CartRepository;
import com.buyon.domain.repository.OrderRepository;
import com.buyon.domain.repository.PaymentRepository;
import com.buyon.ui.pricing.OrderPricing;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the online payment flow:
 *  1. Loads cart items via real-time listener.
 *  2. Exposes amount for display.
 *  3. initiatePayment()  → Firestore payment record, paymentId.
 *  4. confirmAndPlaceOrder() → mark PAID + create order + clear cart.
 *  5. markPaymentFailed() → mark FAILED.
 */
public final class PaymentViewModel extends ViewModel {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final CartRepository    cartRepository;

    private final MutableLiveData<Resource<String>> paymentInitResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> orderResult       = new MutableLiveData<>();
    private final MutableLiveData<Double>           totalAmount       = new MutableLiveData<>();
    private final MutableLiveData<String>           cartError         = new MutableLiveData<>();

    private String        userId;
    private List<CartItem> cartItems = new ArrayList<>();
    private String        shippingAddress;
    private String        promoCode;
    private PaymentMethod selectedMethod;
    private String        currentPaymentId;
    private String        customerDisplayName = "";
    private String        customerEmail = "";
    private String        customerPhone = "";

    public PaymentViewModel(
            PaymentRepository paymentRepository,
            OrderRepository   orderRepository,
            CartRepository    cartRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository   = orderRepository;
        this.cartRepository    = cartRepository;
    }

    public void init(
            String userId,
            String shippingAddress,
            String promoCode,
            PaymentMethod method,
            String customerDisplayName,
            String customerEmail,
            String customerPhone) {
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.promoCode = promoCode;
        this.selectedMethod = method;
        this.customerDisplayName = customerDisplayName != null ? customerDisplayName : "";
        this.customerEmail = customerEmail != null ? customerEmail : "";
        this.customerPhone = customerPhone != null ? customerPhone : "";

        // Subscribe to cart for the amount display
        cartRepository.startCartListener(userId, new RepositoryListener<List<CartItem>>() {
            @Override public void onData(List<CartItem> data) {
                cartItems = data != null ? data : new ArrayList<>();
                totalAmount.postValue(calculateTotal(cartItems));
            }
            @Override public void onError(Throwable e) {
                String msg = e != null && e.getMessage() != null
                        ? e.getMessage() : "Failed to load cart. Please go back and try again.";
                cartError.postValue(msg);
            }
        });
    }

    public LiveData<Resource<String>> getPaymentInitResult() { return paymentInitResult; }
    public LiveData<Resource<String>> getOrderResult()       { return orderResult; }
    public LiveData<Double>           getTotalAmount()       { return totalAmount; }
    public LiveData<String>           getCartError()         { return cartError; }
    public String                     getCurrentPaymentId()  { return currentPaymentId; }

    /** Step 1: record payment intent in Firestore */
    public void initiatePayment() {
        if (userId == null) {
            paymentInitResult.setValue(Resource.error(new IllegalStateException("Not signed in. Please log in and try again.")));
            return;
        }
        double total = calculateTotal(cartItems);
        paymentInitResult.setValue(Resource.loading());
        paymentRepository.initiatePayment(userId, total, selectedMethod, new DomainCallback<String>() {
            @Override public void onSuccess(String paymentId) {
                currentPaymentId = paymentId;
                paymentInitResult.postValue(Resource.success(paymentId));
            }
            @Override public void onError(Throwable error) {
                paymentInitResult.postValue(Resource.error(error));
            }
        });
    }

    /** Step 2 (success path): confirm payment, then place order */
    public void confirmAndPlaceOrder(String gatewayTransactionRef) {
        orderResult.setValue(Resource.loading());
        if (currentPaymentId == null) {
            orderResult.setValue(Resource.error(new IllegalStateException("Payment session expired. Please start over.")));
            return;
        }
        paymentRepository.confirmPayment(currentPaymentId, gatewayTransactionRef, new DomainCallback<String>() {
            @Override public void onSuccess(String txRef) { placeOrderInternal(txRef, PaymentStatus.PAID); }
            @Override public void onError(Throwable error) { orderResult.postValue(Resource.error(error)); }
        });
    }

    /** Step 2 (failure path): mark failed in Firestore */
    public void markPaymentFailed() {
        if (currentPaymentId == null) return;
        paymentRepository.failPayment(currentPaymentId, new DomainCallback<Void>() {
            @Override public void onSuccess(Void r) {}
            @Override public void onError(Throwable e) {}
        });
    }

    // ── Internal ────────────────────────────────────────────────────────

    private void placeOrderInternal(String txRef, PaymentStatus pStatus) {
        List<OrderLine> lines = new ArrayList<>();
        for (CartItem c : cartItems) {
            lines.add(new OrderLine(c.getProductId(), c.getName(), c.getUnitPrice(), c.getQuantity()));
        }
        orderRepository.placeOrder(
                userId, lines, shippingAddress, promoCode, OrderPricing.SHIPPING_FEE_USD,
                selectedMethod, pStatus, txRef,
                customerDisplayName, customerEmail, customerPhone,
                new DomainCallback<String>() {
                    @Override public void onSuccess(String orderId) {
                        cartRepository.clearCart(userId, new DomainCallback<Void>() {
                            @Override public void onSuccess(Void r)    { orderResult.postValue(Resource.success(orderId)); }
                            @Override public void onError(Throwable e) { orderResult.postValue(Resource.success(orderId)); }
                        });
                    }
                    @Override public void onError(Throwable error) {
                        orderResult.postValue(Resource.error(error));
                    }
                });
    }

    private double calculateTotal(List<CartItem> items) {
        if (items == null) return 0;
        double sub = 0;
        for (CartItem c : items) sub += c.getLineTotal();
        double disc = OrderPricing.discount(promoCode, sub);
        return sub - disc + OrderPricing.SHIPPING_FEE_USD;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cartRepository.stopCartListener();
    }
}
