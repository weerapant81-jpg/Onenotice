package com.allin1.oneinbox;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class MessageListener extends NotificationListenerService {
    static MessageListener instance;

    @Override public void onListenerConnected() {
        instance = this;
        MainActivity.refreshVisible();
    }

    @Override public void onListenerDisconnected() {
        instance = null;
        MainActivity.refreshVisible();
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) { MainActivity.refreshVisible(); }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) { MainActivity.refreshVisible(); }
}
