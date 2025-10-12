package com.example.gosortapplication;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    private static final String PREFS = "notifications_prefs";
    private static final String KEY_ITEMS = "notifications_json";

    public interface Listener { void onUnreadCountChanged(int newCount); }

    private static NotificationRepository INSTANCE;
    private final List<NotificationItem> items = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private SharedPreferences prefs;

    private NotificationRepository() {}

    public static NotificationRepository get() {
        if (INSTANCE == null) INSTANCE = new NotificationRepository();
        return INSTANCE;
    }

    // Initialize repository with application context to enable persistence
    public void init(Context ctx) {
        if (prefs != null) return; // already initialized
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        loadFromPrefs();
    // If there are no persisted notifications, create a few sample items so the app has content
    if (items.isEmpty()) {
        items.add(new NotificationItem("Bin 'Bio' is FULL! Distance reading has been consistently between 0-10cm for the last 5 readings. Please empty immediately.",
            "© 2025-10-11 15:35:24 | Device: sorter | Bin: Bio | Fullness: 100%", true));
        items.add(new NotificationItem("Bin 'Recyclable' is FULL! Distance reading has been consistently between 0-10cm for the last 5 readings. Please empty immediately.",
            "© 2025-10-11 15:35:24 | Device: sorter | Bin: Recyclable | Fullness: 100%", true));
        items.add(new NotificationItem("Bin 'Extra' is FULL! Distance reading has been consistently between 0-10cm for the last 5 readings. Please empty immediately.",
            "© 2025-10-11 15:35:23 | Device: sorter | Bin: Extra | Fullness: 100%", true));
        saveToPrefs();
    }

    // Notify listeners so UI (MainActivity) can update badge immediately after init/load
    notifyListeners();
    }

    public void setInitial(List<NotificationItem> start) {
        items.clear();
        if (start != null) items.addAll(start);
        saveToPrefs();
        notifyListeners();
    }

    // Return the backing list so UI can observe/modify it through repository methods
    public List<NotificationItem> getAll() { return items; }

    public void markRead(int index) {
        if (index >= 0 && index < items.size()) {
            items.get(index).isRead = true;
            saveToPrefs();
            notifyListeners();
        }
    }

    public void markAllRead() {
        for (NotificationItem n : items) n.isRead = true;
        saveToPrefs();
        notifyListeners();
    }

    public int getUnreadCount() {
        int c = 0; for (NotificationItem n : items) if (!n.isRead) c++; return c;
    }

    public void addListener(Listener l) {
        if (l == null) return;
        if (!listeners.contains(l)) listeners.add(l);
        // Immediately notify the newly added listener with current unread count
        l.onUnreadCountChanged(getUnreadCount());
    }
    public void removeListener(Listener l) { listeners.remove(l); }

    private void notifyListeners() {
        int u = getUnreadCount();
        for (Listener l : new ArrayList<>(listeners)) {
            l.onUnreadCountChanged(u);
        }
    }

    private void saveToPrefs() {
        if (prefs == null) return;
        JSONArray arr = new JSONArray();
        for (NotificationItem n : items) {
            JSONObject o = new JSONObject();
            try {
                o.put("message", n.message);
                o.put("meta", n.meta);
                o.put("high", n.isHighPriority);
                o.put("read", n.isRead);
            } catch (JSONException e) {
                // ignore single item
            }
            arr.put(o);
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
    }

    private void loadFromPrefs() {
        if (prefs == null) return;
        String s = prefs.getString(KEY_ITEMS, null);
        if (s == null) return;
        try {
            JSONArray arr = new JSONArray(s);
            items.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    String msg = o.optString("message", "");
                    String meta = o.optString("meta", "");
                    boolean high = o.optBoolean("high", false);
                    boolean read = o.optBoolean("read", false);
                    NotificationItem ni = new NotificationItem(msg, meta, high);
                    ni.isRead = read;
                    items.add(ni);
                }
            }
        } catch (JSONException e) {
            // ignore parse errors
        }
    }
}
