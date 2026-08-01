package com.watchparty.backend.dto;

/**
 * Used for both chat messages and reactions - a reaction is just a
 * message whose content happens to be an emoji rather than free text.
 * See ChatWsController for how the two are distinguished on the way out.
 */
public class ChatMessageWsRequest {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
