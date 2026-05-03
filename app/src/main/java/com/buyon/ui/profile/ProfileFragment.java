package com.buyon.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.buyon.app.R;
import com.buyon.app.databinding.FragmentProfileBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.domain.model.UserProfile;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.admin.AdminActivity;
import com.buyon.ui.viewmodel.ProfileViewModel;

public final class ProfileFragment extends Fragment {

    private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");
    private FragmentProfileBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyStatusBarPadding();
        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        ProfileViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps))
                        .get(ProfileViewModel.class);

        String email = deps.authRepository().currentUserEmail();
        if (email != null) binding.email.setText(email);

        // ── Admin card — hidden by default; shown after role is confirmed ──
        if (binding.cardAdmin != null) {
            binding.cardAdmin.setVisibility(View.GONE);
        }

        vm.getProfile().observe(getViewLifecycleOwner(), p -> {
            bindProfile(p);

            // Show admin card only when Firestore confirms role == "admin"
            if (p != null
                    && UserProfile.ROLE_ADMIN.equals(p.getRole())
                    && binding.cardAdmin != null) {
                binding.cardAdmin.setVisibility(View.VISIBLE);
                binding.cardAdmin.setOnClickListener(v -> {
                    Intent intent = new Intent(requireActivity(), AdminActivity.class);
                    startActivity(intent);
                });
            } else if (binding.cardAdmin != null) {
                binding.cardAdmin.setVisibility(View.GONE);
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            String name    = binding.inputName.getText()    != null ? binding.inputName.getText().toString().trim()    : "";
            String phone   = binding.inputPhone.getText()   != null ? binding.inputPhone.getText().toString().trim()   : "";
            String address = binding.inputAddress.getText() != null ? binding.inputAddress.getText().toString().trim() : "";

            if (binding.layoutPhone != null) binding.layoutPhone.setError(null);
            if (!phone.isEmpty()) {
                if (!DIGITS_ONLY.matcher(phone).matches()) {
                    if (binding.layoutPhone != null) binding.layoutPhone.setError(getString(R.string.profile_phone_digits_only));
                    return;
                }
                if (phone.length() < 10) {
                    if (binding.layoutPhone != null) binding.layoutPhone.setError(getString(R.string.profile_phone_too_short));
                    return;
                }
                if (phone.length() > 15) {
                    if (binding.layoutPhone != null) binding.layoutPhone.setError(getString(R.string.profile_phone_too_long));
                    return;
                }
            }
            vm.save(name, phone, address);
        });

        binding.btnSignOut.setOnClickListener(v -> {
            vm.signOut();
            Navigation.findNavController(view)
                    .navigate(R.id.action_profileFragment_to_loginFragment);
        });

        vm.getSaveState().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            boolean loading = res.getStatus() == Resource.Status.LOADING;
            setSaveButtonLoading(loading);
            if (res.getStatus() == Resource.Status.SUCCESS) {
                AppErrorHandler.showSuccess(view, "Profile saved \u2713");
            }
            if (res.getStatus() == Resource.Status.ERROR) {
                AppErrorHandler.showError(view,
                        AppErrorHandler.getFirebaseErrorMessage(res.getError()));
            }
        });
    }

    private void setSaveButtonLoading(boolean loading) {
        if (binding == null) return;
        binding.progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
        binding.btnSave.setText(loading ? "" : getString(R.string.save));
    }

    private void bindProfile(UserProfile p) {
        if (p == null || binding == null) return;
        if (p.getDisplayName() != null && !p.getDisplayName().isEmpty()) {
            binding.inputName.setText(p.getDisplayName());
            binding.title.setText(p.getDisplayName());
        }
        if (p.getPhone()                  != null && !p.getPhone().isEmpty())                  binding.inputPhone.setText(p.getPhone());
        if (p.getDefaultShippingAddress() != null && !p.getDefaultShippingAddress().isEmpty()) binding.inputAddress.setText(p.getDefaultShippingAddress());
        if (p.getEmail()                  != null && !p.getEmail().isEmpty())                  binding.email.setText(p.getEmail());
    }

    private void applyStatusBarPadding() {
        int basePaddingTop = binding.profileHeroHeader.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(binding.profileHeroHeader, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    basePaddingTop + bars.top,
                    v.getPaddingRight(),
                    v.getPaddingBottom());
            return insets;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
