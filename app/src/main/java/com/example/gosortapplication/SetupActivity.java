package com.example.gosortapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {
    private GoSortApiClient apiClient;
    private NetworkScanner networkScanner;
    private EditText ipAddressInput;
    private ProgressBar progressBar;
    private Button btnScan;
    private final List<String> goSortServers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        apiClient = new GoSortApiClient();
        networkScanner = new NetworkScanner();
        ViewPager2 pager = findViewById(R.id.viewPager);
        Button btn = findViewById(R.id.btnSetupDevice);
        btnScan = findViewById(R.id.btnScan);
        ipAddressInput = findViewById(R.id.ipAddressInput);
        progressBar = findViewById(R.id.progressBar);

        List<OnboardingAdapter.Page> pages = new ArrayList<>();
        pages.add(new OnboardingAdapter.Page("Welcome! Let's Get You Started", "Set up your device in a few easy steps."));
        pages.add(new OnboardingAdapter.Page("Connect Device", "Enter your GoSort device's IP address to connect."));
        pages.add(new OnboardingAdapter.Page("Monitor Status", "See fullness and real-time stats."));
        pages.add(new OnboardingAdapter.Page("You're Ready", "Finish setup and start sorting!"));

        OnboardingAdapter adapter = new OnboardingAdapter(pages);
        pager.setAdapter(adapter);

        View dot0 = findViewById(R.id.dot0);
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        View[] dots = new View[]{dot0, dot1, dot2, dot3};

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setBackgroundResource(i == position ? R.drawable.unread_dot : R.drawable.dot_unselected);
                }

                if (position == 1) { // Connect Device page
                    ipAddressInput.setVisibility(View.VISIBLE);
                    btnScan.setVisibility(View.VISIBLE); // Show scan button on connection page
                    btn.setText(R.string.connect);
                } else if (position == pages.size() - 1) {
                    ipAddressInput.setVisibility(View.GONE);
                    btnScan.setVisibility(View.GONE); // Hide scan button
                    btn.setText(R.string.get_started);
                } else {
                    ipAddressInput.setVisibility(View.GONE);
                    btnScan.setVisibility(View.GONE); // Hide scan button
                    btn.setText(position == 0 ? R.string.setup_device : R.string.next);
                }
            }
        });

        btnScan.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            btnScan.setEnabled(false);
            btn.setEnabled(false);
            goSortServers.clear();

            networkScanner.scanNetwork(new NetworkScanner.ScanCallback() {
                @Override
                public void onDeviceFound(String ipAddress) {
                    runOnUiThread(() -> goSortServers.add(ipAddress));
                }

                @Override
                public void onScanProgress(int progress) {
                    runOnUiThread(() -> progressBar.setProgress(progress));
                }

                @Override
                public void onScanComplete() {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnScan.setEnabled(true);
                        btn.setEnabled(true);

                        if (goSortServers.isEmpty()) {
                            Toast.makeText(SetupActivity.this,
                                R.string.no_gosort_servers_found, Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Show simple list of found GoSort servers
                        new AlertDialog.Builder(SetupActivity.this)
                            .setTitle(R.string.select_gosort_server)
                            .setItems(goSortServers.toArray(new String[0]),
                                (dialog, which) -> ipAddressInput.setText(goSortServers.get(which)))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnScan.setEnabled(true);
                        btn.setEnabled(true);
                        Toast.makeText(SetupActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        btn.setOnClickListener(v -> {
            int pos = pager.getCurrentItem();
            if (pos == 1) { // Connect Device page
                String ipAddress = ipAddressInput.getText().toString().trim();
                if (ipAddress.isEmpty()) {
                    Toast.makeText(this, R.string.enter_ip_address, Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                btn.setEnabled(false);

                apiClient.testConnection(ipAddress, new GoSortApiClient.ApiCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btn.setEnabled(true);
                            // Save IP for future use
                            getSharedPreferences("GoSort", MODE_PRIVATE)
                                .edit()
                                .putString("device_ip", ipAddress)
                                .apply();

                            // Navigate to LoginActivity instead of continuing onboarding
                            Intent loginIntent = new Intent(SetupActivity.this, LoginActivity.class);
                            startActivity(loginIntent);
                            finish();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btn.setEnabled(true);
                            Toast.makeText(SetupActivity.this,
                                getString(R.string.connection_failed, message),
                                Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } else if (pos < pages.size() - 1) {
                pager.setCurrentItem(pos + 1, true);
            } else {
                Intent i = new Intent(SetupActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        });
    }
}