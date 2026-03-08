package com.example.gosortapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface Listener { void onClick(NotificationItem item, int pos); }

    private final List<NotificationItem> items;
    private final Listener listener;

    // Color constants matching app theme
    private static final int COLOR_BIO       = 0xFFF39C12; // orange
    private static final int COLOR_NON_BIO   = 0xFF4A90E2; // blue
    private static final int COLOR_MIXED     = 0xFF27AE60; // green
    private static final int COLOR_HAZARDOUS = 0xFFE74C3C; // red
    private static final int COLOR_DEFAULT   = 0xFF9E9E9E; // grey

    public NotificationAdapter(List<NotificationItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationItem it = items.get(position);

        // Bin name
        String binName = it.binName != null && !it.binName.isEmpty() ? it.binName : "Bin Alert";
        h.tvBinName.setText(binName);

        // Message — strip emoji prefixes since we have icons now
        h.tvMessage.setText(it.message);

        // Timestamp — pull just the timestamp portion from meta
        h.tvMeta.setText(extractTimestamp(it.meta));

        // Priority badge
        h.tvBadge.setVisibility(it.isHighPriority ? View.VISIBLE : View.GONE);

        // Unread dot
        h.viewUnreadDot.setVisibility(!it.isRead ? View.VISIBLE : View.GONE);

        // Icon emoji based on fullness
        if (it.fullnessLevel == -1) {
            h.tvIcon.setText("⚠️");
        } else if (it.fullnessLevel >= 100) {
            h.tvIcon.setText("🚨");
        } else if (it.fullnessLevel >= 90) {
            h.tvIcon.setText("⚠️");
        } else {
            h.tvIcon.setText("🗑️");
        }

        // Icon background tint by bin type
        int color = binColor(binName);
        h.viewIconBg.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(withAlpha(color, 30)));

        // Card background — unread gets a very subtle tint
        h.llInner.setBackgroundColor(it.isRead ? 0xFFFFFFFF : 0xFFF9FFFA);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(it, position);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int binColor(String binName) {
        if (binName == null) return COLOR_DEFAULT;
        String lower = binName.toLowerCase();
        if (lower.contains("bio") && !lower.contains("non")) return COLOR_BIO;
        if (lower.contains("non")) return COLOR_NON_BIO;
        if (lower.contains("mix"))  return COLOR_MIXED;
        if (lower.contains("haz"))  return COLOR_HAZARDOUS;
        return COLOR_DEFAULT;
    }

    /** Returns color with specified alpha (0-255) */
    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /** Extracts just the timestamp from the meta string */
    private String extractTimestamp(String meta) {
        if (meta == null || meta.isEmpty()) return "";
        // meta format: "© 2024-01-01 12:00:00 | Device: ..."
        String[] parts = meta.split("\\|");
        if (parts.length > 0) {
            return parts[0].replace("©", "").trim();
        }
        return meta;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvBinName, tvMessage, tvMeta, tvBadge, tvIcon;
        View viewIconBg, viewUnreadDot, llInner;

        VH(@NonNull View itemView) {
            super(itemView);
            tvBinName    = itemView.findViewById(R.id.tvBinName);
            tvMessage    = itemView.findViewById(R.id.tvMessage);
            tvMeta       = itemView.findViewById(R.id.tvMeta);
            tvBadge      = itemView.findViewById(R.id.tvBadge);
            tvIcon       = itemView.findViewById(R.id.tvIcon);
            viewIconBg   = itemView.findViewById(R.id.viewIconBg);
            viewUnreadDot= itemView.findViewById(R.id.viewUnreadDot);
            llInner      = itemView.findViewById(R.id.llInner);
        }
    }
}
