package com.riskmanager.notification;

public interface NotificationChannel {

    String getChannelName();

    void send(NotificationPayload payload);

    boolean isEnabled();
}
