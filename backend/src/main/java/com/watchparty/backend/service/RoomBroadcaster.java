package com.watchparty.backend.service;

import com.watchparty.backend.dto.ChatMessageResponse;
import com.watchparty.backend.dto.RoomResponse;
import com.watchparty.backend.model.Room;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoomBroadcaster {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomBroadcaster(RoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Pushes the current full room state to everyone subscribed to
     * /topic/room/{roomId}. If the room no longer exists (e.g. it just
     * emptied out and got cleaned up), this is a no-op.
     */
    public void broadcastRoomState(String roomId) {
        Room room = roomService.findRoom(roomId);
        if (room == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/room/" + roomId, RoomResponse.from(room));
    }

    /**
     * Chat messages and reactions go out on their own topic, separate
     * from room/playback state - keeps the two concerns from getting
     * mixed into one payload, and lets the frontend subscribe to them
     * independently.
     */
    public void broadcastChatMessage(String roomId, ChatMessageResponse message) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/chat", message);
    }
}

