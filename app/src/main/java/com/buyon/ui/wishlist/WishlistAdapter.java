package com.buyon.ui.wishlist;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.buyon.app.databinding.ItemWishlistBinding;
import com.buyon.domain.model.Product;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class WishlistAdapter extends ListAdapter<Product, WishlistAdapter.VH> {

    interface Listener {
        void onProductClick(Product product);
        void onAddToCart(Product product);
        void onRemove(Product product);
    }

    private final Listener listener;
    private final Set<String> addToCartLoadingIds = new HashSet<>();

    WishlistAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    void setAddToCartLoading(String productId, boolean loading) {
        if (productId == null || productId.isEmpty()) {
            return;
        }
        if (loading) {
            addToCartLoadingIds.add(productId);
        } else {
            addToCartLoadingIds.remove(productId);
        }
        int position = findPositionById(productId);
        if (position != -1) {
            notifyItemChanged(position);
        }
    }

    private static final DiffUtil.ItemCallback<Product> DIFF =
            new DiffUtil.ItemCallback<Product>() {
                @Override
                public boolean areItemsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWishlistBinding binding =
                ItemWishlistBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    final class VH extends RecyclerView.ViewHolder {
        private final ItemWishlistBinding binding;

        VH(ItemWishlistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            binding.name.setText(product.getName());
            binding.price.setText(String.format(Locale.US, "$ %.0f", product.getPrice()));

            String url = product.getImageUrl();
            if (url != null && !url.isEmpty()) {
                Glide.with(binding.image.getContext()).load(url).centerCrop().into(binding.image);
            }

            boolean loading = addToCartLoadingIds.contains(product.getId());
            binding.btnAddToCart.setEnabled(!loading);
            binding.btnAddToCart.setVisibility(loading ? android.view.View.GONE : android.view.View.VISIBLE);
            binding.progressAddToCart.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);

            binding.getRoot().setOnClickListener(v -> listener.onProductClick(product));
            binding.btnAddToCart.setOnClickListener(v -> listener.onAddToCart(product));
            binding.btnRemove.setOnClickListener(v -> listener.onRemove(product));
        }
    }

    private int findPositionById(String productId) {
        for (int i = 0; i < getItemCount(); i++) {
            Product p = getItem(i);
            if (p != null && productId.equals(p.getId())) {
                return i;
            }
        }
        return -1;
    }
}




