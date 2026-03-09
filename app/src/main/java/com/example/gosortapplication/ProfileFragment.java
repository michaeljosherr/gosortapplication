package com.example.gosortapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private static final String PREF_NAME = "GoSort";
    private static final String KEY_DEVICE_IP = "device_ip";
    private static final String BASE_URL = "https://web-production-15f71.up.railway.app/api/";
    private static final long ONLINE_THRESHOLD_MS = 5 * 60 * 1000;

    private ImageView imgAvatar;
    private ActivityResultLauncher<String> pickImageLauncher;

    private TextView txtUsername;
    private TextView txtRole;
    private TextView tvDeviceCount;
    private RecyclerView recyclerAssignedSorters;
    private AssignedSortersAdapter assignedSortersAdapter;

    private Handler handler;
    private Runnable updateRunnable;
    private UserDetailsApi userDetailsApi;

    private List<JSONObject> currentSorterList = new ArrayList<>();
    private boolean sortersLoaded = false;

    // ─── Per-user avatar keys ─────────────────────────────────────────────────

    private String avatarKey() {
        String username = requireActivity()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString("username", "default");
        return "avatar_path_" + username;
    }

    private String avatarFileName() {
        String username = requireActivity()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString("username", "default");
        return "avatar_" + username + ".png";
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        imgAvatar               = root.findViewById(R.id.imgAvatar);
        txtUsername             = root.findViewById(R.id.txtUsername);
        txtRole                 = root.findViewById(R.id.txtRole);
        tvDeviceCount           = root.findViewById(R.id.tvDeviceCount);
        recyclerAssignedSorters = root.findViewById(R.id.recyclerAssignedSorters);

        String serverIp = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DEVICE_IP, "");
        userDetailsApi = new UserDetailsApi(serverIp);

        assignedSortersAdapter = new AssignedSortersAdapter();
        recyclerAssignedSorters.setAdapter(assignedSortersAdapter);

        loadSavedAvatar();
        setupPeriodicUpdates();

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                result -> { if (result != null) saveAndDisplayAvatar(result); });

        View btnEditProfilePic = root.findViewById(R.id.btnEditProfilePic);
        if (btnEditProfilePic != null)
            btnEditProfilePic.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Change Password row — shows contact admin dialog
        View btnChangePassword = root.findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null)
            btnChangePassword.setOnClickListener(v -> showContactAdminDialog());

        // Logout row
        View btnLogout = root.findViewById(R.id.btnLogout);
        if (btnLogout != null)
            btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        return root;
    }

    // ─── Avatar ───────────────────────────────────────────────────────────────

    private void saveAndDisplayAvatar(Uri uri) {
        new Thread(() -> {
            try {
                InputStream in = requireActivity().getContentResolver().openInputStream(uri);
                Bitmap original = android.graphics.BitmapFactory.decodeStream(in);
                if (in != null) in.close();
                if (original == null) return;

                Bitmap circular = toCircleBitmap(original);

                File file = new File(requireActivity().getFilesDir(), avatarFileName());
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                circular.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putString(avatarKey(), file.getAbsolutePath()).apply();

                final Bitmap bmp = circular;
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (imgAvatar != null) imgAvatar.setImageBitmap(bmp);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error saving avatar: " + e.getMessage());
            }
        }).start();
    }

    private void loadSavedAvatar() {
        String path = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(avatarKey(), "");
        if (path.isEmpty() || !new File(path).exists()) return;

        new Thread(() -> {
            try {
                Bitmap bm = android.graphics.BitmapFactory.decodeFile(path);
                if (bm == null) return;
                Bitmap circular = toCircleBitmap(bm);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (imgAvatar != null) imgAvatar.setImageBitmap(circular);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading avatar: " + e.getMessage());
            }
        }).start();
    }

    private Bitmap toCircleBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, size, size, true);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        return output;
    }

    // ─── Periodic updates ────────────────────────────────────────────────────

    private void setupPeriodicUpdates() {
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                fetchUserDetails();
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updateRunnable);
    }

    // ─── Step 1: user details ────────────────────────────────────────────────

    private void fetchUserDetails() {
        if (getActivity() == null) return;
        String username = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString("username", "");

        userDetailsApi.getUserDetails(username, new UserDetailsApi.UserDetailsCallback() {
            @Override
            public void onSuccess(JSONObject userDetails) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject userInfo = userDetails.getJSONObject("user_info");
                        JSONArray  sorters  = userDetails.getJSONArray("assigned_sorters");

                        txtUsername.setText(getString(R.string.full_name_format,
                                userInfo.getString("userName"),
                                userInfo.getString("lastName")));
                        txtRole.setText(formatRole(userInfo.getString("role")));
                        tvDeviceCount.setText(String.valueOf(sorters.length()));


                        if (!sortersLoaded) {
                            currentSorterList.clear();
                            for (int i = 0; i < sorters.length(); i++)
                                currentSorterList.add(sorters.getJSONObject(i));
                            assignedSortersAdapter.setSorters(new ArrayList<>(currentSorterList));
                            sortersLoaded = true;
                        }

                        for (int i = 0; i < currentSorterList.size(); i++) {
                            String deviceId = currentSorterList.get(i)
                                    .optString("device_identity", "");
                            if (!deviceId.isEmpty())
                                fetchBinFullnessForDevice(deviceId, i);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "fetchUserDetails error: " + message);
            }
        });
    }

    private String formatRole(String role) {
        if (role == null) return "";
        switch (role.toLowerCase().trim()) {
            case "utility": return "Utility Member";
            default:        return role;
        }
    }

    // ─── Step 2: bin fullness + online status ────────────────────────────────

    private void fetchBinFullnessForDevice(String deviceId, int sorterIndex) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "bin_fullness.php?device_identity=" + deviceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getResponseCode() == 200
                                ? conn.getInputStream()
                                : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                if (!json.optString("status", "").equals("success")) return;

                JSONArray binData = json.getJSONArray("data");

                int bio = 0, nonBio = 0, mixed = 0, hazardous = 0;
                boolean foundBio = false, foundNonBio = false,
                        foundMixed = false, foundHazardous = false;

                long latestTimestampMs = 0;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));

                for (int i = 0; i < binData.length(); i++) {
                    JSONObject bin = binData.getJSONObject(i);
                    String name   = bin.optString("bin_name", "").trim();
                    int pct       = bin.optInt("fullness_percentage", 0);

                    String ts = bin.optString("timestamp", "");
                    if (!ts.isEmpty()) {
                        try {
                            Date d = sdf.parse(ts);
                            if (d != null && d.getTime() > latestTimestampMs)
                                latestTimestampMs = d.getTime();
                        } catch (Exception ignored) {}
                    }

                    if (!foundBio       && name.equalsIgnoreCase("Bio"))       { bio       = pct; foundBio       = true; }
                    if (!foundNonBio    && name.equalsIgnoreCase("Non-Bio"))   { nonBio    = pct; foundNonBio    = true; }
                    if (!foundMixed     && name.equalsIgnoreCase("Mixed"))     { mixed     = pct; foundMixed     = true; }
                    if (!foundHazardous && name.equalsIgnoreCase("Hazardous")) { hazardous = pct; foundHazardous = true; }

                    if (foundBio && foundNonBio && foundMixed && foundHazardous) break;
                }

                long now = System.currentTimeMillis();
                boolean isOnline = latestTimestampMs > 0
                        && (now - latestTimestampMs) <= ONLINE_THRESHOLD_MS;

                final int fBio = bio, fNonBio = nonBio, fMixed = mixed, fHazardous = hazardous;
                final boolean fOnline = isOnline;

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (sorterIndex >= currentSorterList.size()) return;
                    assignedSortersAdapter.updateCard(sorterIndex, fBio, fNonBio, fMixed, fHazardous, fOnline);
                });

            } catch (Exception e) {
                Log.e(TAG, "fetchBinFullness error for " + deviceId + ": " + e.getMessage());
            }
        }).start();
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────

    private void showContactAdminDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_contact_admin, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Transparent background so our rounded XML shows properly
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.btnDialogOk).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showLogoutConfirmationDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_logout, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnDialogLogout).setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        dialog.show();
    }

    private void performLogout() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedIp    = prefs.getString(KEY_DEVICE_IP, "");
        String avatarPath = prefs.getString(avatarKey(), "");
        prefs.edit()
                .clear()
                .putString(KEY_DEVICE_IP, savedIp)
                .putString(avatarKey(), avatarPath)
                .apply();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && updateRunnable != null)
            handler.removeCallbacks(updateRunnable);
        sortersLoaded = false;
    }
}