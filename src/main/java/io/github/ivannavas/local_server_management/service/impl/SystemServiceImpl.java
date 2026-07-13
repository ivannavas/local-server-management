package io.github.ivannavas.local_server_management.service.impl;

import io.github.ivannavas.local_server_management.entity.HardwareStatusRecord;
import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.local_server_management.repository.HardwareStatusRepository;
import io.github.ivannavas.local_server_management.service.NtfyService;
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

    private static final double BASE_THRESHOLD = 65.0;
    private static final double THRESHOLD_STEP = 10.0;

    @Autowired
    private SystemTools systemTools;

    @Autowired
    private HardwareStatusRepository hardwareStatusRepository;

    @Autowired
    private NtfyService ntfyService;

    private int lastNotifiedLevel = 0;

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

        notifyOnTemperatureChange(temperature);
    }

    private void notifyOnTemperatureChange(double temperature) {
        int currentLevel = alertLevel(temperature);

        if (currentLevel > lastNotifiedLevel) {
            double crossedThreshold = thresholdForLevel(currentLevel);
            ntfyService.send(
                    "CPU temperature high",
                    String.format("La temperatura de la CPU ha superado los %.0f°C (actual: %.1f°C)",
                            crossedThreshold, temperature),
                    "warning,fire"
            );
        } else if (currentLevel < lastNotifiedLevel) {
            String message = currentLevel == 0
                    ? String.format("La temperatura de la CPU ha vuelto a la normalidad (actual: %.1f°C)", temperature)
                    : String.format("La temperatura de la CPU ha bajado por debajo de los %.0f°C (actual: %.1f°C)",
                            thresholdForLevel(lastNotifiedLevel), temperature);
            ntfyService.send("CPU temperature dropping", message, "white_check_mark");
        }

        lastNotifiedLevel = currentLevel;
    }

    private int alertLevel(double temperature) {
        if (temperature < BASE_THRESHOLD) {
            return 0;
        }
        return (int) Math.floor((temperature - BASE_THRESHOLD) / THRESHOLD_STEP) + 1;
    }

    private double thresholdForLevel(int level) {
        return BASE_THRESHOLD + (level - 1) * THRESHOLD_STEP;
    }
}
