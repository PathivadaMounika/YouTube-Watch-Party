package com.watchparty.backend.repository;

import com.watchparty.backend.model.RoomRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRecordRepository extends JpaRepository<RoomRecord, Long> {

    Optional<RoomRecord> findByRoomId(String roomId);
}
