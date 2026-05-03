package com.buyon.ui.adapters;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.bumptech.glide.Glide;
import com.buyon.app.R;
import com.buyon.domain.model.Product;
import com.buyon.app.databinding.ItemProductBinding;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ProductAdapter extends ListAdapter<Product, ProductAdapter.VH> {

    public interface Listener {
        void onProductClick(Product product);
        void onWishlistToggle(Product product, boolean currentlyWishlisted);
    }

    private final Listener listener;

    public ProductAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private final Set<String> wishlistedIds = new HashSet<>();

    public void setWishlistedIds(Set<String> ids) {
        wishlistedIds.clear();
        if (ids != null) {
            wishlistedIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public void setWishlisted(String productId, boolean wishlisted) {
        if (productId == null || productId.isEmpty()) {
            return;
        }
        if (wishlisted) {
            wishlistedIds.add(productId);
        } else {
            wishlistedIds.remove(productId);
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
        ItemProductBinding b =
                ItemProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    final class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        private final ItemProductBinding b;

        VH(ItemProductBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Product p) {
            b.name.setText(p.getName());
            b.price.setText(String.format(Locale.US, "$ %.0f", p.getPrice()));
            b.meta.setText(
                    String.format(
                            Locale.US,
                            "%.1f | Sold %d+",
                            p.getRating(),
                            Math.max(p.getSoldCount(), 0)));
            String url = p.getImageUrl();
            if (url != null && !url.isEmpty()) {
                Glide.with(b.image.getContext()).load(url).centerCrop().into(b.image);
            }

            boolean isWishlisted = wishlistedIds.contains(p.getId());
            b.btnWishlist.setImageResource(
                    isWishlisted ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            b.btnWishlist.setOnClickListener(
                    v -> {
                        animateWishlist(v);
                        listener.onWishlistToggle(p, wishlistedIds.contains(p.getId()));
                    });

            b.getRoot()
                    .setOnClickListener(v -> listener.onProductClick(p));
        }

        private void animateWishlist(View view) {
            ObjectAnimator sx = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.2f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.2f, 1f);
            sx.setDuration(220);
            sy.setDuration(220);
            sx.setInterpolator(new OvershootInterpolator(3f));
            sy.setInterpolator(new OvershootInterpolator(3f));
            AnimatorSet set = new AnimatorSet();
            set.playTogether(sx, sy);
            set.start();
        }
    }

    private int findPositionById(String productId) {
        for (int i = 0; i < getItemCount(); i++) {
            Product item = getItem(i);
            if (item != null && productId.equals(item.getId())) {
                return i;
            }
        }
        return -1;
    }
}
