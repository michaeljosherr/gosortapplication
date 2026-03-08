package com.example.gosortapplication;

import org.json.JSONException;
import org.json.JSONObject;

public class ActivityLog {
    private String deviceName;
    private String binType;
    private String message;
    private long timestamp;
    private int iconResId;

    public ActivityLog(String deviceName, String binType, String message, long timestamp, int iconResId) {
        this.deviceName = deviceName;
        this.binType = binType;
        this.message = message;
        this.timestamp = timestamp;
        this.iconResId = iconResId;
    }

    // Constructor for JSON deserialization
    public ActivityLog(JSONObject jsonObject) throws JSONException {
        this.deviceName = jsonObject.getString("deviceName");
        this.binType = jsonObject.getString("binType");
        this.message = jsonObject.getString("message");
        this.timestamp = jsonObject.getLong("timestamp");
        this.iconResId = jsonObject.getInt("iconResId");
    }

    // Convert to JSON for storage
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("deviceName", deviceName);
        jsonObject.put("binType", binType);
        jsonObject.put("message", message);
        jsonObject.put("timestamp", timestamp);
        jsonObject.put("iconResId", iconResId);
        return jsonObject;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getBinType() {
        return binType;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTimeAgo() {
        long currentTime = System.currentTimeMillis();
        long diffMs = currentTime - timestamp;
        long diffSeconds = diffMs / 1000;
        long diffMinutes = diffSeconds / 60;
        long diffHours = diffMinutes / 60;
        long diffDays = diffHours / 24;

        if (diffSeconds < 60) {
            return "just now";
        } else if (diffMinutes < 60) {
            return diffMinutes + " minute" + (diffMinutes > 1 ? "s" : "") + " ago";
        } else if (diffHours < 24) {
            return diffHours + " hour" + (diffHours > 1 ? "s" : "") + " ago";
        } else {
            return diffDays + " day" + (diffDays > 1 ? "s" : "") + " ago";
        }
    }
}
