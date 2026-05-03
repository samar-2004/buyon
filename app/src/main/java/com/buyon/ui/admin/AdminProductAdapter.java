package com.buyon.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.buyon.app.databinding.ItemAdminProductBinding;
import com.buyon.domain.model.Product;

import java.util.Locale;
import java.util.function.Consumer;

public final class AdminProductAdapter extends ListAdapter<Product, AdminProductAdapter.VH> {

    private final Consumer<Product> onEdit;
    private final Consumer<Product> onDelete;

    AdminProductAdapter(Consumer<Product> onEdit, Consumer<Product> onDelete) {
        super(DIFF);
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    private static final DiffUtil.ItemCallback<Product> DIFF = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product a, @NonNull Product b) {
            return a.getId().equals(b.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product a, @NonNull Product b) {
            return a.equals(b);
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemAdminProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    class VH extends RecyclerView.ViewHolder {
        private final ItemAdminProductBinding binding;

        VH(ItemAdminProductBinding b) {
            super(b.getRoot());
            binding = b;
        }

        void bind(Product product) {
            binding.name.setText(product.getName());
            binding.category.setText(product.getCategoryId());
            binding.price.setText(String.format(Locale.US, "$%.0f", product.getPrice()));

            String url = product.getImageUrl();
            if (url != null && !url.isEmpty()) {
                Glide.with(binding.image.getContext())
                        .load(url)
                        .centerCrop()
                        .into(binding.image);
            }

            binding.btnEdit.setOnClickListener(v -> onEdit.accept(product));
            binding.btnDelete.setOnClickListener(v -> onDelete.accept(product));
        }
    }
}
