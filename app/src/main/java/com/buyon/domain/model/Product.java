package com.buyon.domain.model;

import java.util.Objects;

public final class Product {
    private final String id;
    private final String name;
    private final String description;
    private final double price;
    private final String imageUrl;
    private final String categoryId;
    private final double rating;
    private final int soldCount;
    private final String locationLabel;

    public Product(
            String id,
            String name,
            String description,
            double price,
            String imageUrl,
            String categoryId,
            double rating,
            int soldCount,
            String locationLabel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.rating = rating;
        this.soldCount = soldCount;
        this.locationLabel = locationLabel;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public double getRating() {
        return rating;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
