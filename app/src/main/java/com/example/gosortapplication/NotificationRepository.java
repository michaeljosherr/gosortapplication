package com.example.gosortapplication;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    private static final String PREFS   = "notifications_prefs";
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

    public void init(Context ctx) {
        if (prefs != null) return;
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        loadFromPrefs();
        notifyListeners();
    }

    public void setInitial(List<NotificationItem> start) {
        items.clear();
        if (start != null) items.addAll(start);
        saveToPrefs();
        notifyListeners();
    }

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
        int c = 0;
        for (NotificationItem n : items) if (!n.isRead) c++;
        return c;
    }

    public void addListener(Listener l) {
        if (l == null) return;
        if (!listeners.contains(l)) listeners.add(l);
        l.onUnreadCountChanged(getUnreadCount());
    }

    public void removeListener(Listener l) { listeners.remove(l); }

    private void notifyListeners() {
        int u = getUnreadCount();
        for (Listener l : new ArrayList<>(listeners)) l.onUnreadCountChanged(u);
    }

    public void deleteNotification(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            saveToPrefs();
            notifyListeners();
        }
    }

    private void saveToPrefs() {
        if (prefs == null) return;
        JSONArray arr = new JSONArray();
        for (NotificationItem n : items) {
            JSONObject o = new JSONObject();
            try {
                o.put("message",  n.message);
                o.put("meta",     n.meta);
                o.put("high",     n.isHighPriority);
                o.put("read",     n.isRead);
                o.put("binName",  n.binName);
                o.put("fullness", n.fullnessLevel);
            } catch (JSONException ignored) {}
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
                if (o == null) continue;
                NotificationItem ni = new NotificationItem(
                        o.optString("message", ""),
                        o.optString("meta",    ""),
                        o.optBoolean("high",   false),
                        o.optString("binName", ""),
                        o.optInt("fullness",   0)
                );
                ni.isRead = o.optBoolean("read", false);
                items.add(ni);
            }
        } catch (JSONException ignored) {}
    }
}