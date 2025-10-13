package com.example.gosortapplication;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkScanner {
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final GoSortApiClient apiClient;

    public interface ScanCallback {
        void onDeviceFound(String ipAddress);
        void onScanProgress(int progress);
        void onScanComplete();
        void onError(String message);
    }

    public NetworkScanner() {
        this.executor = Executors.newFixedThreadPool(10);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.apiClient = new GoSortApiClient();
    }

    public void scanNetwork(ScanCallback callback) {
        try {
            // Get local IP addresses
            List<String> localIPs = getLocalIPAddresses();
            if (localIPs.isEmpty()) {
                mainHandler.post(() -> callback.onError("No network interfaces found"));
                return;
            }

            for (String localIP : localIPs) {
                scanSubnet(localIP, callback);
            }
        } catch (Exception e) {
            mainHandler.post(() -> callback.onError("Error scanning network: " + e.getMessage()));
        }
    }

    private void scanSubnet(String localIP, ScanCallback callback) {
        String subnet = localIP.substring(0, localIP.lastIndexOf(".") + 1);
        AtomicInteger progress = new AtomicInteger(0);
        AtomicInteger activeThreads = new AtomicInteger(0);

        for (int i = 1; i < 255; i++) {
            String targetIP = subnet + i;
            activeThreads.incrementAndGet();

            executor.execute(() -> {
                try {
                    // First check if host is reachable
                    if (InetAddress.getByName(targetIP).isReachable(200)) {
                        // Then check if it's a GoSort device
                        apiClient.testConnection(targetIP, new GoSortApiClient.ApiCallback() {
                            @Override
                            public void onSuccess() {
                                mainHandler.post(() -> callback.onDeviceFound(targetIP));
                            }

                            @Override
                            public void onError(String message) {
                                // Not a GoSort device, ignore
                            }
                        });
                    }
                } catch (IOException e) {
                    // Skip unreachable hosts
                }

                int currentProgress = progress.incrementAndGet();
                mainHandler.post(() -> callback.onScanProgress((currentProgress * 100) / 254));

                if (activeThreads.decrementAndGet() == 0) {
                    mainHandler.post(callback::onScanComplete);
                }
            });
        }
    }

    private List<String> getLocalIPAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.isUp() && !intf.isLoopback()) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress() && addr.getHostAddress().contains(".")) {
                            addresses.add(addr.getHostAddress());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return addresses;
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
