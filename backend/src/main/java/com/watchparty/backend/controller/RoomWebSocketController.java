package com.watchparty.backend.controller;

import com.watchparty.backend.dto.AssignRoleWsRequest;
import com.watchparty.backend.dto.ChangeVideoWsRequest;
import com.watchparty.backend.dto.ChatMessageResponse;
import com.watchparty.backend.dto.ChatMessageWsRequest;
import com.watchparty.backend.dto.JoinWsRequest;
import com.watchparty.backend.dto.PlaybackWsRequest;
import com.watchparty.backend.dto.RemoveParticipantWsRequest;
import com.watchparty.backend.dto.SeekWsRequest;
import com.watchparty.backend.dto.TransferHostWsRequest;
import com.watchparty.backend.dto.WsNotification;
import com.watchparty.backend.model.Participant;
import com.watchparty.backend.model.Role;
import com.watchparty.backend.model.Room;
import com.watchparty.backend.service.NotificationSender;
import com.watchparty.backend.service.RoleService;
import com.watchparty.backend.service.RoomBroadcaster;
import com.watchparty.backend.service.RoomService;
import com.watchparty.backend.service.WebSocketSessionRegistry;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class RoomWebSocketController {

    private final RoomService roomService;
    private final WebSocketSessionRegistry sessionRegistry;
    private final RoomBroadcaster broadcaster;
    private final RoleService roleService;
    private final NotificationSender notificationSender;

    public RoomWebSocketController(RoomService roomService,
                                    WebSocketSessionRegistry sessionRegistry,
                                    RoomBroadcaster broadcaster,
                                    RoleService roleService,
                                    NotificationSender notificationSender) {
        this.roomService = roomService;
        this.sessionRegistry = sessionRegistry;
        this.broadcaster = broadcaster;
        this.roleService = roleService;
        this.notificationSender = notificationSender;
    }

    /**
     * Client sends this right after the WebSocket connects, carrying the
     * userId it was given by the earlier REST create/join call. We look
     * that participant up (it must already exist in the room - joining
     * the room itself still happens over REST), tie this WS session to
     * them, then broadcast the current room state so everyone's list
     * updates live.
     */
    @MessageMapping("/room/{roomId}/join")
    public void handleJoin(@DestinationVariable String roomId,
                            JoinWsRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {

        Room room = roomService.getRoomOrThrow(roomId);
        Participant participant = roomService.getParticipantOrThrow(room, request.getUserId());

        String sessionId = headerAccessor.getSessionId();
        sessionRegistry.register(sessionId, roomId, participant.getUserId());

        broadcaster.broadcastRoomState(roomId);
    }

    @MessageMapping("/room/{roomId}/play")
    public void handlePlay(@DestinationVariable String roomId,
                            PlaybackWsRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomOrThrow(roomId);
        if (!requirePlaybackPermission(room, headerAccessor)) return;

        room.setPlaying(true);
        room.setCurrentTime(request.getTime());
        room.touchPlaybackTimestamp();
        broadcaster.broadcastRoomState(roomId);
    }

    @MessageMapping("/room/{roomId}/pause")
    public void handlePause(@DestinationVariable String roomId,
                             PlaybackWsRequest request,
                             SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomOrThrow(roomId);
        if (!requirePlaybackPermission(room, headerAccessor)) return;

        room.setPlaying(false);
        room.setCurrentTime(request.getTime());
        room.touchPlaybackTimestamp();
        broadcaster.broadcastRoomState(roomId);
    }

    @MessageMapping("/room/{roomId}/seek")
    public void handleSeek(@DestinationVariable String roomId,
                            SeekWsRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomOrThrow(roomId);
        if (!requirePlaybackPermission(room, headerAccessor)) return;

        room.setCurrentTime(request.getTime());
        room.touchPlaybackTimestamp();
        broadcaster.broadcastRoomState(roomId);
    }

    @MessageMapping("/room/{roomId}/change-video")
    public void handleChangeVideo(@DestinationVariable String roomId,
                                   ChangeVideoWsRequest request,
                                   SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomOrThrow(roomId);
        if (!requirePlaybackPermission(room, headerAccessor)) return;

        room.setVideoId(request.getVideoId());
        room.setCurrentTime(0.0);
        room.setPlaying(true);
        room.touchPlaybackTimestamp();
        roomService.recordVideoChange(roomId, request.getVideoId());
        broadcaster.broadcastRoomState(roomId);
    }

    /**
     * Host assigns a role (Moderator or Participant) to someone else in
     * the room. Host-only. We don't allow assigning HOST here - there
     * can only be one Host at a time, and swapping it is the more
     * deliberate transfer-host action below instead.
     */
    @MessageMapping("/room/{roomId}/assign-role")
    public void handleAssignRole(@DestinationVariable String roomId,
                                  AssignRoleWsRequest request,
                                  SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomOrThrow(roomId);
        Participant sender = requireSender(room, headerAccessor);
        if (sender == null) return;

        if (!roleService.isHost(sender.getRole())) {
            denyAction(headerAccessor, "Only the Host can assign roles.");
            return;
        }

        if (request.getRole() == Role.HOST) {
            denyAction(headerAccessor, "Host cannot be assigned this way.");
            return;
        }

        Participant target = room.getParticipant(request.getUserId());
        if (target == null) {
            return; // already left - nothing to do
        }

        target.setRole(request.getRole());
        broadcaster.broadcastRoomState(roomId);
    }

    /**
     * Host removes a participant from the room. Host-only. The removed
     * participant gets a private notice so their frontend can disconnect
     * and navigate them home; everyone else sees the updated list.
     */
    @MessageMapping("/room/{roomId}/remove-participant")
    public void handleRemoveParticipant(@DestinationVariable String roomId,
                                         RemoveParticipantWsRequest request,
                                         SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomOrThrow(roomId);
        Participant sender = requireSender(room, headerAccessor);
        if (sender == null) return;

        if (!roleService.isHost(sender.getRole())) {
            denyAction(headerAccessor, "Only the Host can remove participants.");
            return;
        }

        if (request.getUserId().equals(sender.getUserId())) {
            denyAction(headerAccessor, "You can't remove yourself.");
            return;
        }

        String targetSessionId = sessionRegistry.findSessionId(roomId, request.getUserId());
        roomService.leaveRoom(roomId, request.getUserId());

        notificationSender.send(targetSessionId,
                new WsNotification(WsNotification.Type.REMOVED_FROM_ROOM, "You were removed from the room."));

        broadcaster.broadcastRoomState(roomId);
    }

    /**
     * Host hands the Host role to another participant. The old Host
     * becomes a Moderator (a softer landing than dropping straight to
     * Participant) rather than being left without any special role.
     */
    @MessageMapping("/room/{roomId}/transfer-host")
    public void handleTransferHost(@DestinationVariable String roomId,
                                    TransferHostWsRequest request,
                                    SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomOrThrow(roomId);
        Participant sender = requireSender(room, headerAccessor);
        if (sender == null) return;

        if (!roleService.isHost(sender.getRole())) {
            denyAction(headerAccessor, "Only the Host can transfer host.");
            return;
        }

        if (request.getUserId().equals(sender.getUserId())) {
            return; // already host - nothing to do
        }

        Participant target = room.getParticipant(request.getUserId());
        if (target == null) {
            return; // they already left - nothing to do
        }

        target.setRole(Role.HOST);
        sender.setRole(Role.MODERATOR);
        room.setHostId(target.getUserId());

        broadcaster.broadcastRoomState(roomId);
    }

    /**
     * Chat message - anyone in the room can send one, no role check.
     * Broadcast on a separate topic from room/playback state (see
     * RoomBroadcaster.broadcastChatMessage) so the two stay independent.
     */
    @MessageMapping("/room/{roomId}/chat")
    public void handleChat(@DestinationVariable String roomId,
                            ChatMessageWsRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomOrThrow(roomId);
        Participant sender = requireSender(room, headerAccessor);
        if (sender == null) return;

        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isEmpty() || message.length() > 500) {
            return; // ignore empty/oversized messages rather than erroring
        }

        broadcaster.broadcastChatMessage(roomId, new ChatMessageResponse(
                ChatMessageResponse.Type.CHAT, sender.getUserId(), sender.getUsername(), message));
    }

    /**
     * Reaction - same shape as a chat message, but the "message" is
     * expected to be a single emoji and the frontend renders it
     * differently (a floating burst over the player instead of a chat
     * bubble). We don't validate it's actually an emoji server-side -
     * that's a rendering concern, not a correctness one.
     */
    @MessageMapping("/room/{roomId}/reaction")
    public void handleReaction(@DestinationVariable String roomId,
                                ChatMessageWsRequest request,
                                SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomOrThrow(roomId);
        Participant sender = requireSender(room, headerAccessor);
        if (sender == null) return;

        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isEmpty() || message.length() > 8) {
            return;
        }

        broadcaster.broadcastChatMessage(roomId, new ChatMessageResponse(
                ChatMessageResponse.Type.REACTION, sender.getUserId(), sender.getUsername(), message));
    }

    /**
     * Looks up which participant this WebSocket session belongs to (set
     * during the earlier "join" message). Returns null if unknown, which
     * shouldn't normally happen but is handled defensively.
     */
    private Participant requireSender(Room room, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        WebSocketSessionRegistry.SessionInfo info = sessionRegistry.get(sessionId);
        if (info == null) {
            return null;
        }
        return room.getParticipant(info.userId());
    }

    /**
     * Shared guard for the four playback actions: looks up the sender
     * and checks Host/Moderator permission. Sends a private denial
     * notice (rather than broadcasting anything) if not allowed.
     */
    private boolean requirePlaybackPermission(Room room, SimpMessageHeaderAccessor headerAccessor) {
        Participant sender = requireSender(room, headerAccessor);
        if (sender == null) {
            return false;
        }
        if (!roleService.canControlPlayback(sender.getRole())) {
            denyAction(headerAccessor, "Only the Host or a Moderator can control playback.");
            return false;
        }
        return true;
    }

    private void denyAction(SimpMessageHeaderAccessor headerAccessor, String message) {
        notificationSender.send(headerAccessor.getSessionId(),
                new WsNotification(WsNotification.Type.ACTION_DENIED, message));
    }
}
