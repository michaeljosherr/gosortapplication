package com.GoSort.Application;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnalyticsFragment extends Fragment implements DeviceAdapter.OnDeviceClickListener {
    private static final String TAG = "AnalyticsFragment";
    private static final String BASE_URL = "https://web-production-15f71.up.railway.app/api/";
    private static final String PREF_NAME = "GoSort";

    private View root;
    private ConcentricDonutView donut;

    // Legend % labels
    private TextView tvBiodeg, tvNonBiodeg, tvMixed, tvHazardous;

    // Today's count cards
    private TextView tvCountBiodeg, tvCountNonBiodeg, tvCountMixed, tvCountHazardous;
    private TextView tvTotalCount;

    // Device selector
    private DeviceAdapter deviceAdapter;
    private String currentDeviceId = "";

    private final int[] cols = new int[]{0xFFF39C12, 0xFF4A90E2, 0xFF27AE60, 0xFFE74C3C};

    private AnalyticsModel model;
    private AnalyticsModel.DailyListener dailyListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_analytics, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
        setupDeviceRecyclerView();

        model = AnalyticsModel.init(requireContext());

        dailyListener = (counts, percentages, total) -> {
            if (getActivity() == null || root == null) return;
            getActivity().runOnUiThread(() -> {
                // Update donut with today's percentages
                if (donut != null) donut.setData(percentages, cols);

                // Update legend
                setLegendText(tvBiodeg,    percentages[0]);
                setLegendText(tvNonBiodeg, percentages[1]);
                setLegendText(tvMixed,     percentages[2]);
                setLegendText(tvHazardous, percentages[3]);

                // Update count cards
                if (tvCountBiodeg    != null) tvCountBiodeg.setText(String.valueOf(counts[0]));
                if (tvCountNonBiodeg != null) tvCountNonBiodeg.setText(String.valueOf(counts[1]));
                if (tvCountMixed     != null) tvCountMixed.setText(String.valueOf(counts[2]));
                if (tvCountHazardous != null) tvCountHazardous.setText(String.valueOf(counts[3]));
                if (tvTotalCount     != null) tvTotalCount.setText(
                        String.format(Locale.getDefault(), "%d items sorted today", total));
            });
        };
        model.addDailyListener(dailyListener);
    }

    private void initViews() {
        donut            = root.findViewById(R.id.concentricDonut);
        tvBiodeg         = root.findViewById(R.id.tvBiodeg);
        tvNonBiodeg      = root.findViewById(R.id.tvNonBiodeg);
        tvMixed          = root.findViewById(R.id.tvMixed);
        tvHazardous      = root.findViewById(R.id.tvHazardous);
        tvCountBiodeg    = root.findViewById(R.id.tvCountBiodeg);
        tvCountNonBiodeg = root.findViewById(R.id.tvCountNonBiodeg);
        tvCountMixed     = root.findViewById(R.id.tvCountMixed);
        tvCountHazardous = root.findViewById(R.id.tvCountHazardous);
        tvTotalCount     = root.findViewById(R.id.tvTotalCount);
    }

    private void setLegendText(TextView tv, int pct) {
        if (tv == null) return;
        tv.setText(String.format(Locale.getDefault(), "%d%%", pct));
        tv.setTextColor(0xFF333333);
    }

    private void setupDeviceRecyclerView() {
        RecyclerView recycler = root.findViewById(R.id.recyclerDevicesAnalytics);
        if (recycler == null) { Log.e(TAG, "recyclerDevicesAnalytics not found"); return; }
        deviceAdapter = new DeviceAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(deviceAdapter);
        fetchAssignedDevices();
    }

    private void fetchAssignedDevices() {
        android.content.SharedPreferences prefs =
                requireActivity().getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");
        if (username.isEmpty()) return;

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "user_details_api.php?username=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                if (!json.optBoolean("success", false)) return;

                JSONArray sorters = json.getJSONObject("data").getJSONArray("assigned_sorters");
                List<JSONObject> deviceList = new ArrayList<>();
                for (int i = 0; i < sorters.length(); i++) deviceList.add(sorters.getJSONObject(i));

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    deviceAdapter.setDevices(deviceList);
                    if (!deviceList.isEmpty()) onDeviceClick(deviceList.get(0));
                });
            } catch (Exception e) {
                Log.e(TAG, "fetchAssignedDevices error: " + e.getMessage());
            }
        }).start();

    }

    @Override
    public void onDeviceClick(JSONObject device) {
        try {
            currentDeviceId = device.optString("device_identity", "");
            for (int i = 0; i < deviceAdapter.getItemCount(); i++) {
                if (deviceAdapter.getDevices().get(i)
                        .optString("device_identity", "").equals(currentDeviceId)) {
                    deviceAdapter.setSelectedPosition(i);
                    break;
                }
            }
            model.fetch(currentDeviceId);
        } catch (Exception e) {
            Log.e(TAG, "onDeviceClick error: " + e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (model != null && dailyListener != null) model.removeDailyListener(dailyListener);
        donut = null; tvBiodeg = null; tvNonBiodeg = null;
        tvMixed = null; tvHazardous = null;
        tvCountBiodeg = null; tvCountNonBiodeg = null;
        tvCountMixed = null; tvCountHazardous = null;
        tvTotalCount = null; root = null;
    }
}