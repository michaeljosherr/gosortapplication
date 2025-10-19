package com.example.gosortapplication;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkScanner {
    private static final String TAG = "NetworkScanner";
    private static final int TIMEOUT_MS = 500; // Increased timeout for HTTP requests
    private ExecutorService executor;
    private boolean isScanning = false;

    public interface ScanCallback {
        void onDeviceFound(String ipAddress);
        void onScanProgress(int progress);
        void onScanComplete();
        void onError(String message);
    }

    public interface ServerValidationCallback {
        void onResult(boolean isValid);
    }

    private boolean isDefaultGateway(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return false;

            // Common default gateway patterns
            return parts[3].equals("1") ||     // 192.168.1.1
                   parts[3].equals("254") ||   // 192.168.1.254
                   (parts[2].equals("1") && parts[3].equals("1")) || // 192.168.1.1
                   (parts[2].equals("0") && parts[3].equals("1"));   // 192.168.0.1
        } catch (Exception e) {
            return false;
        }
    }

    public void scanNetwork(ScanCallback callback) {
        if (isScanning) {
            callback.onError("Scan already in progress");
            return;
        }

        executor = Executors.newFixedThreadPool(50);
        isScanning = true;

        new Thread(() -> {
            String localIP = getLocalIP();
            if (localIP == null || localIP.isEmpty()) {
                callback.onError("Could not determine local IP address");
                stopScan();
                return;
            }

            String subnet = localIP.substring(0, localIP.lastIndexOf(".") + 1);
            final int totalIPs = 254;
            AtomicInteger scannedIPs = new AtomicInteger(0);

            for (int i = 1; i <= 254; i++) {
                if (!isScanning) break;

                final String ip = subnet + i;
                final int currentIP = i;

                executor.execute(() -> {
                    if (!isDefaultGateway(ip) && isGoSortServer(ip)) {
                        callback.onDeviceFound(ip);
                    }
                    int progress = (int) ((currentIP / (float) totalIPs) * 100);
                    callback.onScanProgress(progress);

                    if (scannedIPs.incrementAndGet() == totalIPs) {
                        callback.onScanComplete();
                        stopScan();
                    }
                });
            }
        }).start();
    }

    public void isGoSortServer(String ip, ServerValidationCallback callback) {
        if (isDefaultGateway(ip)) {
            callback.onResult(false);
            return;
        }

        new Thread(() -> {
            boolean result = checkGoSortServer(ip);
            callback.onResult(result);
        }).start();
    }

    private boolean checkGoSortServer(String ip) {
        if (isDefaultGateway(ip)) return false;

        try {
            URL url = new URL("http://" + ip + "/GoSort_Web/gs_DB/trash_detected.php");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            
            // Check if it's a success response (200) or a "No trash type provided" error (400)
            if (responseCode == 200) {
                return true;
            } else if (responseCode == 400) {
                // Read the response to check for the specific error message
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()))) {
                    String response = reader.readLine();
                    return response != null && response.contains("No trash type provided");
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isGoSortServer(String ip) {
        return !isDefaultGateway(ip) && checkGoSortServer(ip);
    }

    public void stopScan() {
        isScanning = false;
        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    Log.w(TAG, "Executor did not terminate in the specified time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Executor termination interrupted", e);
            }
        }
    }

    private String getLocalIP() {
        try {
            // First try using DatagramSocket approach (no actual connection needed)
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("10.255.255.255"), 10002);
                String ip = socket.getLocalAddress().getHostAddress();
                if (ip != null && !ip.equals("0.0.0.0")) {
                    return ip;
                }
            } catch (Exception e) {
                Log.d(TAG, "DatagramSocket approach failed: " + e.getMessage());
            }

            // Fallback: iterate through network interfaces
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (addr != null) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null) {
                            // Filter for IPv4 addresses that are not loopback
                            boolean isIPv4 = sAddr.indexOf(':') < 0;
                            if (isIPv4 && !addr.isLoopbackAddress() && sAddr.startsWith("192.168.")) {
                                return sAddr;
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Error getting local IP: " + e.getMessage());
        }

        return null;
    }
}
