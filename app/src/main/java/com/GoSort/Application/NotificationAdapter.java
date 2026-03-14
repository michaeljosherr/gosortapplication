package com.GoSort.Application;

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

    public NotificationAdapter(List<NotificationItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationItem it = items.get(position);
        holder.tvMessage.setText(it.message);
        holder.tvMeta.setText(it.meta);
        holder.tvBadge.setVisibility(it.isHighPriority ? View.VISIBLE : View.GONE);

        // change background based on read state (llInner is the inner horizontal container)
        View bgTarget = holder.itemView.findViewById(R.id.llInner);
        if (bgTarget != null) {
            if (it.isRead) {
                bgTarget.setBackgroundColor(0x00FFFFFF); // transparent
            } else {
                bgTarget.setBackgroundResource(R.drawable.notification_unread);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(it, position);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvMeta, tvBadge;
        VH(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }
    }
}
