package com.example.gosortapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.example.gosortapplication.BuildConfig;

public class NotificationsFragment extends Fragment {

    private RecyclerView rv;
    private NotificationAdapter adapter;
    private List<NotificationItem> data;
    private View layoutEmpty;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Repository is already init'd by MainActivity — safe to use immediately
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notifications, container, false);

        rv          = v.findViewById(R.id.rvNotifications);
        layoutEmpty = v.findViewById(R.id.layoutEmpty);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        NotificationRepository repo = NotificationRepository.get();
        data = repo.getAll();

        // Test/demo data — debug builds only
        if (BuildConfig.DEBUG && data.isEmpty()) {
            data.add(new NotificationItem(
                    "Bin 'Non-Biodegradable' is FULL at 95%. Please empty immediately.",
                    "© 2026-03-09 | Device: TEST123 | Bin: Non-Biodegradable | Fullness: 95%",
                    true, "Non-Biodegradable", 95));
            data.add(new NotificationItem(
                    "Bin 'Biodegradable' has reached 75% capacity. Consider emptying soon.",
                    "© 2026-03-09 | Device: TEST123 | Bin: Biodegradable | Fullness: 75%",
                    false, "Biodegradable", 75));
            data.add(new NotificationItem(
                    "Bin 'Mixed' sensor MALFUNCTION detected! Please check immediately.",
                    "© 2026-03-09 | Device: TEST123 | Bin: Mixed | Status: Sensor Error",
                    true, "Mixed", -1));
            data.add(new NotificationItem(
                    "Bin 'Hazardous' is COMPLETELY FULL at 100%! Immediate emptying required.",
                    "© 2026-03-09 | Device: TEST123 | Bin: Hazardous | Fullness: 100% | Priority: CRITICAL",
                    true, "Hazardous", 100));
        }

        adapter = new NotificationAdapter(data, (item, pos) -> showActionDialog(item, pos));
        rv.setAdapter(adapter);

        View markAll = v.findViewById(R.id.tvMarkAll);
        if (markAll != null) {
            markAll.setOnClickListener(x -> {
                repo.markAllRead();
                adapter.notifyItemRangeChanged(0, data.size());
                updateEmptyState();
            });
        }

        updateEmptyState();
        return v;
    }

    /**
     * Called by MainActivity whenever a new notification is added via polling,
     * so the list stays in sync without this fragment doing its own polling.
     */
    public void onNewNotification() {
        if (adapter == null) return;
        adapter.notifyItemInserted(0);
        if (rv != null) rv.scrollToPosition(0);
        updateEmptyState();
    }

    // ─── Action dialog ────────────────────────────────────────────────────────

    private void showActionDialog(NotificationItem item, int position) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_notification_action, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // View Details
        dialogView.findViewById(R.id.btnViewDetails).setOnClickListener(v -> {
            dialog.dismiss();
            NotificationRepository.get().markReadById(item.id);
            adapter.notifyItemChanged(position);

            NotificationDetailFragment detail = NotificationDetailFragment.newInstance(
                    item.id,
                    item.message,
                    item.meta,
                    item.isHighPriority,
                    item.binName,
                    item.fullnessLevel
            );
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detail)
                    .addToBackStack(null)
                    .commit();
        });

        // Mark as Resolved
        dialogView.findViewById(R.id.btnResolved).setOnClickListener(v -> {
            dialog.dismiss();
            int idx = data.indexOf(item);
            NotificationRepository.get().deleteById(item.id);
            // Tell MainActivity to allow fresh alerts for this bin
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).clearLastNotifiedFullness(item.binName);
            if (idx >= 0) adapter.notifyItemRemoved(idx);
            updateEmptyState();
        });

        // Cancel
        dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ─── Empty state ──────────────────────────────────────────────────────────

    private void updateEmptyState() {
        if (layoutEmpty == null) return;
        boolean empty = data.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}