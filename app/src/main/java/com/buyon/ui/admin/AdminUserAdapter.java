package com.buyon.ui.admin;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.buyon.app.databinding.ItemAdminUserBinding;
import com.buyon.domain.model.UserProfile;

public final class AdminUserAdapter extends ListAdapter<UserProfile, AdminUserAdapter.VH> {

    AdminUserAdapter() {
        super(DIFF);
    }

    private static final DiffUtil.ItemCallback<UserProfile> DIFF = new DiffUtil.ItemCallback<UserProfile>() {
        @Override
        public boolean areItemsTheSame(@NonNull UserProfile a, @NonNull UserProfile b) {
            return a.getUid().equals(b.getUid());
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserProfile a, @NonNull UserProfile b) {
            return a.getUid().equals(b.getUid())
                    && a.getDisplayName().equals(b.getDisplayName())
                    && a.getEmail().equals(b.getEmail());
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ItemAdminUserBinding binding;

        VH(ItemAdminUserBinding b) {
            super(b.getRoot());
            binding = b;
        }

        @SuppressLint("SetTextI18n")
        void bind(UserProfile user) {
            String name = user.getDisplayName();
            binding.name.setText(name != null && !name.isEmpty() ? name : "Unknown");
            binding.email.setText(user.getEmail());
            String roleLabel = user.isAdmin() ? "👑 Admin" : "User";
            binding.orderCount.setText(roleLabel + "  ·  ID: " + user.getUid().substring(0,
                    Math.min(8, user.getUid().length())) + "...");
        }
    }
}
