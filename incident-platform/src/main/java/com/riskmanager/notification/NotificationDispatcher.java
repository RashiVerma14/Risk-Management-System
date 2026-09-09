package com.riskmanager.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final List<NotificationChannel> channels;

    public void dispatch(NotificationPayload payload) {
        for (NotificationChannel channel : channels) {
            if (channel.isEnabled()) {
                try {
                    channel.send(payload);
                } catch (Exception ex) {
                    log.error("Error executing notification channel {}: {}", channel.getChannelName(), ex.getMessage(), ex);
                }
            }
        }
    }
}
