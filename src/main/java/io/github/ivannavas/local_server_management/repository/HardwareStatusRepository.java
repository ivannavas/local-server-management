package io.github.ivannavas.local_server_management.repository;

import io.github.ivannavas.local_server_management.entity.HardwareStatusRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface HardwareStatusRepository extends JpaRepository<HardwareStatusRecord, Long> {
    List<HardwareStatusRecord> findByRecordedAtBetween(OffsetDateTime from, OffsetDateTime to);

    List<HardwareStatusRecord> findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(
            OffsetDateTime from, OffsetDateTime to);

    Optional<HardwareStatusRecord> findTopByOrderByRecordedAtDesc();
}
