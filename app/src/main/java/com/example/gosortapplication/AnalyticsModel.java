package com.example.gosortapplication;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsModel {
    private static final AnalyticsModel instance = new AnalyticsModel();

    public interface Listener {
        void onDataChanged(int[] values);
    }

    private final List<Listener> listeners = new ArrayList<>();

    // order: biodegradable, nonBiodeg, mixed, unidentified, hazardous
    private final int[] values = new int[]{70, 45, 85, 30, 100};

    private AnalyticsModel() {}

    public static AnalyticsModel get() {
        return instance;
    }

    public synchronized void setValues(int[] vals) {
        if (vals == null) return;
        int n = Math.min(vals.length, values.length);
        for (int i = 0; i < n; i++) values[i] = vals[i];
        // notify
        int[] copy = values.clone();
        for (Listener l : listeners) {
            l.onDataChanged(copy);
        }
    }

    public synchronized int[] getValues() {
        return values.clone();
    }

    public synchronized void addListener(Listener l) {
        if (l == null) return;
        listeners.add(l);
    }

    public synchronized void removeListener(Listener l) {
        listeners.remove(l);
    }
}
