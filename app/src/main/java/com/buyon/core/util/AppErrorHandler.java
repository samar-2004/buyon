package com.buyon.core.util;

import android.util.Patterns;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Centralized error handling, validation helpers, and Snackbar utilities.
 *
 * Usage:
 *   AppErrorHandler.getAuthErrorMessage(throwable)  → human-readable string
 *   AppErrorHandler.isValidEmail(email)             → boolean
 *   AppErrorHandler.showError(view, message)        → shows long Snackbar
 */
public final class AppErrorHandler {

    private AppErrorHandler() {}

    // ─────────────────────────────────────────────────────────────────────
    // Firebase Auth error translation
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Translates Firebase Auth exceptions to concise, user-friendly messages.
     */
    public static String getAuthErrorMessage(Throwable e) {
        if (e == null) return "Something went wrong. Please try again.";

        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "Invalid email address format.";
            }
            return "Incorrect email or password. Please try again.";
        }
        if (e instanceof FirebaseAuthInvalidUserException) {
            String code = ((FirebaseAuthInvalidUserException) e).getErrorCode();
            if ("ERROR_USER_DISABLED".equals(code)) {
                return "This account has been disabled.";
            }
            return "No account found with this email address.";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "An account already exists with this email address.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Password is too weak. Use at least 6 characters.";
        }
        if (e instanceof FirebaseNetworkException) {
            return "Network unavailable. Check your connection and try again.";
        }

        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("NETWORK_ERROR") || msg.contains("network")) {
                return "Network unavailable. Check your connection and try again.";
            }
            if (msg.contains("TOO_MANY_REQUESTS")) {
                return "Too many failed attempts. Please wait a moment and try again.";
            }
            if (msg.contains("USER_NOT_FOUND")) {
                return "No account found with this email address.";
            }
            if (msg.contains("WRONG_PASSWORD") || msg.contains("INVALID_PASSWORD")) {
                return "Incorrect password. Please try again.";
            }
            if (!msg.isEmpty()) return msg;
        }
        return "Something went wrong. Please try again.";
    }

    /**
     * Maps a login failure to field errors where possible. Clears both layouts first.
     *
     * @return message for an inline banner above the actions, or {@code null} when a field
     *         error is enough (or there is nothing to show).
     */
    @Nullable
    public static String resolveLoginAuthUi(
            @Nullable Throwable e,
            @Nullable TextInputLayout layoutEmail,
            @Nullable TextInputLayout layoutPassword) {
        if (layoutEmail != null) {
            layoutEmail.setError(null);
        }
        if (layoutPassword != null) {
            layoutPassword.setError(null);
        }
        if (e == null) {
            return null;
        }

        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code) && layoutEmail != null) {
                layoutEmail.setError("Check this email address");
                return null;
            }
            if (layoutPassword != null) {
                layoutPassword.setError("Incorrect email or password");
            }
            return null;
        }
        if (e instanceof FirebaseAuthInvalidUserException) {
            String code = ((FirebaseAuthInvalidUserException) e).getErrorCode();
            if (layoutEmail != null) {
                if ("ERROR_USER_DISABLED".equals(code)) {
                    layoutEmail.setError("This account is disabled");
                } else {
                    layoutEmail.setError("No account for this email");
                }
            }
            return null;
        }
        if (e instanceof FirebaseNetworkException) {
            return getAuthErrorMessage(e);
        }

        String msg = e.getMessage();
        if (msg != null && msg.contains("TOO_MANY_REQUESTS")) {
            return getAuthErrorMessage(e);
        }

        return getAuthErrorMessage(e);
    }

    /**
     * Maps a sign-up or post-registration profile error to fields or a banner message.
     */
    @Nullable
    public static String resolveSignupAuthUi(
            @Nullable Throwable e,
            @Nullable TextInputLayout layoutName,
            @Nullable TextInputLayout layoutEmail,
            @Nullable TextInputLayout layoutPassword) {
        if (layoutName != null) {
            layoutName.setError(null);
        }
        if (layoutEmail != null) {
            layoutEmail.setError(null);
        }
        if (layoutPassword != null) {
            layoutPassword.setError(null);
        }
        if (e == null) {
            return null;
        }

        if (e instanceof FirebaseAuthUserCollisionException) {
            if (layoutEmail != null) {
                layoutEmail.setError("This email is already registered");
            }
            return null;
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            if (layoutPassword != null) {
                layoutPassword.setError("Use a stronger password (at least 6 characters)");
            }
            return null;
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code) && layoutEmail != null) {
                layoutEmail.setError("Check this email address");
                return null;
            }
            return getAuthErrorMessage(e);
        }

        if (e instanceof FirebaseNetworkException) {
            return getAuthErrorMessage(e);
        }

        if (e instanceof IllegalStateException) {
            return "Could not finish sign-up. Try signing in, or use a different email.";
        }

        return getFirebaseErrorMessage(e);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Firestore / generic Firebase error translation
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Translates Firestore and generic Firebase errors to user-friendly messages.
     */
    public static String getFirebaseErrorMessage(Throwable e) {
        if (e == null) return "Something went wrong. Please try again.";
        if (e instanceof FirebaseNetworkException) {
            return "Network unavailable. Check your connection and try again.";
        }
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("UNAVAILABLE")) {
                return "Service temporarily unavailable. Please retry.";
            }
            if (msg.contains("PERMISSION_DENIED")) {
                return "Access denied. Please sign in again.";
            }
            if (msg.contains("NOT_FOUND")) {
                return "Requested data was not found.";
            }
            if (msg.contains("DEADLINE_EXCEEDED") || msg.contains("timeout")) {
                return "Request timed out. Check your connection and retry.";
            }
            if (!msg.isEmpty()) return msg;
        }
        return "Operation failed. Please try again.";
    }

    // ─────────────────────────────────────────────────────────────────────
    // Input validation
    // ─────────────────────────────────────────────────────────────────────

    /** Returns true if the email format is valid. */
    public static boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty()
                && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /** Returns true if password meets the minimum 6-character requirement. */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /** Returns true if the string is non-null and non-blank. */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Snackbar helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Shows a long-duration error Snackbar. Safe to call from any Fragment. */
    public static void showError(View anchorView, String message) {
        if (anchorView == null || message == null) return;
        Snackbar.make(anchorView, message, Snackbar.LENGTH_LONG).show();
    }

    /** Shows a short-duration success Snackbar. */
    public static void showSuccess(View anchorView, String message) {
        if (anchorView == null || message == null) return;
        Snackbar.make(anchorView, message, Snackbar.LENGTH_SHORT).show();
    }
}
