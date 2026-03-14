package com.GoSort.Application;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class NotificationRepository {

    private static final String PREFS     = "notifications_prefs";
    private static final String KEY_ITEMS = "notifications_json";

    public interface Listener { void onUnreadCountChanged(int newCount); }

    private static NotificationRepository INSTANCE;

    // FIX #7: use synchronizedList to guard against concurrent access
    private final List<NotificationItem> items = Collections.synchronizedList(new ArrayList<>());
    private final List<Listener> listeners = new ArrayList<>();
    private SharedPreferences prefs;

    private NotificationRepository() {}

    public static NotificationRepository get() {
        if (INSTANCE == null) INSTANCE = new NotificationRepository();
        return INSTANCE;
    }

    // FIX #1: init() must be called before use (idempotent — safe to call multiple times)
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

    // ─── ID-based operations (FIX #4) ────────────────────────────────────────

    public void markReadById(String id) {
        synchronized (items) {
            for (NotificationItem n : items) {
                if (n.id.equals(id)) {
                    n.isRead = true;
                    break;
                }
            }
        }
        saveToPrefs();
        notifyListeners();
    }

    public void deleteById(String id) {
        synchronized (items) {
            for (int i = items.size() - 1; i >= 0; i--) {
                if (items.get(i).id.equals(id)) {
                    items.remove(i);
                    break;
                }
            }
        }
        saveToPrefs();
        notifyListeners();
    }

    // ─── Legacy index-based operations (kept for compatibility) ──────────────

    public void markRead(int index) {
        synchronized (items) {
            if (index >= 0 && index < items.size()) {
                items.get(index).isRead = true;
            }
        }
        saveToPrefs();
        notifyListeners();
    }

    public void markAllRead() {
        synchronized (items) {
            for (NotificationItem n : items) n.isRead = true;
        }
        saveToPrefs();
        notifyListeners();
    }

    /** Called by BinPollingService after manually adding an item to the list. */
    public void notifyAdded() {
        saveToPrefs();
        notifyListeners();
    }

    public void deleteNotification(int position) {
        synchronized (items) {
            if (position >= 0 && position < items.size()) {
                items.remove(position);
            }
        }
        saveToPrefs();
        notifyListeners();
    }

    // ─── Unread count ─────────────────────────────────────────────────────────

    public int getUnreadCount() {
        int c = 0;
        synchronized (items) {
            for (NotificationItem n : items) if (!n.isRead) c++;
        }
        return c;
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    public void addListener(Listener l) {
        if (l == null) return;
        if (!listeners.contains(l)) listeners.add(l);
        l.onUnreadCountChanged(getUnreadCount());
    }

    public void removeListener(Listener l) { listeners.remove(l); }

    /** Exposed so MainActivity can trigger a badge refresh after directly mutating the list. */
    public void notifyListenersPublic() { notifyListeners(); }

    private void notifyListeners() {
        int u = getUnreadCount();
        for (Listener l : new ArrayList<>(listeners)) l.onUnreadCountChanged(u);
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    private void saveToPrefs() {
        if (prefs == null) return;
        JSONArray arr = new JSONArray();
        synchronized (items) {
            for (NotificationItem n : items) {
                JSONObject o = new JSONObject();
                try {
                    o.put("id",       n.id != null ? n.id : UUID.randomUUID().toString());
                    o.put("message",  n.message);
                    o.put("meta",     n.meta);
                    o.put("high",     n.isHighPriority);
                    o.put("read",     n.isRead);
                    o.put("binName",  n.binName);
                    o.put("fullness", n.fullnessLevel);
                } catch (JSONException ignored) {}
                arr.put(o);
            }
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
                // Restore persisted ID, or generate one if missing (migration)
                String savedId = o.optString("id", "");
                ni.id = savedId.isEmpty() ? UUID.randomUUID().toString() : savedId;
                ni.isRead = o.optBoolean("read", false);
                items.add(ni);
            }
        } catch (JSONException ignored) {}
    }
}