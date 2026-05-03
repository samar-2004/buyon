package com.buyon.domain.model;

import java.util.Objects;

public final class CartItem {
    private final String productId;
    private final String name;
    private final double unitPrice;
    private final String imageUrl;
    private final int quantity;

    public CartItem(String productId, String name, double unitPrice, String imageUrl, int quantity) {
        this.productId = productId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.imageUrl = imageUrl;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getLineTotal() {
        return unitPrice * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem)) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(productId, cartItem.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}
