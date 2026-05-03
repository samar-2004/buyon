package com.buyon.domain.model;

public enum PaymentMethod {
    COD("Cash on Delivery"),
    JAZZCASH("JazzCash"),
    EASYPAISA("EasyPaisa");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public static PaymentMethod fromString(String s) {
        if (s == null) return COD;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COD;
        }
    }

    /** Returns true for any non-cash gateway that requires remote processing. */
    public boolean isOnline() {
        return this == JAZZCASH || this == EASYPAISA;
    }
}
