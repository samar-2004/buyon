package com.buyon.domain.repository;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Order;
import com.buyon.domain.model.OrderLine;
import com.buyon.domain.model.PaymentMethod;
import com.buyon.domain.model.PaymentStatus;

import java.util.List;

public interface OrderRepository {

    void startOrdersListener(String userId, RepositoryListener<List<Order>> listener);

    void stopOrdersListener();

    /**
     * Creates an order document in Firestore.
     *
     * @param paymentMethod   Selected payment method (COD / JazzCash / EasyPaisa).
     * @param paymentStatus   Initial status — PENDING for COD, PAID for verified online payment.
     * @param paymentReference Transaction reference returned by the gateway; empty string for COD.
     */
    void placeOrder(
            String userId,
            List<OrderLine> lines,
            String shippingAddress,
            String promoCode,
            double shippingFee,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            String paymentReference,
            String customerDisplayName,
            String customerEmail,
            String customerPhone,
            DomainCallback<String> callback);
}
