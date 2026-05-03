package com.buyon.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.buyon.app.databinding.ItemOnboardingPageBinding;

public final class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {

    private final String[] titles;
    private final String[] descriptions;

    public OnboardingAdapter(String[] titles, String[] descriptions) {
        this.titles = titles;
        this.descriptions = descriptions;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOnboardingPageBinding b =
                ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(titles[position], descriptions[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    static final class VH extends RecyclerView.ViewHolder {
        private final ItemOnboardingPageBinding b;

        VH(ItemOnboardingPageBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(String title, String desc) {
            b.title.setText(title);
            b.description.setText(desc);
        }
    }
}
