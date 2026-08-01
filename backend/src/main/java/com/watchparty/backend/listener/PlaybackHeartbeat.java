package com.watchparty.backend.listener;

import com.watchparty.backend.model.Room;
import com.watchparty.backend.service.RoomBroadcaster;
import com.watchparty.backend.service.RoomService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * `Room.currentTime` only changes on explicit play/pause/seek/change-video
 * events - it doesn't tick on its own. That's fine for clients that are
 * already playing (their local player just keeps going), but it means
 * anyone who fell behind right after joining - e.g. because their video
 * was still buffering when the room state first arrived - has no way to
 * catch back up until the next actual event, which might be a while.
 *
 * This periodically re-broadcasts the (live-adjusted, see
 * RoomResponse.computeLiveCurrentTime) state for every room that's
 * currently playing, so every connected client's existing drift-
 * correction logic (see YouTubePlayer.jsx) gets a chance to self-heal
 * every few seconds, without needing anyone to click anything.
 */
@Component
public class PlaybackHeartbeat {

    private static final long HEARTBEAT_INTERVAL_MS = 4000;

    private final RoomService roomService;
    private final RoomBroadcaster broadcaster;

    public PlaybackHeartbeat(RoomService roomService, RoomBroadcaster broadcaster) {
        this.roomService = roomService;
        this.broadcaster = broadcaster;
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    public void broadcastPlayingRooms() {
        for (Room room : roomService.getAllRooms()) {
            if (room.isPlaying()) {
                broadcaster.broadcastRoomState(room.getRoomId());
            }
        }
    }
}
