package com.buyon.ui.admin;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.buyon.app.R;
import com.buyon.app.databinding.DialogAdminCancelOrderBinding;
import com.buyon.app.databinding.DialogAdminOrderDetailsBinding;
import com.buyon.app.databinding.ItemAdminOrderBinding;
import com.buyon.domain.model.Order;
import com.buyon.domain.model.OrderLine;
import com.buyon.ui.util.OrderStatusFormatter;
import com.buyon.ui.viewmodel.AdminViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AdminOrderAdapter extends ListAdapter<Order, AdminOrderAdapter.VH> {

    private final AdminViewModel vm;

    AdminOrderAdapter(AdminViewModel vm) {
        super(DIFF);
        this.vm = vm;
    }

    private static final DiffUtil.ItemCallback<Order> DIFF =
            new DiffUtil.ItemCallback<Order>() {
                @Override
                public boolean areItemsTheSame(@NonNull Order a, @NonNull Order b) {
                    return a.getId().equals(b.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Order a, @NonNull Order b) {
                    return a.equals(b);
                }
            };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(
                ItemAdminOrderBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    final class VH extends RecyclerView.ViewHolder {
        private final ItemAdminOrderBinding binding;

        VH(ItemAdminOrderBinding b) {
            super(b.getRoot());
            binding = b;
        }

        void bind(Order order) {
            Context ctx = binding.getRoot().getContext();
            String shortId =
                    "#"
                            + order.getId()
                                    .substring(0, Math.min(10, order.getId().length()))
                                    .toUpperCase(Locale.US);
            binding.orderId.setText(shortId);

            String name = safeTrim(order.getCustomerDisplayName());
            if (name.isEmpty()) {
                name = ctx.getString(R.string.admin_customer_fallback_name);
            }
            binding.customerName.setText(name);
            binding.customerContact.setText(formatCustomerContact(ctx, order));

            binding.total.setText(money(order.getTotal()));
            binding.paymentMethod.setText(order.paymentMethodEnum().getDisplayName());
            binding.itemCount.setText(
                    ctx.getString(R.string.admin_items_count_format, countItems(order.getLines())));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US);
            binding.date.setText(sdf.format(new Date(order.getCreatedAtMillis())));

            boolean canUpdate = hasStatusActions(order.getStatus());
            binding.status.setText(
                    canUpdate
                            ? OrderStatusFormatter.label(ctx, order.getStatus()) + " v"
                            : OrderStatusFormatter.label(ctx, order.getStatus()));
            applyStatusStyle(order.getStatus());
            binding.status.setEnabled(canUpdate);
            binding.status.setClickable(canUpdate);
            binding.status.setAlpha(canUpdate ? 1f : 0.8f);

            binding.status.setOnClickListener(
                    v -> {
                        if (!hasStatusActions(order.getStatus())) {
                            showStatusLockedMessage(ctx, order.getStatus());
                            return;
                        }
                        showStatusPicker(ctx, order);
                    });

            binding.getRoot().setOnClickListener(v -> showOrderDetail(ctx, order));
        }

        private String formatCustomerContact(Context ctx, Order order) {
            String email = safeTrim(order.getCustomerEmail());
            String phone = safeTrim(order.getCustomerPhone());
            if (email.isEmpty() && phone.isEmpty()) {
                return ctx.getString(R.string.admin_no_contact_on_order);
            }
            if (email.isEmpty()) return phone;
            if (phone.isEmpty()) return email;
            return email + " | " + phone;
        }

        private void applyStatusStyle(Order.Status status) {
            Context ctx = binding.status.getContext();
            switch (status) {
                case PENDING:
                    binding.status.setBackgroundResource(R.drawable.bg_status_pending);
                    binding.status.setTextColor(ctx.getColor(R.color.status_pending));
                    break;
                case SHIPPED:
                case PROCESSING:
                    binding.status.setBackgroundResource(R.drawable.bg_status_shipped);
                    binding.status.setTextColor(ctx.getColor(R.color.status_shipped));
                    break;
                case DELIVERED:
                    binding.status.setBackgroundResource(R.drawable.bg_status_delivered);
                    binding.status.setTextColor(ctx.getColor(R.color.status_delivered));
                    break;
                case CANCELLED:
                    binding.status.setBackgroundResource(R.drawable.bg_status_pending);
                    binding.status.setTextColor(ctx.getColor(R.color.status_cancelled));
                    break;
            }
        }

        private void showStatusPicker(Context context, Order order) {
            final List<Order.Status> actions = allowedTransitions(order.getStatus());
            if (actions.isEmpty()) {
                showStatusLockedMessage(context, order.getStatus());
                return;
            }
            String[] labels = new String[actions.size()];
            for (int i = 0; i < actions.size(); i++) {
                labels[i] = OrderStatusFormatter.label(context, actions.get(i));
            }

            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.admin_update_order_status)
                    .setItems(
                            labels,
                            (dialog, which) -> {
                                Order.Status target = actions.get(which);
                                if (target == Order.Status.CANCELLED) {
                                    showCancellationReasonDialog(context, order);
                                    return;
                                }
                                vm.updateOrderStatus(order.getId(), target.name());
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        private void showOrderDetail(Context context, Order order) {
            DialogAdminOrderDetailsBinding detailBinding =
                    DialogAdminOrderDetailsBinding.inflate(LayoutInflater.from(context));
            bindDetail(detailBinding, context, order);

            MaterialAlertDialogBuilder builder =
                    new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.admin_order_details_title)
                    .setView(detailBinding.getRoot())
                    .setNegativeButton(android.R.string.ok, null);

            if (hasStatusActions(order.getStatus())) {
                builder.setPositiveButton(
                        R.string.admin_action_update_status,
                        (dialog, which) -> showStatusPicker(context, order));
            }
            if (canCancel(order.getStatus())) {
                builder.setNeutralButton(
                        R.string.admin_action_cancel_order,
                        (dialog, which) -> showCancellationReasonDialog(context, order));
            }
            builder.show();
        }

        private void bindDetail(DialogAdminOrderDetailsBinding b, Context context, Order order) {
            b.detailOrderId.setText(context.getString(R.string.admin_detail_order_short, order.getId()));
            b.detailPlacedAt.setText(
                    context.getString(
                            R.string.admin_detail_placed_at,
                            new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US)
                                    .format(new Date(order.getCreatedAtMillis()))));
            b.detailStatus.setText(OrderStatusFormatter.label(context, order.getStatus()));
            applyStatusStyleForView(b.detailStatus, order.getStatus());

            String customerName = safeTrim(order.getCustomerDisplayName());
            if (customerName.isEmpty()) {
                customerName = context.getString(R.string.admin_customer_fallback_name);
            }
            b.detailCustomerName.setText(customerName);
            b.detailCustomerContact.setText(formatCustomerContact(context, order));
            b.detailAccountId.setText(
                    context.getString(R.string.admin_detail_account_id, safeText(order.getUserId())));
            b.detailAddress.setText(safeText(order.getShippingAddress()));
            b.detailItems.setText(buildItemsText(order.getLines()));
            b.detailPricing.setText(
                    context.getString(
                                    R.string.admin_detail_pricing_block,
                                    order.getSubtotal(),
                                    order.getShippingFee(),
                                    order.getTotal())
                            + promoLine(order));
            b.detailPayment.setText(buildPaymentText(context, order));

            String cancellationReason = safeTrim(order.getCancellationReason());
            if (!cancellationReason.isEmpty()) {
                b.layoutCancelReason.setVisibility(android.view.View.VISIBLE);
                b.detailCancelReason.setText(cancellationReason);
            } else {
                b.layoutCancelReason.setVisibility(android.view.View.GONE);
            }
        }

        private void showCancellationReasonDialog(Context context, Order order) {
            if (order.getStatus() == Order.Status.DELIVERED) {
                Toast.makeText(context, R.string.admin_error_delivered_cannot_cancel, Toast.LENGTH_LONG).show();
                return;
            }
            if (order.getStatus() == Order.Status.CANCELLED) {
                Toast.makeText(context, R.string.admin_error_already_cancelled, Toast.LENGTH_LONG).show();
                return;
            }

            DialogAdminCancelOrderBinding b =
                    DialogAdminCancelOrderBinding.inflate(LayoutInflater.from(context));

            androidx.appcompat.app.AlertDialog dialog =
                    new MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.admin_cancel_order_title)
                            .setView(b.getRoot())
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.admin_action_confirm_cancel, null)
                            .show();

            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(
                            v -> {
                                String reason = safeTrim(String.valueOf(b.inputCancelReason.getText()));
                                if (reason.length() < 5) {
                                    b.layoutCancelReason.setError(
                                            context.getString(R.string.admin_cancel_reason_required));
                                    return;
                                }
                                b.layoutCancelReason.setError(null);
                                vm.updateOrderStatus(
                                        order.getId(),
                                        Order.Status.CANCELLED.name(),
                                        reason);
                                dialog.dismiss();
                            });
        }

        private void showStatusLockedMessage(Context context, Order.Status status) {
            int msg = status == Order.Status.DELIVERED
                    ? R.string.admin_error_delivered_locked
                    : R.string.admin_error_cancelled_locked;
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }

    private static boolean hasStatusActions(Order.Status status) {
        return !allowedTransitions(status).isEmpty();
    }

    private static boolean canCancel(Order.Status status) {
        return status == Order.Status.PENDING
                || status == Order.Status.PROCESSING
                || status == Order.Status.SHIPPED;
    }

    private static List<Order.Status> allowedTransitions(Order.Status status) {
        List<Order.Status> out = new ArrayList<>();
        switch (status) {
            case PENDING:
                out.add(Order.Status.PROCESSING);
                out.add(Order.Status.CANCELLED);
                break;
            case PROCESSING:
                out.add(Order.Status.SHIPPED);
                out.add(Order.Status.CANCELLED);
                break;
            case SHIPPED:
                out.add(Order.Status.DELIVERED);
                out.add(Order.Status.CANCELLED);
                break;
            case DELIVERED:
            case CANCELLED:
                break;
        }
        return out;
    }

    private static String promoLine(Order order) {
        if (TextUtils.isEmpty(order.getPromoCode())) return "";
        return "\n" + String.format(Locale.US, "Promo: %s", order.getPromoCode());
    }

    private static String buildPaymentText(Context context, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getString(R.string.admin_detail_payment));
        sb.append("\n");
        sb.append(order.paymentMethodEnum().getDisplayName());
        sb.append(" | ");
        sb.append(order.paymentStatusEnum().name());
        if (!TextUtils.isEmpty(order.getPaymentReference())) {
            sb.append("\n");
            sb.append(context.getString(R.string.admin_detail_payment_ref, order.getPaymentReference()));
        }
        return sb.toString();
    }

    private static String buildItemsText(List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (OrderLine line : lines) {
            sb.append("- ")
                    .append(line.getName())
                    .append(" x")
                    .append(line.getQuantity())
                    .append(" @ ")
                    .append(String.format(Locale.US, "$%.2f", line.getUnitPrice()))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private static int countItems(List<OrderLine> lines) {
        int total = 0;
        if (lines == null) return total;
        for (OrderLine line : lines) {
            total += line.getQuantity();
        }
        return total;
    }

    private static String money(double amount) {
        return String.format(Locale.US, "$%.2f", amount);
    }

    private static String safeText(String text) {
        String value = safeTrim(text);
        return value.isEmpty() ? "-" : value;
    }

    private static String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private void applyStatusStyleForView(android.widget.TextView view, Order.Status status) {
        Context ctx = view.getContext();
        switch (status) {
            case PENDING:
                view.setBackgroundResource(R.drawable.bg_status_pending);
                view.setTextColor(ctx.getColor(R.color.status_pending));
                break;
            case SHIPPED:
            case PROCESSING:
                view.setBackgroundResource(R.drawable.bg_status_shipped);
                view.setTextColor(ctx.getColor(R.color.status_shipped));
                break;
            case DELIVERED:
                view.setBackgroundResource(R.drawable.bg_status_delivered);
                view.setTextColor(ctx.getColor(R.color.status_delivered));
                break;
            case CANCELLED:
                view.setBackgroundResource(R.drawable.bg_status_pending);
                view.setTextColor(ctx.getColor(R.color.status_cancelled));
                break;
        }
    }
}
