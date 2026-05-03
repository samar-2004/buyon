package com.buyon.domain.model;

import java.util.Objects;

public final class OrderLine {
    private final String productId;
    private final String name;
    private final double unitPrice;
    private final int quantity;

    public OrderLine(String productId, String name, double unitPrice, int quantity) {
        this.productId = productId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderLine)) return false;
        OrderLine orderLine = (OrderLine) o;
        return Double.compare(orderLine.unitPrice, unitPrice) == 0
                && quantity == orderLine.quantity
                && Objects.equals(productId, orderLine.productId)
                && Objects.equals(name, orderLine.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, name, unitPrice, quantity);
    }
}
