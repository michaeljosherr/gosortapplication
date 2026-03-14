package com.GoSort.Application;

import java.util.UUID;

public class NotificationItem {
    public String id;           // unique ID to avoid stale-position bugs
    public String message;
    public String meta;
    public boolean isHighPriority;
    public boolean isRead;
    public String binName;      // e.g. "Bio", "Non-Bio", "Mixed", "Hazardous"
    public int fullnessLevel;   // 0-100, or -1 for malfunction

    public NotificationItem(String message, String meta, boolean isHighPriority) {
        this.id = UUID.randomUUID().toString();
        this.message = message;
        this.meta = meta;
        this.isHighPriority = isHighPriority;
        this.isRead = false;
        this.binName = "";
        this.fullnessLevel = 0;
    }

    public NotificationItem(String message, String meta, boolean isHighPriority,
                            String binName, int fullnessLevel) {
        this.id = UUID.randomUUID().toString();
        this.message = message;
        this.meta = meta;
        this.isHighPriority = isHighPriority;
        this.isRead = false;
        this.binName = binName;
        this.fullnessLevel = fullnessLevel;
    }
}