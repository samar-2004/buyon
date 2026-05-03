package com.buyon.app;

import android.app.Application;

import com.buyon.data.auth.FirebaseAuthRepository;
import com.buyon.data.firebase.FirestoreAdminRepository;
import com.buyon.data.firebase.FirestoreCartRepository;
import com.buyon.data.firebase.FirestoreCategoryRepository;
import com.buyon.data.firebase.FirestoreOrderRepository;
import com.buyon.data.firebase.FirestorePaymentRepository;
import com.buyon.data.firebase.FirestoreProductRepository;
import com.buyon.data.firebase.FirestoreUserProfileRepository;
import com.buyon.data.firebase.FirestoreWishlistRepository;
import com.buyon.data.prefs.SharedPreferencesRepository;
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
import com.buyon.core.di.AppDependencies;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public final class AppContainer implements AppDependencies {

    public final AuthRepository          authRepository;
    public final ProductRepository       productRepository;
    public final CategoryRepository      categoryRepository;
    public final CartRepository          cartRepository;
    public final OrderRepository         orderRepository;
    public final UserProfileRepository   userProfileRepository;
    public final PreferencesRepository   preferencesRepository;
    public final AdminRepository         adminRepository;
    public final WishlistRepository      wishlistRepository;
    public final PaymentRepository       paymentRepository;

    public AppContainer(Application app) {
        FirebaseAuth      firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore firestore    = FirebaseFirestore.getInstance();

        authRepository        = new FirebaseAuthRepository(firebaseAuth);
        productRepository     = new FirestoreProductRepository(firestore);
        categoryRepository    = new FirestoreCategoryRepository(firestore);
        cartRepository        = new FirestoreCartRepository(firestore);
        orderRepository       = new FirestoreOrderRepository(firestore);
        userProfileRepository = new FirestoreUserProfileRepository(firestore);
        preferencesRepository = new SharedPreferencesRepository(app);
        adminRepository       = new FirestoreAdminRepository(firestore);
        wishlistRepository    = new FirestoreWishlistRepository(firestore);
        paymentRepository     = new FirestorePaymentRepository(firestore);
    }

    @Override public AuthRepository         authRepository()        { return authRepository; }
    @Override public ProductRepository      productRepository()     { return productRepository; }
    @Override public CategoryRepository     categoryRepository()    { return categoryRepository; }
    @Override public CartRepository         cartRepository()        { return cartRepository; }
    @Override public OrderRepository        orderRepository()       { return orderRepository; }
    @Override public UserProfileRepository  userProfileRepository() { return userProfileRepository; }
    @Override public PreferencesRepository  preferencesRepository() { return preferencesRepository; }
    @Override public AdminRepository        adminRepository()       { return adminRepository; }
    @Override public WishlistRepository     wishlistRepository()    { return wishlistRepository; }
    @Override public PaymentRepository      paymentRepository()     { return paymentRepository; }
}
