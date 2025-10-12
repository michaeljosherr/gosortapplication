package com.example.gosortapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class NotificationDetailFragment extends Fragment {

    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_META = "arg_meta";
    private static final String ARG_PRIORITY = "arg_priority";

    public static NotificationDetailFragment newInstance(String message, String meta, boolean isHighPriority) {
        NotificationDetailFragment f = new NotificationDetailFragment();
        Bundle b = new Bundle();
        b.putString(ARG_MESSAGE, message);
        b.putString(ARG_META, meta);
        b.putBoolean(ARG_PRIORITY, isHighPriority);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notification_detail, container, false);
        TextView tvMessage = v.findViewById(R.id.tvDetailMessage);
        TextView tvMeta = v.findViewById(R.id.tvDetailMeta);
        TextView tvPriority = v.findViewById(R.id.tvDetailPriority);

        androidx.appcompat.widget.Toolbar toolbar = v.findViewById(R.id.toolbarDetail);
        if (toolbar != null) {
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            toolbar.setNavigationOnClickListener(x -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else {
                    requireActivity().onBackPressed();
                }
            });
        }

        Bundle args = getArguments();
        if (args != null) {
            String msg = args.getString(ARG_MESSAGE);
            String meta = args.getString(ARG_META);
            boolean p = args.getBoolean(ARG_PRIORITY, false);
            tvMessage.setText(msg);
            tvMeta.setText(meta);
            tvPriority.setVisibility(p ? View.VISIBLE : View.GONE);
            if (toolbar != null) toolbar.setTitle(p ? "Important" : "Notification");
        }

        return v;
    }
}
