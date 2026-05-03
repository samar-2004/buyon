package com.buyon.ui.adapters;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.buyon.domain.model.CartItem;
import com.buyon.app.databinding.ItemCartBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface Listener {
        void onPlus(CartItem item);
        void onMinus(CartItem item);
    }

    private final List<CartItem> items = new ArrayList<>();
    private final boolean editMode;
    private final Listener listener;

    public CartAdapter(boolean editMode, Listener listener) {
        this.editMode = editMode;
        this.listener = listener;
    }

    public void submit(List<CartItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding b = ItemCartBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class VH extends RecyclerView.ViewHolder {
        private final ItemCartBinding b;

        VH(ItemCartBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(CartItem item) {
            b.name.setText(item.getName());
            b.subtitle.setText(
                    String.format(
                            Locale.US,
                            "$ %.2f each · Qty %d",
                            item.getUnitPrice(),
                            item.getQuantity()));
            b.price.setText(String.format(Locale.US, "Total $ %.2f", item.getLineTotal()));
            b.quantity.setText(String.valueOf(item.getQuantity()));

            String url = item.getImageUrl();
            if (url != null && !url.isEmpty()) {
                Glide.with(b.image.getContext()).load(url).centerCrop().into(b.image);
            }

            boolean show = editMode;
            b.qtyControls.setVisibility(show ? View.VISIBLE : View.GONE);
            b.btnRemove.setVisibility(show ? View.VISIBLE : View.GONE);

            if (editMode) {
                b.btnPlus.setOnClickListener(v -> {
                    animateBounce(v);
                    listener.onPlus(item);
                });
                b.btnMinus.setOnClickListener(v -> {
                    animateBounce(v);
                    listener.onMinus(item);
                });
                b.btnRemove.setOnClickListener(v -> {
                    // Animate the entire card row out, then call listener
                    animateRemove(b.getRoot(), () -> {
                        CartItem toRemove = new CartItem(
                                item.getProductId(), item.getName(),
                                item.getUnitPrice(), item.getImageUrl(), 1);
                        listener.onMinus(toRemove);
                    });
                });
            }
        }

        /** Bounce (overshoot scale) on +/- buttons */
        private void animateBounce(View view) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.75f, 1.15f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.75f, 1.15f, 1f);
            scaleX.setDuration(300);
            scaleY.setDuration(300);
            scaleX.setInterpolator(new OvershootInterpolator(3f));
            scaleY.setInterpolator(new OvershootInterpolator(3f));
            AnimatorSet set = new AnimatorSet();
            set.playTogether(scaleX, scaleY);
            set.start();
        }

        /** Shrink + fade the card, then invoke the callback */
        private void animateRemove(View card, Runnable onEnd) {
            card.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(onEnd)
                    .start();
        }
    }
}
