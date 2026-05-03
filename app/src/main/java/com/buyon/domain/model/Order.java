package com.buyon.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Order {

    public enum Status {
        PENDING,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }

    private final String id;
    private final String userId;
    private final long createdAtMillis;
    private final double subtotal;
    private final double shippingFee;
    private final double total;
    private final String shippingAddress;
    private final String promoCode;
    private final Status status;
    private final List<OrderLine> lines;

    /** Snapshot of the buyer at checkout (for admin / support). */
    private final String customerDisplayName;
    private final String customerEmail;
    private final String customerPhone;

    private final String paymentMethod;
    private final String paymentStatus;
    private final String paymentReference;
    private final String cancellationReason;

    public Order(
            String id,
            String userId,
            long createdAtMillis,
            double subtotal,
            double shippingFee,
            double total,
            String shippingAddress,
            String promoCode,
            Status status,
            List<OrderLine> lines,
            String customerDisplayName,
            String customerEmail,
            String customerPhone,
            String paymentMethod,
            String paymentStatus,
            String paymentReference,
            String cancellationReason) {
        this.id = id;
        this.userId = userId;
        this.createdAtMillis = createdAtMillis;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.total = total;
        this.shippingAddress = shippingAddress;
        this.promoCode = promoCode;
        this.status = status;
        this.lines = Collections.unmodifiableList(lines);
        this.customerDisplayName = customerDisplayName != null ? customerDisplayName : "";
        this.customerEmail = customerEmail != null ? customerEmail : "";
        this.customerPhone = customerPhone != null ? customerPhone : "";
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.COD.name();
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.PENDING.name();
        this.paymentReference = paymentReference != null ? paymentReference : "";
        this.cancellationReason = cancellationReason != null ? cancellationReason : "";
    }

    /** Legacy — no customer snapshot (older Firestore documents). */
    public Order(
            String id,
            String userId,
            long createdAtMillis,
            double subtotal,
            double shippingFee,
            double total,
            String shippingAddress,
            String promoCode,
            Status status,
            List<OrderLine> lines,
            String paymentMethod,
            String paymentStatus,
            String paymentReference) {
        this(
                id,
                userId,
                createdAtMillis,
                subtotal,
                shippingFee,
                total,
                shippingAddress,
                promoCode,
                status,
                lines,
                "",
                "",
                "",
                paymentMethod,
                paymentStatus,
                paymentReference,
                "");
    }

    /** Legacy — COD / PENDING defaults, no customer snapshot. */
    public Order(
            String id,
            String userId,
            long createdAtMillis,
            double subtotal,
            double shippingFee,
            double total,
            String shippingAddress,
            String promoCode,
            Status status,
            List<OrderLine> lines) {
        this(
                id,
                userId,
                createdAtMillis,
                subtotal,
                shippingFee,
                total,
                shippingAddress,
                promoCode,
                status,
                lines,
                "",
                "",
                "",
                PaymentMethod.COD.name(),
                PaymentStatus.PENDING.name(),
                "",
                "");
    }

    public Order(
            String id,
            String userId,
            long createdAtMillis,
            double subtotal,
            double shippingFee,
            double total,
            String shippingAddress,
            String promoCode,
            Status status,
            List<OrderLine> lines,
            String customerDisplayName,
            String customerEmail,
            String customerPhone,
            String paymentMethod,
            String paymentStatus,
            String paymentReference) {
        this(
                id,
                userId,
                createdAtMillis,
                subtotal,
                shippingFee,
                total,
                shippingAddress,
                promoCode,
                status,
                lines,
                customerDisplayName,
                customerEmail,
                customerPhone,
                paymentMethod,
                paymentStatus,
                paymentReference,
                "");
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getShippingFee() {
        return shippingFee;
    }

    public double getTotal() {
        return total;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public Status getStatus() {
        return status;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    public String getCustomerDisplayName() {
        return customerDisplayName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public PaymentMethod paymentMethodEnum() {
        return PaymentMethod.fromString(paymentMethod);
    }

    public PaymentStatus paymentStatusEnum() {
        return PaymentStatus.fromString(paymentStatus);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return createdAtMillis == order.createdAtMillis
                && Double.compare(order.subtotal, subtotal) == 0
                && Double.compare(order.shippingFee, shippingFee) == 0
                && Double.compare(order.total, total) == 0
                && status == order.status
                && Objects.equals(id, order.id)
                && Objects.equals(userId, order.userId)
                && Objects.equals(shippingAddress, order.shippingAddress)
                && Objects.equals(promoCode, order.promoCode)
                && Objects.equals(lines, order.lines)
                && Objects.equals(customerDisplayName, order.customerDisplayName)
                && Objects.equals(customerEmail, order.customerEmail)
                && Objects.equals(customerPhone, order.customerPhone)
                && Objects.equals(paymentMethod, order.paymentMethod)
                && Objects.equals(paymentStatus, order.paymentStatus)
                && Objects.equals(paymentReference, order.paymentReference)
                && Objects.equals(cancellationReason, order.cancellationReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                userId,
                createdAtMillis,
                subtotal,
                shippingFee,
                total,
                shippingAddress,
                promoCode,
                status,
                lines,
                customerDisplayName,
                customerEmail,
                customerPhone,
                paymentMethod,
                paymentStatus,
                paymentReference,
                cancellationReason);
    }
}
