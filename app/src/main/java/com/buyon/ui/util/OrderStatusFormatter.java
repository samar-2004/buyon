package com.buyon.ui.util;

import android.content.Context;

import com.buyon.app.R;
import com.buyon.domain.model.Order;

public final class OrderStatusFormatter {

    private OrderStatusFormatter() {}

    public static String label(Context ctx, Order.Status status) {
        if (status == null) return "";
        switch (status) {
            case PENDING:
                return ctx.getString(R.string.order_status_pending);
            case PROCESSING:
                return ctx.getString(R.string.order_status_processing);
            case SHIPPED:
                return ctx.getString(R.string.order_status_shipped);
            case DELIVERED:
                return ctx.getString(R.string.order_status_delivered);
            case CANCELLED:
            default:
                return ctx.getString(R.string.order_status_cancelled);
        }
    }
}
