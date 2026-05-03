package com.buyon.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.buyon.app.R;
import com.buyon.app.databinding.ItemOrderBinding;
import com.buyon.domain.model.Order;
import com.buyon.ui.util.OrderStatusFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    private final List<Order> items = new ArrayList<>();

    public void submit(List<Order> orders) {
        items.clear();
        if (orders != null) {
            items.addAll(orders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding b = ItemOrderBinding.inflate(
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

    static final class VH extends RecyclerView.ViewHolder {
        private final ItemOrderBinding b;

        VH(ItemOrderBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Order o) {
            Context ctx = b.getRoot().getContext();

            // Trim long IDs for display
            String shortId = o.getId().length() > 12
                    ? "#" + o.getId().substring(0, 12).toUpperCase()
                    : "#" + o.getId().toUpperCase();
            b.orderId.setText(shortId);

            b.date.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US)
                    .format(new Date(o.getCreatedAtMillis())));
            // Show total + payment method
            b.total.setText(String.format(Locale.US, "$ %.2f  ·  %s",
                    o.getTotal(), o.paymentMethodEnum().getDisplayName()));

            b.status.setText(OrderStatusFormatter.label(ctx, o.getStatus()));

            switch (o.getStatus()) {
                case PENDING:
                    b.status.setBackgroundResource(R.drawable.bg_status_pending);
                    b.status.setTextColor(ctx.getColor(R.color.status_pending));
                    break;
                case PROCESSING:
                case SHIPPED:
                    b.status.setBackgroundResource(R.drawable.bg_status_shipped);
                    b.status.setTextColor(ctx.getColor(R.color.status_shipped));
                    break;
                case DELIVERED:
                    b.status.setBackgroundResource(R.drawable.bg_status_delivered);
                    b.status.setTextColor(ctx.getColor(R.color.status_delivered));
                    break;
                case CANCELLED:
                    b.status.setBackgroundResource(R.drawable.bg_status_pending);
                    b.status.setTextColor(ctx.getColor(R.color.status_cancelled));
                    break;
            }

            boolean isShippedOrBeyond =
                    o.getStatus() == Order.Status.PROCESSING
                            || o.getStatus() == Order.Status.SHIPPED
                            || o.getStatus() == Order.Status.DELIVERED;
            boolean isDelivered = o.getStatus() == Order.Status.DELIVERED;

            if (b.stepShippedDot != null) {
                b.stepShippedDot.setBackgroundResource(
                        isShippedOrBeyond ? R.drawable.bg_circle_primary : R.drawable.bg_circle_surface);
            }
            if (b.stepDeliveredDot != null) {
                b.stepDeliveredDot.setBackgroundResource(
                        isDelivered ? R.drawable.bg_circle_primary : R.drawable.bg_circle_surface);
            }
        }
    }
}
