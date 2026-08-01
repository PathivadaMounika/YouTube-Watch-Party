package com.watchparty.backend.service;

import com.watchparty.backend.exception.RoomNotFoundException;
import com.watchparty.backend.model.Participant;
import com.watchparty.backend.model.Role;
import com.watchparty.backend.model.Room;
import com.watchparty.backend.model.RoomRecord;
import com.watchparty.backend.model.RoomStatus;
import com.watchparty.backend.repository.RoomRecordRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private static final String ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I confusion
    private static final int ROOM_CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();
    private final RoomRecordRepository roomRecordRepository;

    // The single source of truth for LIVE room state (participants,
    // playback position, etc). Lost on server restart by design - this is
    // a real-time session, not durable data. Durable metadata about each
    // room (id, status, host, last video) lives in RoomRecordRepository
    // instead - see RoomRecord.
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public RoomService(RoomRecordRepository roomRecordRepository) {
        this.roomRecordRepository = roomRecordRepository;
    }

    /**
     * Creates a new room. The creator automatically becomes Host.
     * hostAccountId/hostUsername identify the logged-in account creating
     * it - login is required to create a room (see RoomController).
     */
    public Room createRoom(Long hostAccountId, String hostUsername) {
        String roomId = generateUniqueRoomId();
        String hostUserId = UUID.randomUUID().toString();

        Room room = new Room(roomId, hostUserId);
        Participant host = new Participant(hostUserId, hostUsername, Role.HOST);
        room.addParticipant(host);

        rooms.put(roomId, room);
        roomRecordRepository.save(new RoomRecord(roomId, hostAccountId, hostUsername));
        return room;
    }

    /**
     * Adds a new participant (default role PARTICIPANT) to an existing room.
     * Returns the newly created Participant so the caller knows their
     * generated userId.
     */
    public Participant joinRoom(String roomId, String username) {
        Room room = getRoomOrThrow(roomId);

        String userId = UUID.randomUUID().toString();
        Participant participant = new Participant(userId, username, Role.PARTICIPANT);
        room.addParticipant(participant);

        return participant;
    }

    /**
     * Removes a participant from a room (called when their WebSocket
     * disconnects). If the room no longer exists (e.g. already cleaned up)
     * this is a no-op rather than an error - disconnects can race with
     * other cleanup.
     */
    public void leaveRoom(String roomId, String userId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return;
        }
        room.removeParticipant(userId);

        // Room emptied out - drop it so the registry doesn't grow forever,
        // and mark the persisted record ENDED.
        if (room.getParticipants().isEmpty()) {
            rooms.remove(roomId);
            markEnded(roomId);
        }
    }

    /**
     * Keeps the persisted room record's last-played video in sync so it's
     * visible in room history even after the live session ends.
     */
    public void recordVideoChange(String roomId, String videoId) {
        roomRecordRepository.findByRoomId(roomId).ifPresent(record -> {
            record.setLastVideoId(videoId);
            roomRecordRepository.save(record);
        });
    }

    private void markEnded(String roomId) {
        roomRecordRepository.findByRoomId(roomId).ifPresent(record -> {
            record.setStatus(RoomStatus.ENDED);
            record.setEndedAt(Instant.now());
            roomRecordRepository.save(record);
        });
    }

    /**
     * Non-throwing lookup - returns null if the room doesn't exist.
     * Used by places that need to handle "already gone" gracefully
     * (e.g. broadcasting after a leave might race with room cleanup).
     */
    public Room findRoom(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * All currently active rooms - used by the playback heartbeat to
     * decide which rooms need a periodic re-sync broadcast.
     */
    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public Room getRoomOrThrow(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new RoomNotFoundException(roomId);
        }
        return room;
    }

    /**
     * The most recently added participant is the one we just created via
     * createRoom/joinRoom. Since userId is generated inside this service,
     * callers pass it back explicitly instead of guessing - see overloads
     * below used by the controller.
     */
    public Participant getParticipantOrThrow(Room room, String userId) {
        Participant participant = room.getParticipant(userId);
        if (participant == null) {
            throw new RoomNotFoundException("participant not found in room: " + userId);
        }
        return participant;
    }

    private String generateUniqueRoomId() {
        String code;
        do {
            code = generateRoomCode();
        } while (rooms.containsKey(code));
        return code;
    }

    private String generateRoomCode() {
        StringBuilder sb = new StringBuilder(ROOM_CODE_LENGTH);
        for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
            sb.append(ROOM_CODE_ALPHABET.charAt(random.nextInt(ROOM_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
