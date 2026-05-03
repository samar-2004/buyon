package com.buyon.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.buyon.app.R;
import com.buyon.app.databinding.FragmentSignupBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.viewmodel.SignupViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public final class SignupFragment extends Fragment {

    private FragmentSignupBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSignupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        SignupViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps))
                        .get(SignupViewModel.class);

        addTextChangeListeners();

        if (binding.linkLogin != null) {
            binding.linkLogin.setOnClickListener(v ->
                    Navigation.findNavController(view).navigateUp());
        }

        binding.btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                String name     = getText(binding.inputName);
                String email    = getText(binding.inputEmail);
                String password = binding.inputPassword.getText() != null
                        ? binding.inputPassword.getText().toString() : "";
                vm.register(email, password, name);
            }
        });

        vm.getState().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            switch (res.getStatus()) {
                case LOADING:
                    dismissSignupFeedback();
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    navigateToMainDashboard();
                    break;
                case ERROR:
                    setLoadingState(false);
                    String banner = AppErrorHandler.resolveSignupAuthUi(
                            res.getError(),
                            binding.layoutName,
                            binding.layoutEmail,
                            binding.layoutPassword);
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

        String name = getText(binding.inputName);
        if (TextUtils.isEmpty(name)) {
            binding.layoutName.setError("Display name is required");
            valid = false;
        } else {
            binding.layoutName.setError(null);
        }

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

        String password = binding.inputPassword.getText() != null
                ? binding.inputPassword.getText().toString() : "";
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
        addClearErrorWatcher(binding.inputName,     binding.layoutName);
        addClearErrorWatcher(binding.inputEmail,    binding.layoutEmail);
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
        binding.progressRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!isLoading);
        if (isLoading) {
            binding.btnRegister.setText("");
        } else {
            binding.btnRegister.setText(R.string.sign_up);
        }
    }

    private void dismissSignupFeedback() {
        binding.layoutName.setError(null);
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);
        hideAuthBanner();
    }

    private void hideAuthBanner() {
        if (binding == null) return;
        binding.textAuthError.setVisibility(View.GONE);
        binding.textAuthError.setText("");
    }

    private void navigateToMainDashboard() {
        View v = getView();
        if (v == null) return;
        v.post(() -> {
            if (!isAdded()) return;
            NavHostFragment.findNavController(SignupFragment.this)
                    .navigate(R.id.action_signupFragment_to_homeFragment);
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
