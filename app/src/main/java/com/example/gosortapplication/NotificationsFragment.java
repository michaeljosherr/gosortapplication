package com.example.gosortapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rv;
    private NotificationAdapter adapter;
    private List<NotificationItem> data;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notifications, container, false);
        rv = v.findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Use central repository for notifications so other parts of the app can observe unread count
        NotificationRepository repo = NotificationRepository.get();

        if (repo.getAll().isEmpty()) {
            repo.setInitial(new ArrayList<NotificationItem>() {{
                add(new NotificationItem("Bin 'Bio' is FULL! Distance reading has been consistently between 0-10cm for the last 5 readings. Please empty immediately.",
                        "© 2025-10-11 15:35:24 | Device: sorter | Bin: Bio | Fullness: 100%", true));
                add(new NotificationItem("Bin 'Recyclable' is FULL! Distance reading has been consistently between 0-10cm for the last 5 readings. Please empty immediately.",
                        "© 2025-10-11 15:35:24 | Device: sorter | Bin: Recyclable | Fullness: 100%", true));
                add(new NotificationItem("Bin 'Extra' is FULL! Distance reading has been consistently between 0-10cm for the last 5 readings. Please empty immediately.",
                        "© 2025-10-11 15:35:23 | Device: sorter | Bin: Extra | Fullness: 100%", true));
            }});
        }

        data = repo.getAll();

                adapter = new NotificationAdapter(data, (item, pos) -> {
                        // mark as read via repository which will notify listeners
                        repo.markRead(pos);
                        adapter.notifyItemChanged(pos);

                        // Open detail fragment to show notification in full
                        NotificationDetailFragment detail = NotificationDetailFragment.newInstance(item.message, item.meta, item.isHighPriority);
                        getParentFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, detail)
                                        .addToBackStack(null)
                                        .commit();
                });

        rv.setAdapter(adapter);
                View markAll = v.findViewById(R.id.tvMarkAll);
                if (markAll != null) {
                        final NotificationRepository r = NotificationRepository.get();
                        markAll.setOnClickListener(x -> {
                                r.markAllRead();
                                adapter.notifyDataSetChanged();
                        });
                }
        return v;
    }
}
