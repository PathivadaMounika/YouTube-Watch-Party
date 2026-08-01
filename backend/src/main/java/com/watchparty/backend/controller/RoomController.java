package com.watchparty.backend.controller;

import com.watchparty.backend.dto.JoinResultResponse;
import com.watchparty.backend.dto.ParticipantDto;
import com.watchparty.backend.dto.RoomResponse;
import com.watchparty.backend.exception.UnauthorizedException;
import com.watchparty.backend.model.Participant;
import com.watchparty.backend.model.Room;
import com.watchparty.backend.security.JwtService;
import com.watchparty.backend.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Creates a new room. Caller becomes Host. Requires a logged-in
     * account - SecurityConfig rejects unauthenticated requests before
     * this method even runs, but we double-check here too since the
     * principal is what supplies the host's identity.
     */
    @PostMapping
    public ResponseEntity<JoinResultResponse> createRoom() {
        JwtService.AuthPrincipal me = requireLogin();

        Room room = roomService.createRoom(me.userId(), me.username());
        Participant host = room.getParticipant(room.getHostId());

        JoinResultResponse response = new JoinResultResponse(
                RoomResponse.from(room),
                ParticipantDto.from(host)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetches current room state - used by the join page to validate a
     * room code exists before attempting to join, and by the room page to
     * (re)load state on refresh. Requires login, same as every other
     * room endpoint now.
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String roomId) {
        requireLogin();
        Room room = roomService.getRoomOrThrow(roomId);
        return ResponseEntity.ok(RoomResponse.from(room));
    }

    /**
     * Joins an existing room. Caller becomes a Participant (default role).
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<JoinResultResponse> joinRoom(@PathVariable String roomId) {
        JwtService.AuthPrincipal me = requireLogin();

        Participant participant = roomService.joinRoom(roomId, me.username());
        Room room = roomService.getRoomOrThrow(roomId);

        JoinResultResponse response = new JoinResultResponse(
                RoomResponse.from(room),
                ParticipantDto.from(participant)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Pulls the authenticated principal that JwtAuthFilter attached to
     * this request. SecurityConfig already blocks unauthenticated calls
     * to /api/rooms/**, so reaching here with no principal would mean a
     * config bug rather than a normal user error - but we fail closed
     * with a clear 401 either way instead of trusting a missing identity.
     */
    private JwtService.AuthPrincipal requireLogin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtService.AuthPrincipal principal)) {
            throw new UnauthorizedException("Please log in to create or join a room.");
        }
        return principal;
    }
}
