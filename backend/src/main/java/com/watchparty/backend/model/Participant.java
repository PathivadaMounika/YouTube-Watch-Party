package com.watchparty.backend.model;

/**
 * Represents a single user inside a Room.
 *
 * userId is generated server-side on create/join and handed back to the
 * client. The client must present this userId on every subsequent request
 * (and later, WebSocket connection) so the server can look up who is
 * acting and what role they hold. We never trust a role sent by the client.
 */
public class Participant {

    private final String userId;
    private String username;
    private Role role;

    // Will be populated once WebSocket support is added (Phase 2+).
    // Kept nullable here so REST-only join still works.
    private String sessionId;

    public Participant(String userId, String username, Role role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
