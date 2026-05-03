package com.buyon.ui.pricing;

import com.buyon.domain.model.CartItem;

import java.util.List;

/** Shared cart / checkout totals so UI, COD, and online payment stay in sync. */
public final class OrderPricing {

    public static final String PROMO_BUYON20 = "BUYON20";
    public static final double SHIPPING_FEE_USD = 5.0;
    public static final double BUYON20_FRACTION = 0.2;

    private OrderPricing() {}

    public static double subtotal(List<CartItem> items) {
        if (items == null) return 0;
        double s = 0;
        for (CartItem c : items) {
            s += c.getLineTotal();
        }
        return s;
    }

    public static double discount(String promoCode, double subtotal) {
        if (promoCode != null && PROMO_BUYON20.equalsIgnoreCase(promoCode.trim())) {
            return subtotal * BUYON20_FRACTION;
        }
        return 0;
    }

    public static double grandTotal(List<CartItem> items, String promoCode, double shippingFee) {
        double sub = subtotal(items);
        return sub - discount(promoCode, sub) + shippingFee;
    }
}
