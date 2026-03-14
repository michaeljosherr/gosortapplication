package com.GoSort.Application;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class NotificationDetailFragment extends Fragment {

    // FIX #4: ARG_ID replaces ARG_POSITION — stable across list mutations
    private static final String ARG_ID            = "arg_id";
    private static final String ARG_MESSAGE       = "arg_message";
    private static final String ARG_META          = "arg_meta";
    private static final String ARG_PRIORITY      = "arg_priority";
    private static final String ARG_BIN_NAME      = "arg_bin_name";
    private static final String ARG_FULLNESS      = "arg_fullness";

    // Bin colors matching app theme
    private static final int COLOR_BIO       = 0xFFF39C12;
    private static final int COLOR_NON_BIO   = 0xFF4A90E2;
    private static final int COLOR_MIXED     = 0xFF27AE60;
    private static final int COLOR_HAZARDOUS = 0xFFE74C3C;
    private static final int COLOR_DEFAULT   = 0xFF9E9E9E;

    // FIX #4: factory now takes id (String) instead of position (int)
    public static NotificationDetailFragment newInstance(String id,
                                                         String message, String meta,
                                                         boolean isHighPriority,
                                                         String binName, int fullness) {
        NotificationDetailFragment f = new NotificationDetailFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ID,       id);
        b.putString(ARG_MESSAGE,  message);
        b.putString(ARG_META,     meta);
        b.putBoolean(ARG_PRIORITY,isHighPriority);
        b.putString(ARG_BIN_NAME, binName);
        b.putInt(ARG_FULLNESS,    fullness);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notification_detail, container, false);

        TextView tvDetailTitle    = v.findViewById(R.id.tvDetailTitle);
        TextView tvDetailBinName  = v.findViewById(R.id.tvDetailBinName);
        TextView tvDetailFullness = v.findViewById(R.id.tvDetailFullness);
        TextView tvDetailMessage  = v.findViewById(R.id.tvDetailMessage);
        TextView tvDetailMeta     = v.findViewById(R.id.tvDetailMeta);
        TextView tvDetailPriority = v.findViewById(R.id.tvDetailPriority);
        TextView tvDetailIcon     = v.findViewById(R.id.tvDetailIcon);
        View     viewDetailIconBg = v.findViewById(R.id.viewDetailIconBg);
        MaterialButton btnMarkEmptied = v.findViewById(R.id.btnMarkEmptied);
        MaterialButton btnDismiss     = v.findViewById(R.id.btnDismiss);

        // Back button
        v.findViewById(R.id.btnBack).setOnClickListener(x -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0)
                getParentFragmentManager().popBackStack();
            else
                requireActivity().onBackPressed();
        });

        Bundle args = getArguments();
        if (args == null) return v;

        String  id         = args.getString(ARG_ID,       "");
        String  message    = args.getString(ARG_MESSAGE,  "");
        String  meta       = args.getString(ARG_META,     "");
        boolean highPri    = args.getBoolean(ARG_PRIORITY, false);
        String  binName    = args.getString(ARG_BIN_NAME,  "Bin Alert");
        int     fullness   = args.getInt(ARG_FULLNESS,     0);

        // Top bar title
        tvDetailTitle.setText(highPri ? "Urgent Alert" : "Notification");

        // Bin name
        tvDetailBinName.setText(binName.isEmpty() ? "Bin Alert" : binName);

        // Priority badge
        tvDetailPriority.setVisibility(highPri ? View.VISIBLE : View.GONE);

        // Fullness
        String fullnessText = fullness == -1 ? "Sensor Error" : fullness + "%";
        tvDetailFullness.setText(fullnessText);

        // Icon + bg color based on bin type
        int color = binColor(binName);
        viewDetailIconBg.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(withAlpha(color, 30)));

        if (fullness == -1)       tvDetailIcon.setText("⚠️");
        else if (fullness >= 100) tvDetailIcon.setText("🚨");
        else if (fullness >= 90)  tvDetailIcon.setText("⚠️");
        else                      tvDetailIcon.setText("🗑️");

        // Message and timestamp
        tvDetailMessage.setText(message);
        tvDetailMeta.setText(extractTimestamp(meta));

        // Button label based on notification type
        btnMarkEmptied.setText(fullness == -1 ? "🔧  Mark as Fixed" : "✅  Mark as Bin Emptied");

        // FIX #4: resolve by stable ID — immune to list mutations while detail is open
        btnMarkEmptied.setOnClickListener(x -> {
            if (!id.isEmpty())
                NotificationRepository.get().deleteById(id);
            if (getParentFragmentManager().getBackStackEntryCount() > 0)
                getParentFragmentManager().popBackStack();
            else
                requireActivity().onBackPressed();
        });

        // Dismiss — marks as read by stable ID
        btnDismiss.setOnClickListener(x -> {
            if (!id.isEmpty())
                NotificationRepository.get().markReadById(id);
            if (getParentFragmentManager().getBackStackEntryCount() > 0)
                getParentFragmentManager().popBackStack();
            else
                requireActivity().onBackPressed();
        });

        return v;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int binColor(String binName) {
        if (binName == null) return COLOR_DEFAULT;
        String lower = binName.toLowerCase();
        if (lower.contains("bio") && !lower.contains("non")) return COLOR_BIO;
        if (lower.contains("non")) return COLOR_NON_BIO;
        if (lower.contains("mix")) return COLOR_MIXED;
        if (lower.contains("haz")) return COLOR_HAZARDOUS;
        return COLOR_DEFAULT;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private String extractTimestamp(String meta) {
        if (meta == null || meta.isEmpty()) return "";
        String[] parts = meta.split("\\|");
        if (parts.length > 0) return parts[0].replace("©", "").trim();
        return meta;
    }
}