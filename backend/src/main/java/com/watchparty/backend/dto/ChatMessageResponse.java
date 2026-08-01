package com.watchparty.backend.dto;

import java.time.Instant;

public class ChatMessageResponse {

    public enum Type {
        CHAT,
        REACTION
    }

    private Type type;
    private String userId;
    private String username;
    private String message;
    private String timestamp;

    public ChatMessageResponse(Type type, String userId, String username, String message) {
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    public Type getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
