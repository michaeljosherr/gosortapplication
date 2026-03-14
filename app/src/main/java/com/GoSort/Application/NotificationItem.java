package com.GoSort.Application;

public class NotificationItem {
    public String message;
    public String meta;
    public boolean isHighPriority;
    public boolean isRead;

    public NotificationItem(String message, String meta, boolean isHighPriority) {
        this.message = message;
        this.meta = meta;
        this.isHighPriority = isHighPriority;
        this.isRead = false;
    }
}
