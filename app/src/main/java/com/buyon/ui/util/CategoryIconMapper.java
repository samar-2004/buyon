package com.buyon.ui.util;

import com.buyon.app.R;

import java.util.Locale;

public final class CategoryIconMapper {

    private CategoryIconMapper() {}

    public static int resolveIconRes(String categoryId, String iconName) {
        String key = normalize(iconName);
        if (key.isEmpty()) {
            key = normalize(categoryId);
        }

        switch (key) {
            case "electronics":
            case "devices":
            case "gadget":
                return R.drawable.ic_search;
            case "fashion":
            case "style":
                return R.drawable.ic_profile;
            case "home":
            case "home-kitchen":
            case "kitchen":
                return R.drawable.ic_home;
            case "beauty":
            case "sparkles":
                return R.drawable.ic_star;
            case "sports":
            case "fitness":
                return R.drawable.ic_notification;
            case "books":
            case "book":
                return R.drawable.ic_orders;
            case "toys":
                return R.drawable.ic_add;
            case "grocery":
            case "basket":
                return R.drawable.ic_cart;
            default:
                return R.drawable.ic_search;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }
}

