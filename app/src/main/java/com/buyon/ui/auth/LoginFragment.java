package com.buyon.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.buyon.app.R;
import com.buyon.app.databinding.FragmentLoginBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.model.UserProfile;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.admin.AdminActivity;
import com.buyon.ui.viewmodel.LoginViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public final class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;
    /** When true, {@link #setLoadingState(boolean)} shows progress on the Google button. */
    private boolean googleSignInInFlight;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getData() == null) return;
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null && account.getIdToken() != null) {
                                googleSignInInFlight = true;
                                viewModel.signInWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            googleSignInInFlight = false;
                            if (e.getStatusCode() == CommonStatusCodes.CANCELED) {
                                return;
                            }
                            showAuthBanner(getString(R.string.error_google_sign_in));
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new BuyonViewModelFactory(deps))
                .get(LoginViewModel.class);

        // Clear field errors when user starts typing
        addTextChangeListeners();

        binding.btnLogin.setOnClickListener(v -> {
            if (validateForm()) {
                googleSignInInFlight = false;
                String email = getText(binding.inputEmail);
                String password = getText(binding.inputPassword);
                viewModel.signIn(email, password);
            }
        });

        binding.btnGoogle.setOnClickListener(v -> {
            String webClientId = getString(R.string.default_web_client_id);
            if ("REPLACE_WITH_WEB_CLIENT_ID".equals(webClientId)) {
                Snackbar.make(view,
                        "Add Firebase Web client ID to strings.xml (default_web_client_id)",
                        Snackbar.LENGTH_LONG).show();
                return;
            }
            GoogleSignInOptions gso =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(webClientId)
                            .requestEmail()
                            .build();
            GoogleSignInClient client = GoogleSignIn.getClient(requireContext(), gso);
            googleLauncher.launch(client.getSignInIntent());
        });

        binding.linkSignup.setOnClickListener(
                v -> Navigation.findNavController(view)
                        .navigate(R.id.action_loginFragment_to_signupFragment));

        binding.linkForgot.setOnClickListener(v -> showForgotPasswordDialog());

        viewModel.getResetPasswordState().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.getStatus() == Resource.Status.SUCCESS) {
                AppErrorHandler.showSuccess(requireView(), "Reset link sent — check your inbox.");
            } else if (res.getStatus() == Resource.Status.ERROR) {
                AppErrorHandler.showError(requireView(),
                        AppErrorHandler.getAuthErrorMessage(res.getError()));
            }
        });

        viewModel.getSignInState().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            switch (res.getStatus()) {
                case LOADING:
                    dismissSignInFeedback();
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    googleSignInInFlight = false;
                    resolvePostLoginDestination();
                    break;
                case ERROR:
                    setLoadingState(false);
                    googleSignInInFlight = false;
                    String banner = AppErrorHandler.resolveLoginAuthUi(
                            res.getError(), binding.layoutEmail, binding.layoutPassword);
                    showAuthBanner(banner);
                    break;
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────

    private boolean validateForm() {
        boolean valid = true;

        String email = getText(binding.inputEmail);
        if (TextUtils.isEmpty(email)) {
            binding.layoutEmail.setError("Email is required");
            valid = false;
        } else if (!AppErrorHandler.isValidEmail(email)) {
            binding.layoutEmail.setError("Enter a valid email address");
            valid = false;
        } else {
            binding.layoutEmail.setError(null);
        }

        String password = getText(binding.inputPassword);
        if (TextUtils.isEmpty(password)) {
            binding.layoutPassword.setError("Password is required");
            valid = false;
        } else if (!AppErrorHandler.isValidPassword(password)) {
            binding.layoutPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else {
            binding.layoutPassword.setError(null);
        }

        return valid;
    }

    private void addTextChangeListeners() {
        addClearErrorWatcher(binding.inputEmail, binding.layoutEmail);
        addClearErrorWatcher(binding.inputPassword, binding.layoutPassword);
    }

    private void addClearErrorWatcher(TextInputEditText field, TextInputLayout layout) {
        field.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
                hideAuthBanner();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Loading state
    // ─────────────────────────────────────────────────────────────────────

    private void setLoadingState(boolean isLoading) {
        binding.progressLogin.setVisibility(View.GONE);
        binding.progressGoogle.setVisibility(View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnGoogle.setEnabled(!isLoading);
        if (!isLoading) {
            binding.btnLogin.setText(R.string.sign_in);
            binding.btnGoogle.setText(R.string.continue_google);
            return;
        }
        if (googleSignInInFlight) {
            binding.progressGoogle.setVisibility(View.VISIBLE);
            binding.btnGoogle.setText("");
        } else {
            binding.progressLogin.setVisibility(View.VISIBLE);
            binding.btnLogin.setText("");
        }
    }

    private void dismissSignInFeedback() {
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);
        hideAuthBanner();
    }

    private void hideAuthBanner() {
        if (binding == null) return;
        binding.textAuthError.setVisibility(View.GONE);
        binding.textAuthError.setText("");
    }

    /** After Firebase auth succeeds, route admins to {@link AdminActivity}, others to home. */
    private void resolvePostLoginDestination() {
        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        String uid = deps.authRepository().currentUserId();
        if (uid == null) {
            setLoadingState(false);
            navigateToMainDashboard();
            return;
        }

        // Timeout guard: if Firestore hangs for 10 s, fall back to email-based routing.
        final boolean[] settled = {false};
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (settled[0] || !isAdded()) return;
            settled[0] = true;
            setLoadingState(false);
            routeAfterRoleResolved(deps, null);
        };
        timeoutHandler.postDelayed(timeoutRunnable, 10_000);

        deps.userProfileRepository().fetchRole(uid, new DomainCallback<String>() {
            @Override
            public void onSuccess(String role) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (settled[0] || !isAdded()) return;
                settled[0] = true;
                setLoadingState(false);
                routeAfterRoleResolved(deps, role);
            }

            @Override
            public void onError(Throwable error) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (settled[0] || !isAdded()) return;
                settled[0] = true;
                setLoadingState(false);
                // Firestore unreadable (rules, offline, etc.) — honor fallback admin email.
                routeAfterRoleResolved(deps, null);
            }
        });
    }

    /**
     * Uses Firebase email when present; otherwise the email field still on screen (covers brief
     * null {@link com.google.firebase.auth.FirebaseUser#getEmail()} right after sign-in).
     */
    private void routeAfterRoleResolved(AppDependencies deps, @Nullable String firestoreRole) {
        String email = deps.authRepository().currentUserEmail();
        if (email == null || email.trim().isEmpty()) {
            email = getText(binding.inputEmail);
        }
        if (UserProfile.hasAdminAccess(firestoreRole, email)) {
            launchAdminDashboard();
        } else {
            navigateToMainDashboard();
        }
    }

    /** Clears the auth back stack and opens the consumer home tab. Posted to avoid nav timing issues. */
    private void navigateToMainDashboard() {
        View v = getView();
        if (v == null) return;
        v.post(() -> {
            if (!isAdded()) return;
            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(R.id.action_loginFragment_to_homeFragment);
        });
    }

    private void launchAdminDashboard() {
        View v = getView();
        if (v == null) return;
        v.post(() -> {
            if (!isAdded()) return;
            startActivity(new Intent(requireActivity(), AdminActivity.class));
            requireActivity().finish();
        });
    }

    private void showAuthBanner(@Nullable String message) {
        if (binding == null) return;
        if (message != null && !message.trim().isEmpty()) {
            binding.textAuthError.setText(message.trim());
            binding.textAuthError.setVisibility(View.VISIBLE);
        } else {
            hideAuthBanner();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Forgot password dialog
    // ─────────────────────────────────────────────────────────────────────

    private void showForgotPasswordDialog() {
        android.view.View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_forgot_password, null, false);
        TextInputLayout layoutEmail = dialogView.findViewById(R.id.layout_reset_email);
        TextInputEditText inputEmail = dialogView.findViewById(R.id.input_reset_email);

        String currentEmail = getText(binding.inputEmail);
        if (AppErrorHandler.isValidEmail(currentEmail)) {
            inputEmail.setText(currentEmail);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset Password")
                .setView(dialogView)
                .setPositiveButton("Send Link", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.buyon_primary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.buyon_text_secondary));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = inputEmail.getText() != null
                    ? inputEmail.getText().toString().trim() : "";
            if (!AppErrorHandler.isValidEmail(email)) {
                layoutEmail.setError("Enter a valid email address");
                return;
            }
            layoutEmail.setError(null);
            viewModel.resetPassword(email);
            dialog.dismiss();
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
