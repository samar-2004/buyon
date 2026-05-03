package com.buyon.core.di;

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

public interface AppDependencies {
    AuthRepository        authRepository();
    ProductRepository     productRepository();
    CategoryRepository    categoryRepository();
    CartRepository        cartRepository();
    OrderRepository       orderRepository();
    UserProfileRepository userProfileRepository();
    PreferencesRepository preferencesRepository();
    AdminRepository       adminRepository();
    WishlistRepository    wishlistRepository();
    PaymentRepository     paymentRepository();
}
