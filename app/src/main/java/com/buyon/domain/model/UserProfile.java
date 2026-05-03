package com.buyon.domain.model;

import androidx.annotation.Nullable;

public final class UserProfile {

    public static final String ROLE_USER  = "user";
    public static final String ROLE_ADMIN = "admin";

    /** Fallback admin e-mail used ONLY for first-time setup / testing. */
    public static final String ADMIN_FALLBACK_EMAIL = "admin@buyon.com";

    private final String uid;
    private final String displayName;
    private final String email;
    private final String phone;
    private final String defaultShippingAddress;
    /** Either {@link #ROLE_USER} or {@link #ROLE_ADMIN}. Defaults to {@link #ROLE_USER}. */
    private final String role;

    public UserProfile(
            String uid,
            String displayName,
            String email,
            String phone,
            String defaultShippingAddress) {
        this(uid, displayName, email, phone, defaultShippingAddress, ROLE_USER);
    }

    public UserProfile(
            String uid,
            String displayName,
            String email,
            String phone,
            String defaultShippingAddress,
            String role) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.defaultShippingAddress = defaultShippingAddress;
        this.role = role != null ? role : ROLE_USER;
    }

    public String getUid()                  { return uid; }
    public String getDisplayName()          { return displayName; }
    public String getEmail()                { return email; }
    public String getPhone()                { return phone; }
    public String getDefaultShippingAddress() { return defaultShippingAddress; }
    public String getRole()                 { return role; }

    /** Returns true only when the role stored in Firestore is "admin". */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role)
                || ADMIN_FALLBACK_EMAIL.equalsIgnoreCase(email);
    }

    /**
     * Whether this signed-in account should use the admin app (matches {@link AdminActivity} gate).
     * Trims {@code email} so values typed in a form still match the fallback address.
     */
    public static boolean hasAdminAccess(@Nullable String role, @Nullable String email) {
        if (ROLE_ADMIN.equals(role)) {
            return true;
        }
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        return !trimmed.isEmpty() && ADMIN_FALLBACK_EMAIL.equalsIgnoreCase(trimmed);
    }
}
