package com.watchparty.backend.dto;

/**
 * Used for both play and pause messages. Includes the sender's current
 * playback position so the server's state stays accurate the moment
 * someone hits play/pause, not just on the next seek.
 */
public class PlaybackWsRequest {

    private double time;

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }
}
