package com.watchparty.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * The durable database record of a room. This is distinct from
 * {@link Room}, which is the live, in-memory object driving real-time
 * playback sync (participants, current playback position, etc.) - that
 * state is inherently ephemeral and lives only as long as the party does.
 *
 * RoomRecord is the persisted history/metadata: one row is written when a
 * room is created and updated when it ends, so rooms survive a server
 * restart as records even though the live session itself doesn't.
 */
@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = "roomId"))
public class RoomRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The human-facing 6-character room code (e.g. "AB3XQ9") - same value
    // used as the key in the in-memory Room registry.
    @Column(nullable = false, length = 6)
    private String roomId;

    @Column(nullable = false)
    private Long hostUserId;

    @Column(nullable = false, length = 30)
    private String hostUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoomStatus status = RoomStatus.ACTIVE;

    // Last video played in the room, kept for history even after it ends.
    @Column(length = 50)
    private String lastVideoId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant endedAt;

    protected RoomRecord() {
        // for JPA
    }

    public RoomRecord(String roomId, Long hostUserId, String hostUsername) {
        this.roomId = roomId;
        this.hostUserId = hostUserId;
        this.hostUsername = hostUsername;
    }

    public Long getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public Long getHostUserId() {
        return hostUserId;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getLastVideoId() {
        return lastVideoId;
    }

    public void setLastVideoId(String lastVideoId) {
        this.lastVideoId = lastVideoId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
