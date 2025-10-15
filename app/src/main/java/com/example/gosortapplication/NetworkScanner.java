package com.example.gosortapplication;

import android.os.Handler;
import android.os.Looper;
import java.net.InetSocketAddress;
import java.net.Socket;
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
        this.executor = Executors.newFixedThreadPool(50);  // Same as Python's max_workers
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.apiClient = new GoSortApiClient();
    }

    public void scanNetwork(ScanCallback callback) {
        // Get local IP - similar to Python's socket.getsockname()
        String localIP = getLocalIP();
        if (localIP == null) {
            mainHandler.post(() -> callback.onError("Could not determine local IP"));
            return;
        }

        String subnet = localIP.substring(0, localIP.lastIndexOf(".") + 1);
        AtomicInteger progress = new AtomicInteger(0);
        AtomicInteger activeThreads = new AtomicInteger(0);

        for (int i = 1; i < 255; i++) {
            String targetIP = subnet + i;
            activeThreads.incrementAndGet();

            executor.execute(() -> {
                try {
                    // Directly test each IP - match Python's behavior exactly
                    apiClient.testConnection(targetIP, new GoSortApiClient.ApiCallback() {
                        @Override
                        public void onSuccess() {
                            mainHandler.post(() -> callback.onDeviceFound(targetIP));
                        }

                        @Override
                        public void onError(String message) {
                            // Silently ignore non-GoSort servers like Python does
                        }
                    });
                } catch (Exception e) {
                    // Silently ignore errors like Python does
                }

                int currentProgress = progress.incrementAndGet();
                mainHandler.post(() -> callback.onScanProgress((currentProgress * 100) / 254));

                if (activeThreads.decrementAndGet() == 0) {
                    mainHandler.post(callback::onScanComplete);
                }
            });
        }
    }

    private String getLocalIP() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 1000);
            String localIP = socket.getLocalAddress().getHostAddress();
            socket.close();
            return localIP;
        } catch (Exception e) {
            return null;
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}