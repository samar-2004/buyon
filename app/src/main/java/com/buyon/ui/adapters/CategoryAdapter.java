package com.buyon.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.buyon.domain.model.Category;
import com.buyon.app.R;
import com.buyon.app.databinding.ItemCategoryBinding;
import com.buyon.ui.util.CategoryIconMapper;

import java.util.ArrayList;
import java.util.List;

public final class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface Listener {
        void onCategoryClick(String categoryId);
    }

    private final List<Category> items = new ArrayList<>();
    private final Listener listener;
    private String selectedId;

    public CategoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Category> categories) {
        items.clear();
        if (categories != null) {
            items.addAll(categories);
        }
        notifyDataSetChanged();
    }

    public void setSelected(String categoryId) {
        this.selectedId = categoryId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding b =
                ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ItemCategoryBinding b;

        VH(ItemCategoryBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Category c) {
            b.name.setText(c.getName());
            boolean sel = selectedId != null && !selectedId.isEmpty() && c.getId().equals(selectedId);

            // Toggle background between selected/unselected chip style
            b.getRoot().setBackgroundResource(
                    sel ? R.drawable.bg_category_chip_selected : R.drawable.bg_category_chip);
            b.name.setTextColor(b.getRoot().getContext().getColor(
                    sel ? R.color.white : R.color.buyon_text_secondary));
            b.icon.setImageResource(CategoryIconMapper.resolveIconRes(c.getId(), c.getIconName()));
            b.icon.setColorFilter(
                    b.getRoot().getContext().getColor(sel ? R.color.white : R.color.buyon_text_secondary));

            b.getRoot().setOnClickListener(v -> {
                String id = c.getId();
                if (id.equals(selectedId)) {
                    listener.onCategoryClick("");
                } else {
                    listener.onCategoryClick(id);
                }
            });
        }
    }
}
