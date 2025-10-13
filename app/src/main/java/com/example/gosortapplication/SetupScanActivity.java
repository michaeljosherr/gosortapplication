package com.example.gosortapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class SetupScanActivity extends AppCompatActivity {
    private LinearLayout list;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button scanButton;
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_scan);

        list = findViewById(R.id.listContainer);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        scanButton = findViewById(R.id.btnChooseIp);

        executor = Executors.newFixedThreadPool(50);

        scanButton.setText(getString(R.string.scan_network));
        scanButton.setOnClickListener(v -> startNetworkScan());
    }

    private void startNetworkScan() {
        list.removeAllViews();
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        scanButton.setEnabled(false);
        statusText.setText(R.string.scanning_network);

        scanNetwork();
    }

    private void scanNetwork() {
        executor.execute(() -> {
            List<String> gosortServers = new ArrayList<>();

            try {
                // Get WiFi info
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                String localIp = String.format(Locale.US, "%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));

                // Get network prefix
                String[] ipParts = localIp.split("\\.");
                String networkPrefix = String.format(Locale.US, "%s.%s.%s", ipParts[0], ipParts[1], ipParts[2]);

                List<String> ipToCheck = new ArrayList<>();
                for (int i = 1; i < 255; i++) {
                    ipToCheck.add(networkPrefix + "." + i);
                }

                int totalIps = ipToCheck.size();
                int scanned = 0;

                for (String ip : ipToCheck) {
                    executor.execute(() -> {
                        try {
                            URL url = new URL("http://" + ip + "/GoSort_Web/gs_DB/trash_detected.php");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(500); // 500ms timeout
                            conn.setReadTimeout(500);
                            int responseCode = conn.getResponseCode();

                            if (responseCode == 200 ||
                               (responseCode == 400 && conn.getResponseMessage().contains("No trash type provided"))) {
                                synchronized (gosortServers) {
                                    gosortServers.add(ip);
                                }
                            }
                        } catch (Exception ignored) {}
                    });

                    scanned++;
                    int finalScanned = scanned;
                    mainHandler.post(() -> {
                        progressBar.setProgress((finalScanned * 100) / totalIps);
                        statusText.setText(getString(R.string.scanning_network_progress, (finalScanned * 100) / totalIps));
                    });
                }

                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }

            } catch (Exception e) {
                android.util.Log.e("SetupScanActivity", "Error scanning network", e);
            }

            mainHandler.post(() -> updateUIWithResults(gosortServers));
        });
    }

    private void updateUIWithResults(List<String> gosortServers) {
        progressBar.setVisibility(View.GONE);
        scanButton.setEnabled(true);

        if (gosortServers.isEmpty()) {
            statusText.setText(R.string.no_servers_found);
            return;
        }

        statusText.setText(getString(R.string.servers_found, gosortServers.size()));

        for (String ip : gosortServers) {
            TextView tv = new TextView(SetupScanActivity.this);
            tv.setText(ip);
            tv.setTextSize(16f);
            tv.setPadding(12,12,12,12);
            tv.setBackgroundResource(R.drawable.card_inner_bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0,8,0,8);
            list.addView(tv, lp);

            tv.setOnClickListener(v -> {
                Intent i = new Intent(SetupScanActivity.this, DeviceIdentityActivity.class);
                i.putExtra("ip", ip);
                startActivity(i);
                finish();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}
