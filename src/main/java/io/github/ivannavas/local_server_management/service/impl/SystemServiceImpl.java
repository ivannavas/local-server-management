package io.github.ivannavas.local_server_management.service.impl;

import io.github.ivannavas.local_server_management.entity.HardwareStatusRecord;
import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.local_server_management.repository.HardwareStatusRepository;
import io.github.ivannavas.local_server_management.service.SystemService;
import io.github.ivannavas.local_server_management.tools.SystemTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SystemServiceImpl implements SystemService {

    @Autowired
    private SystemTools systemTools;

    @Autowired
    private HardwareStatusRepository hardwareStatusRepository;

    @Override
    public HardwareStatus getHardwareStatus() {
        return systemTools.getHardwareStatus();
    }

    @Override
    public List<HardwareStatusRecord> getHardwareStatusHistory(OffsetDateTime from, OffsetDateTime to) {
        return hardwareStatusRepository.findByRecordedAtBetween(from, to);
    }

    @Scheduled(fixedRate = 120_000)
    public void recordCpuTemperature() {
        double temperature = systemTools.getHardwareStatus().cpuTemperature();
        HardwareStatusRecord record = new HardwareStatusRecord(
                BigDecimal.valueOf(temperature),
                OffsetDateTime.now()
        );
        hardwareStatusRepository.save(record);
    }
}
