package com.watchparty.backend.dto;

/**
 * Payload the client sends to /app/room/{roomId}/join right after the
 * WebSocket connects. userId must be the one they were handed back from
 * the earlier REST create/join call - the server never trusts a role or
 * identity claim beyond looking up this id in the room's participant map.
 */
public class JoinWsRequest {

    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
