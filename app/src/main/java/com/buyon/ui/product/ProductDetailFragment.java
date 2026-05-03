package com.buyon.ui.product;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.domain.model.Product;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.app.R;
import com.buyon.app.databinding.FragmentProductDetailBinding;
import com.buyon.ui.viewmodel.ProductDetailViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public final class ProductDetailFragment extends Fragment {

    private FragmentProductDetailBinding binding;
    private int quantity = 1;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        String productId = args != null ? args.getString("productId") : null;
        if (productId == null) {
            Navigation.findNavController(view).navigateUp();
            return;
        }
        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        ProductDetailViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps, productId))
                        .get(ProductDetailViewModel.class);

        if (binding.btnBack != null) {
            binding.btnBack.setOnClickListener(v ->
                    Navigation.findNavController(view).navigateUp());
        }

        if (binding.btnMinus != null) {
            binding.btnMinus.setOnClickListener(v -> {
                if (quantity > 1) {
                    quantity--;
                    updateQuantityDisplay();
                    animateBounce(v);
                }
            });
        }
        if (binding.btnPlus != null) {
            binding.btnPlus.setOnClickListener(v -> {
                quantity++;
                updateQuantityDisplay();
                animateBounce(v);
            });
        }

        vm.getProduct().observe(getViewLifecycleOwner(), this::renderProduct);

        vm.getCartAction().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.getStatus() == Resource.Status.LOADING) {
                binding.progressAddCart.setVisibility(View.VISIBLE);
                binding.btnAddCart.setEnabled(false);
                binding.btnAddCart.setText("");
            } else {
                binding.progressAddCart.setVisibility(View.GONE);
                binding.btnAddCart.setEnabled(true);
                binding.btnAddCart.setText(R.string.add_to_cart);
            }
            if (res.getStatus() == Resource.Status.SUCCESS) {
                Snackbar.make(view, "Added to cart ✓", Snackbar.LENGTH_SHORT).show();
            }
            if (res.getStatus() == Resource.Status.ERROR && res.getError() != null) {
                Snackbar.make(view, res.getError().getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });

        vm.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show();
            }
        });

        binding.btnAddCart.setOnClickListener(v -> {
            animateButtonPress(v);
            animateFlyToCart(binding.image);
            vm.addToCart(quantity);
        });
    }

    private void updateQuantityDisplay() {
        if (binding.inputQty != null) {
            binding.inputQty.setText(String.valueOf(quantity));
        }
        // Update total price shown in sticky bar
        if (binding.priceTotal != null && binding.priceTotal.getTag() instanceof Double) {
            double price = (Double) binding.priceTotal.getTag();
            binding.priceTotal.setText(String.format(Locale.US, "$ %.0f", price * quantity));
        }
    }

    /** Fly-to-cart animation: a fading clone of the product image arcs to the bottom-nav cart icon. */
    private void animateFlyToCart(View productImage) {
        if (getActivity() == null) return;

        // Get product image position on screen
        int[] imgPos = new int[2];
        productImage.getLocationOnScreen(imgPos);

        // Find the root window overlay to draw the flying clone above everything
        ViewGroup decorView = (ViewGroup) getActivity().getWindow().getDecorView();

        // Create a clone ImageView
        ImageView clone = new ImageView(requireContext());
        clone.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(((ImageView) productImage).getDrawable()).into(clone);
        clone.setBackground(productImage.getBackground());

        int size = (int) (72 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        lp.leftMargin = imgPos[0] + (productImage.getWidth() - size) / 2;
        lp.topMargin = imgPos[1] - getStatusBarHeight() + (productImage.getHeight() - size) / 2;
        decorView.addView(clone, lp);

        // Target: bottom-nav cart icon approximate position
        int[] navPos = new int[2];
        View bottomNav = getActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.getLocationOnScreen(navPos);
        }
        // Cart is the 3rd item in a 5-item bottom nav → ~60% from left
        float targetX = navPos[0] + decorView.getWidth() * 0.5f - size / 2f;
        float targetY = navPos[1] - getStatusBarHeight();

        float startX = lp.leftMargin;
        float startY = lp.topMargin;

        ObjectAnimator tx = ObjectAnimator.ofFloat(clone, "translationX", 0f, targetX - startX);
        ObjectAnimator ty = ObjectAnimator.ofFloat(clone, "translationY", 0f, targetY - startY);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(clone, "alpha", 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(clone, "scaleX", 1f, 0.3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(clone, "scaleY", 1f, 0.3f);

        tx.setInterpolator(new AccelerateInterpolator(1.5f));
        ty.setInterpolator(new AccelerateInterpolator(1.5f));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(tx, ty, alpha, scaleX, scaleY);
        set.setDuration(520);
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                decorView.removeView(clone);
            }
        });
        set.start();
    }

    private int getStatusBarHeight() {
        Rect rect = new Rect();
        if (getActivity() != null) {
            getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        }
        return rect.top;
    }

    private void animateBounce(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.15f, 1f);
        scaleX.setDuration(280);
        scaleY.setDuration(280);
        scaleX.setInterpolator(new OvershootInterpolator(4f));
        scaleY.setInterpolator(new OvershootInterpolator(4f));
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.start();
    }

    private void animateButtonPress(View view) {
        view.animate()
                .scaleX(0.94f).scaleY(0.94f).setDuration(80)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                .start();
    }

    private void renderProduct(Product p) {
        if (p == null || binding == null) return;
        binding.name.setText(p.getName());
        binding.price.setText(String.format(Locale.US, "$ %.0f", p.getPrice()));
        binding.meta.setText(String.format(Locale.US, "%.1f · %d+ sold",
                p.getRating(), p.getSoldCount()));
        binding.description.setText(p.getDescription());

        if (binding.textLocation != null) {
            String loc = p.getLocationLabel();
            if (loc != null && !loc.isEmpty()) {
                binding.textLocation.setText(loc);
                binding.textLocation.setVisibility(android.view.View.VISIBLE);
                if (binding.iconLocation != null) binding.iconLocation.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.textLocation.setVisibility(android.view.View.GONE);
                if (binding.iconLocation != null) binding.iconLocation.setVisibility(android.view.View.GONE);
            }
        }

        if (binding.priceTotal != null) {
            binding.priceTotal.setVisibility(View.VISIBLE);
            binding.priceTotal.setText(String.format(Locale.US, "$ %.0f", p.getPrice() * quantity));
            // Store unit price as tag so updateQuantityDisplay can reuse it
            binding.priceTotal.setTag(p.getPrice());
        }

        String url = p.getImageUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(binding.image.getContext())
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.bg_product_image)
                    .into(binding.image);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
