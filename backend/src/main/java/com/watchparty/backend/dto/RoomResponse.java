package com.watchparty.backend.dto;

import com.watchparty.backend.model.Room;

import java.util.List;
import java.util.stream.Collectors;

public class RoomResponse {

    private String roomId;
    private String hostId;
    private String videoId;
    private boolean playing;
    private double currentTime;
    private List<ParticipantDto> participants;

    public static RoomResponse from(Room room) {
        RoomResponse dto = new RoomResponse();
        dto.roomId = room.getRoomId();
        dto.hostId = room.getHostId();
        dto.videoId = room.getVideoId();
        dto.playing = room.isPlaying();
        dto.currentTime = computeLiveCurrentTime(room);
        dto.participants = room.getParticipants().values().stream()
                .map(ParticipantDto::from)
                .collect(Collectors.toList());
        return dto;
    }

    /**
     * `Room.currentTime` is only a snapshot from the last play/pause/seek/
     * change-video event - it doesn't advance on its own between events.
     * If we're playing, add however many real seconds have passed since
     * that snapshot was taken, so anyone who fetches state well after the
     * last event (a new joiner, or a page refresh) lands at the actual
     * live position instead of wherever things happened to be last time
     * someone touched a control.
     */
    private static double computeLiveCurrentTime(Room room) {
        if (!room.isPlaying()) {
            return room.getCurrentTime();
        }
        double elapsedSeconds = (System.currentTimeMillis() - room.getLastUpdatedAtMillis()) / 1000.0;
        return room.getCurrentTime() + Math.max(elapsedSeconds, 0);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getVideoId() {
        return videoId;
    }

    public boolean isPlaying() {
        return playing;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public List<ParticipantDto> getParticipants() {
        return participants;
    }
}
