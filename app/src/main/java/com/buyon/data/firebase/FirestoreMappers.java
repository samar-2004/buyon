package com.buyon.data.firebase;

import com.buyon.domain.model.CartItem;
import com.buyon.domain.model.Category;
import com.buyon.domain.model.Order;
import com.buyon.domain.model.OrderLine;
import com.buyon.domain.model.Product;
import com.buyon.domain.model.UserProfile;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class FirestoreMappers {

    private FirestoreMappers() {}

    static Product productFromDoc(DocumentSnapshot doc) {
        Double price  = doc.getDouble("price");
        Double rating = doc.getDouble("rating");
        Long sold     = doc.getLong("soldCount");
        Long stock    = doc.getLong("stockQuantity");
        return new Product(
                doc.getId(),
                str(doc.getString("name")),
                str(doc.getString("description")),
                price  != null ? price  : 0,
                str(doc.getString("imageUrl")),
                str(doc.getString("categoryId")),
                rating != null ? rating : 0,
                sold   != null ? sold.intValue() : 0,
                stock  != null ? stock.intValue() : 0,
                str(doc.getString("locationLabel")));
    }

    static Category categoryFromDoc(DocumentSnapshot doc) {
        return new Category(
                doc.getId(),
                str(doc.getString("name")),
                str(doc.getString("iconName")));
    }

    static CartItem cartItemFromDoc(DocumentSnapshot doc) {
        Double unitPrice = doc.getDouble("unitPrice");
        Long qty         = doc.getLong("quantity");
        return new CartItem(
                doc.getId(),
                str(doc.getString("name")),
                unitPrice != null ? unitPrice : 0,
                str(doc.getString("imageUrl")),
                qty != null ? qty.intValue() : 0);
    }

    // ── UserProfile ─────────────────────────────────────────────────────

    static UserProfile profileFromDoc(DocumentSnapshot doc, String uid) {
        return new UserProfile(
                uid,
                str(doc.getString("displayName")),
                str(doc.getString("email")),
                str(doc.getString("phone")),
                str(doc.getString("defaultShippingAddress")),
                str(doc.getString("role")));       // ← role field
    }

    static Map<String, Object> profileToMap(UserProfile p) {
        Map<String, Object> m = new HashMap<>();
        m.put("displayName",          p.getDisplayName());
        m.put("email",                p.getEmail());
        m.put("phone",                p.getPhone());
        m.put("defaultShippingAddress", p.getDefaultShippingAddress());
        // Only write role when it is explicitly set (avoid overwriting admin promotions)
        if (p.getRole() != null && !p.getRole().isEmpty()) {
            m.put("role", p.getRole());
        }
        return m;
    }

    // ── Order ────────────────────────────────────────────────────────────

    static Order orderFromDoc(DocumentSnapshot doc) {
        Timestamp ts  = doc.getTimestamp("createdAt");
        long millis   = ts != null ? ts.toDate().getTime() : 0L;
        Double sub    = doc.getDouble("subtotal");
        Double ship   = doc.getDouble("shippingFee");
        Double tot    = doc.getDouble("total");
        String statusStr = doc.getString("status");
        Order.Status status = parseOrderStatus(statusStr);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawLines = (List<Map<String, Object>>) doc.get("lines");
        List<OrderLine> lines = new ArrayList<>();
        if (rawLines != null) {
            for (Map<String, Object> m : rawLines) lines.add(orderLineFromMap(m));
        }

        return new Order(
                doc.getId(),
                str(doc.getString("userId")),
                millis,
                sub  != null ? sub  : 0,
                ship != null ? ship : 0,
                tot  != null ? tot  : 0,
                str(doc.getString("shippingAddress")),
                str(doc.getString("promoCode")),
                status,
                lines,
                str(doc.getString("customerDisplayName")),
                str(doc.getString("customerEmail")),
                str(doc.getString("customerPhone")),
                str(doc.getString("paymentMethod")),
                str(doc.getString("paymentStatus")),
                str(doc.getString("paymentReference")),
                str(doc.getString("cancellationReason")));
    }

    private static OrderLine orderLineFromMap(Map<String, Object> m) {
        double up  = 0;
        Object upObj = m.get("unitPrice");
        if (upObj instanceof Number) up = ((Number) upObj).doubleValue();
        int qty    = 0;
        Object qObj  = m.get("quantity");
        if (qObj instanceof Number) qty = ((Number) qObj).intValue();
        return new OrderLine(
                str((String) m.get("productId")),
                str((String) m.get("name")),
                up, qty);
    }

    private static Order.Status parseOrderStatus(String s) {
        if (s == null) return Order.Status.PENDING;
        try { return Order.Status.valueOf(s); }
        catch (IllegalArgumentException e) { return Order.Status.PENDING; }
    }

    // ── Cart helpers ─────────────────────────────────────────────────────

    static Map<String, Object> cartItemToMap(CartItem item) {
        Map<String, Object> m = new HashMap<>();
        m.put("name",      item.getName());
        m.put("unitPrice", item.getUnitPrice());
        m.put("imageUrl",  item.getImageUrl());
        m.put("quantity",  item.getQuantity());
        return m;
    }

    static List<Map<String, Object>> orderLinesToMaps(List<OrderLine> lines) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrderLine line : lines) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", line.getProductId());
            m.put("name",      line.getName());
            m.put("unitPrice", line.getUnitPrice());
            m.put("quantity",  line.getQuantity());
            out.add(m);
        }
        return out;
    }

    private static String str(String s) { return s != null ? s : ""; }
}
