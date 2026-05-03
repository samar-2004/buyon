package com.buyon.domain.model;

public enum PaymentStatus {
    PENDING,   // COD — to be collected on delivery
    PAID,      // Online payment confirmed
    FAILED,    // Online payment failed
    REFUNDED;  // Refunded after cancellation

    public static PaymentStatus fromString(String s) {
        if (s == null) return PENDING;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
