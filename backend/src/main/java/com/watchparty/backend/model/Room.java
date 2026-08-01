package com.watchparty.backend.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory representation of a watch party room.
 *
 * Thread-safety note: rooms will be read/written from multiple HTTP
 * threads today, and from multiple WebSocket session threads once Phase 2
 * lands. We use a ConcurrentHashMap for participants and keep mutation of
 * playback state simple enough to stay safe without extra locking for now.
 */
public class Room {

    private final String roomId;
    private String hostId;

    private final Map<String, Participant> participants = new ConcurrentHashMap<>();

    // Playback state - defaults until a video is chosen.
    private String videoId;
    private boolean playing = false;
    private double currentTime = 0.0;

    // Real-world timestamp (server clock) of the last time playback
    // state changed. Needed because `currentTime` is only a snapshot -
    // it doesn't advance on its own between events. See
    // touchPlaybackTimestamp() and RoomResponse.from() for how this is
    // used to compute a live-adjusted position for anyone who fetches
    // room state well after the last play/pause/seek/change-video event.
    private long lastUpdatedAtMillis = System.currentTimeMillis();

    public Room(String roomId, String hostId) {
        this.roomId = roomId;
        this.hostId = hostId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public Map<String, Participant> getParticipants() {
        return participants;
    }

    public void addParticipant(Participant participant) {
        participants.put(participant.getUserId(), participant);
    }

    public void removeParticipant(String userId) {
        participants.remove(userId);
    }

    public Participant getParticipant(String userId) {
        return participants.get(userId);
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(double currentTime) {
        this.currentTime = currentTime;
    }

    public long getLastUpdatedAtMillis() {
        return lastUpdatedAtMillis;
    }

    /**
     * Call this any time playing/currentTime/videoId changes as a result
     * of a play, pause, seek, or change-video message. Marks "now" as
     * the moment this snapshot is accurate as-of.
     */
    public void touchPlaybackTimestamp() {
        this.lastUpdatedAtMillis = System.currentTimeMillis();
    }
}
