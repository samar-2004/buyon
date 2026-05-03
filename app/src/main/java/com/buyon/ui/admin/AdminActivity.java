package com.buyon.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.buyon.app.BaseActivity;
import com.buyon.app.R;
import com.buyon.app.databinding.ActivityAdminBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.model.UserProfile;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.viewmodel.AdminViewModel;
import com.google.android.material.snackbar.Snackbar;

public final class AdminActivity extends BaseActivity {

    private ActivityAdminBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowInsets();

        // ── Server-side role gate ────────────────────────────────────────
        // Re-read the role from Firestore every time AdminActivity opens —
        // prevents a logged-in non-admin from reaching this screen through
        // UI manipulation (e.g., cached LiveData).
        AppDependencies deps = (AppDependencies) getApplication();
        String uid = deps.authRepository().currentUserId();

        if (uid == null) {
            denyAccess();
            return;
        }

        deps.userProfileRepository().fetchRole(uid, new DomainCallback<String>() {
                    @Override
                    public void onSuccess(String role) {
                        // Must match Firestore rules for admin-only collections.
                        if (UserProfile.ROLE_ADMIN.equals(role)) {
                            setupNavigation();
                            observeOrderStatusFeedback();
                        } else {
                            denyAccess();
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        denyAccess();
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Window insets — applied once here; admin fragments must NOT re-apply.
    // ─────────────────────────────────────────────────────────────────────

    private void setupWindowInsets() {
        // NavHostFragment: do NOT consume the status bar inset here — each admin
        // fragment's root handles it via android:fitsSystemWindows="true" so its
        // header background extends seamlessly behind the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.adminNavHost, (v, insets) -> insets);

        // BottomNav: lift items above the gesture / button bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.adminBottomNav, (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    sysBars.bottom);
            return insets;
        });
    }

    private void setupNavigation() {
        NavHostFragment navHost = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.admin_nav_host);
        if (navHost == null) return;

        NavController navController = navHost.getNavController();
        NavigationUI.setupWithNavController(binding.adminBottomNav, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            binding.adminBottomNav.setVisibility(
                    id == R.id.adminAddProductFragment ? View.GONE : View.VISIBLE);
        });
    }

    private void denyAccess() {
        Toast.makeText(this, "Admin access denied.", Toast.LENGTH_SHORT).show();
        finish();
    }

    /** One place for snackbars / errors when an order status write completes. */
    private void observeOrderStatusFeedback() {
        AppDependencies deps = (AppDependencies) getApplication();
        AdminViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(AdminViewModel.class);
        vm.getUpdateOrderState()
                .observe(
                        this,
                        res -> {
                            if (res == null) return;
                            if (res.getStatus() == Resource.Status.LOADING) return;
                            if (res.getStatus() == Resource.Status.SUCCESS) {
                                Snackbar.make(
                                                binding.getRoot(),
                                                getString(R.string.admin_order_status_saved),
                                                Snackbar.LENGTH_SHORT)
                                        .show();
                                vm.clearUpdateOrderState();
                            } else if (res.getStatus() == Resource.Status.ERROR) {
                                AppErrorHandler.showError(
                                        binding.getRoot(),
                                        AppErrorHandler.getFirebaseErrorMessage(res.getError()));
                                vm.clearUpdateOrderState();
                            }
                        });
    }

}
