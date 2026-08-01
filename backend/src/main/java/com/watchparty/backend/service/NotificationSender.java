package com.watchparty.backend.service;

import com.watchparty.backend.dto.WsNotification;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Sends a message to one specific WebSocket session rather than
 * broadcasting to the whole room - used for "your action was denied" and
 * "you were removed from this room" notices.
 *
 * Spring's convertAndSendToUser() normally keys off an authenticated
 * Principal's name. We don't have logins in this app, so we use the raw
 * sessionId as the "user" identifier instead - Spring supports this as a
 * documented fallback for anonymous WebSocket sessions.
 */
@Component
public class NotificationSender {

    private static final String NOTIFICATIONS_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationSender(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void send(String sessionId, WsNotification notification) {
        if (sessionId == null) {
            return; // not currently connected - nothing to notify
        }

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(
                sessionId,
                NOTIFICATIONS_QUEUE,
                notification,
                headerAccessor.getMessageHeaders()
        );
    }
}
