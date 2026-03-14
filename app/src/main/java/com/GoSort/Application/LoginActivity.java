package com.GoSort.Application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private GoSortApiClient apiClient;
    private ProgressBar progressBar;
    private static final String PREF_NAME = "GoSort";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_LASTNAME = "lastName";
    private static final String KEY_ROLE = "role";
    private static final String KEY_ASSIGNED_FLOOR = "assignedFloor";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_SORTER_DEVICE_NAME = "sorter_device_name";
    private static final String KEY_SORTER_DEVICE_ID = "sorter_device_id";
    private static final String KEY_SORTER_LOCATION = "sorter_location";
    private static final String KEY_SORTER_STATUS = "sorter_status";
    private static final String KEY_SORTER_MAINTENANCE = "sorter_maintenance";
    private static final String KEY_SORTER_FLOOR = "sorter_floor";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if already logged in
        if (getSharedPreferences(PREF_NAME, MODE_PRIVATE).getBoolean(KEY_IS_LOGGED_IN, false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        apiClient = new GoSortApiClient();

        EditText emailInput    = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton     = findViewById(R.id.loginButton);
        progressBar            = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> {
            String email    = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);

            apiClient.login(email, password, new GoSortApiClient.LoginCallback() {
                @Override
                public void onSuccess(JSONObject userData) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);

                        try {
                            Log.d(TAG, "Full userData: " + userData.toString(2));
                            Log.d(TAG, "username: " + userData.optString("username"));
                            Log.d(TAG, "email: "    + userData.optString("email"));

                            // Role restriction — mobile app is for Utility Members only
                            String role = userData.optString("role", "").toLowerCase().trim();
                            if (!role.equals("utility")) {
                                Toast.makeText(LoginActivity.this,
                                        "Access denied. This app is for Utility Members only.",
                                        Toast.LENGTH_LONG).show();
                                Log.w(TAG, "Login blocked for role: " + role);
                                return;
                            }

                            JSONObject sorter = userData.optJSONObject("sorter");

                            android.content.SharedPreferences.Editor editor =
                                    getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();

                            editor.putString(KEY_USER_ID,        userData.optString("userId"))
                                    .putString(KEY_USERNAME,       userData.optString("username"))
                                    .putString(KEY_LASTNAME,       userData.optString("lastName"))
                                    .putString(KEY_ROLE,           userData.optString("role"))
                                    .putString(KEY_ASSIGNED_FLOOR, userData.optString("assignedFloor"))
                                    .putString(KEY_TOKEN,          userData.optString("token"))
                                    .putString("email",            userData.optString("email"))
                                    .putBoolean(KEY_IS_LOGGED_IN,  true);

                            if (sorter != null) {
                                Log.d(TAG, "Sorter data: " + sorter.toString(2));
                                editor.putString(KEY_SORTER_DEVICE_NAME, sorter.optString("device_name"))
                                        .putString(KEY_SORTER_DEVICE_ID,   sorter.optString("device_identity"))
                                        .putString(KEY_SORTER_LOCATION,    sorter.optString("location"))
                                        .putString(KEY_SORTER_STATUS,      sorter.optString("status"))
                                        .putString(KEY_SORTER_MAINTENANCE, String.valueOf(sorter.optBoolean("maintenance_mode")))
                                        .putString(KEY_SORTER_FLOOR,       sorter.optString("assigned_floor"))
                                        .putString("sorter",               sorter.toString());
                            } else {
                                Log.w(TAG, "No sorter object found in userData!");
                            }

                            editor.apply();

                            Log.i(TAG, "Preferences saved, navigating to MainActivity");
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing login response: " + e.getMessage(), e);
                            Toast.makeText(LoginActivity.this,
                                    "Error processing login response", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Login failed: " + message);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
}