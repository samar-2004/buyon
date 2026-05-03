package com.buyon.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.buyon.core.di.AppDependencies;
import com.buyon.domain.repository.AdminRepository;
import com.buyon.domain.repository.AuthRepository;
import com.buyon.domain.repository.CartRepository;
import com.buyon.domain.repository.CategoryRepository;
import com.buyon.domain.repository.OrderRepository;
import com.buyon.domain.repository.PaymentRepository;
import com.buyon.domain.repository.PreferencesRepository;
import com.buyon.domain.repository.ProductRepository;
import com.buyon.domain.repository.UserProfileRepository;
import com.buyon.domain.repository.WishlistRepository;

public final class BuyonApplication extends Application implements AppDependencies {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        container = new AppContainer(this);
    }

    @Override public AuthRepository         authRepository()        { return container.authRepository(); }
    @Override public ProductRepository      productRepository()     { return container.productRepository(); }
    @Override public CategoryRepository     categoryRepository()    { return container.categoryRepository(); }
    @Override public CartRepository         cartRepository()        { return container.cartRepository(); }
    @Override public OrderRepository        orderRepository()       { return container.orderRepository(); }
    @Override public UserProfileRepository  userProfileRepository() { return container.userProfileRepository(); }
    @Override public PreferencesRepository  preferencesRepository() { return container.preferencesRepository(); }
    @Override public AdminRepository        adminRepository()       { return container.adminRepository(); }
    @Override public WishlistRepository     wishlistRepository()    { return container.wishlistRepository(); }
    @Override public PaymentRepository      paymentRepository()     { return container.paymentRepository(); }
}
