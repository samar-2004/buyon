package com.buyon.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.buyon.core.di.AppDependencies;
import com.buyon.ui.viewmodel.AdminViewModel;
import com.buyon.ui.viewmodel.CartViewModel;
import com.buyon.ui.viewmodel.CheckoutViewModel;
import com.buyon.ui.viewmodel.HomeViewModel;
import com.buyon.ui.viewmodel.LoginViewModel;
import com.buyon.ui.viewmodel.OnboardingViewModel;
import com.buyon.ui.viewmodel.OrdersViewModel;
import com.buyon.ui.viewmodel.PaymentViewModel;
import com.buyon.ui.viewmodel.ProductDetailViewModel;
import com.buyon.ui.viewmodel.ProfileViewModel;
import com.buyon.ui.viewmodel.SearchViewModel;
import com.buyon.ui.viewmodel.SignupViewModel;
import com.buyon.ui.viewmodel.SplashViewModel;
import com.buyon.ui.viewmodel.WishlistViewModel;

public final class BuyonViewModelFactory implements ViewModelProvider.Factory {

    private final AppDependencies deps;
    @Nullable private final String productId;

    public BuyonViewModelFactory(AppDependencies deps) {
        this.deps      = deps;
        this.productId = null;
    }

    public BuyonViewModelFactory(AppDependencies deps, @NonNull String productId) {
        this.deps      = deps;
        this.productId = productId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass == SplashViewModel.class)
            return (T) new SplashViewModel(
                    deps.authRepository(),
                    deps.preferencesRepository(),
                    deps.userProfileRepository());
        if (modelClass == OnboardingViewModel.class)
            return (T) new OnboardingViewModel(deps.preferencesRepository());
        if (modelClass == LoginViewModel.class)
            return (T) new LoginViewModel(deps.authRepository());
        if (modelClass == SignupViewModel.class)
            return (T) new SignupViewModel(deps.authRepository(), deps.userProfileRepository());
        if (modelClass == HomeViewModel.class)
            return (T) new HomeViewModel(deps.productRepository(), deps.categoryRepository());
        if (modelClass == SearchViewModel.class)
            return (T) new SearchViewModel(deps.productRepository(), deps.categoryRepository());
        if (modelClass == ProductDetailViewModel.class) {
            if (productId == null) throw new IllegalStateException("productId required");
            return (T) new ProductDetailViewModel(
                    deps.productRepository(), deps.cartRepository(),
                    deps.authRepository(), productId);
        }
        if (modelClass == CartViewModel.class)
            return (T) new CartViewModel(deps.cartRepository(), deps.authRepository());
        if (modelClass == CheckoutViewModel.class)
            return (T) new CheckoutViewModel(
                    deps.cartRepository(), deps.orderRepository(),
                    deps.userProfileRepository(), deps.authRepository());
        if (modelClass == OrdersViewModel.class)
            return (T) new OrdersViewModel(deps.orderRepository(), deps.authRepository());
        if (modelClass == ProfileViewModel.class)
            return (T) new ProfileViewModel(deps.userProfileRepository(), deps.authRepository());
        if (modelClass == AdminViewModel.class)
            return (T) new AdminViewModel(deps.adminRepository());
        if (modelClass == WishlistViewModel.class)
            return (T) new WishlistViewModel(
                    deps.wishlistRepository(), deps.productRepository(), deps.authRepository());
        if (modelClass == PaymentViewModel.class)
            return (T) new PaymentViewModel(
                    deps.paymentRepository(), deps.orderRepository(), deps.cartRepository());

        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}
