package com.buyon.domain.model;

import java.util.Objects;

public final class Category {
    private final String id;
    private final String name;
    private final String iconName;

    public Category(String id, String name, String iconName) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIconName() {
        return iconName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
