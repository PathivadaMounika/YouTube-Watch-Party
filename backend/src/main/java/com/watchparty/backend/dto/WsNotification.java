package com.watchparty.backend.dto;

public class WsNotification {

    public enum Type {
        ACTION_DENIED,
        REMOVED_FROM_ROOM
    }

    private Type type;
    private String message;

    public WsNotification() {
    }

    public WsNotification(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
