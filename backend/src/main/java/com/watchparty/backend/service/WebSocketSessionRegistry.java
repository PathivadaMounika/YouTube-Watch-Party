package com.watchparty.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which (roomId, userId) a given WebSocket session belongs to.
 *
 * Needed because the disconnect event only gives us a sessionId - we need
 * this to know which room to broadcast a "user_left" update to, and which
 * participant to remove.
 */
@Service
public class WebSocketSessionRegistry {

    public record SessionInfo(String roomId, String userId) {
    }

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public void register(String sessionId, String roomId, String userId) {
        sessions.put(sessionId, new SessionInfo(roomId, userId));
    }

    public SessionInfo get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Finds the sessionId currently associated with a given participant
     * in a room - used when the Host removes them and we need to send a
     * private "you were removed" notice to that specific session.
     * Returns null if they're not currently connected over WebSocket.
     */
    public String findSessionId(String roomId, String userId) {
        return sessions.entrySet().stream()
                .filter(e -> e.getValue().roomId().equals(roomId) && e.getValue().userId().equals(userId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
