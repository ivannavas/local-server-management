package io.github.ivannavas.local_server_management.repository;

import io.github.ivannavas.local_server_management.entity.HardwareStatusRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface HardwareStatusRepository extends JpaRepository<HardwareStatusRecord, Long> {
    List<HardwareStatusRecord> findByRecordedAtBetween(OffsetDateTime from, OffsetDateTime to);

    List<HardwareStatusRecord> findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(
            OffsetDateTime from, OffsetDateTime to);
}
