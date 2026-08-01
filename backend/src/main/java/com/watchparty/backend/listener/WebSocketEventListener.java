package com.watchparty.backend.listener;

import com.watchparty.backend.service.RoomBroadcaster;
import com.watchparty.backend.service.RoomService;
import com.watchparty.backend.service.WebSocketSessionRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Fires whenever a client's WebSocket connection drops - closed tab,
 * browser closed, network loss, or an explicit client.deactivate() call.
 * We use this as our signal that the participant has left the room.
 */
@Component
public class WebSocketEventListener {

    private final WebSocketSessionRegistry sessionRegistry;
    private final RoomService roomService;
    private final RoomBroadcaster broadcaster;

    public WebSocketEventListener(WebSocketSessionRegistry sessionRegistry,
                                   RoomService roomService,
                                   RoomBroadcaster broadcaster) {
        this.sessionRegistry = sessionRegistry;
        this.roomService = roomService;
        this.broadcaster = broadcaster;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        WebSocketSessionRegistry.SessionInfo info = sessionRegistry.get(sessionId);
        if (info == null) {
            // This session never completed a "join" (e.g. dropped before
            // sending it) - nothing to clean up.
            return;
        }

        roomService.leaveRoom(info.roomId(), info.userId());
        sessionRegistry.remove(sessionId);
        broadcaster.broadcastRoomState(info.roomId());
    }
}
