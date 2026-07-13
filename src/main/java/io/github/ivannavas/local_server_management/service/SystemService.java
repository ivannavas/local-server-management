package io.github.ivannavas.local_server_management.service;

import io.github.ivannavas.local_server_management.entity.HardwareStatusRecord;
import io.github.ivannavas.local_server_management.model.HardwareStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface SystemService {
    HardwareStatus getHardwareStatus();
    List<HardwareStatusRecord> getHardwareStatusHistory(OffsetDateTime from, OffsetDateTime to);
}
