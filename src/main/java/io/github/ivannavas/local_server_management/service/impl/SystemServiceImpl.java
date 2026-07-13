package io.github.ivannavas.local_server_management.service.impl;

import io.github.ivannavas.local_server_management.entity.HardwareStatusRecord;
import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.local_server_management.repository.HardwareStatusRepository;
import io.github.ivannavas.local_server_management.service.NtfyService;
import io.github.ivannavas.local_server_management.service.SystemService;
import io.github.ivannavas.local_server_management.tools.SystemTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SystemServiceImpl implements SystemService {

    private static final double BASE_THRESHOLD = 60.0;
    private static final double THRESHOLD_STEP = 10.0;

    private static final BigDecimal AGGREGATION_TOLERANCE = BigDecimal.valueOf(3.0);

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

    @Transactional
    @Scheduled(cron = "0 0 19 * * *")
    @EventListener(ApplicationReadyEvent.class)
    public void aggregatePreviousDay() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate day = LocalDate.now(zone).minusDays(1);
        OffsetDateTime from = day.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = day.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        List<HardwareStatusRecord> records =
                hardwareStatusRepository.findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(from, to);
        if (records.size() < 2) {
            return;
        }

        int removed = 0;
        List<HardwareStatusRecord> group = new ArrayList<>();
        BigDecimal groupMin = null;
        BigDecimal groupMax = null;

        for (HardwareStatusRecord record : records) {
            BigDecimal temp = record.getCpuTemperature();
            BigDecimal newMin = group.isEmpty() ? temp : groupMin.min(temp);
            BigDecimal newMax = group.isEmpty() ? temp : groupMax.max(temp);

            if (group.isEmpty() || newMax.subtract(newMin).compareTo(AGGREGATION_TOLERANCE) <= 0) {
                group.add(record);
                groupMin = newMin;
                groupMax = newMax;
            } else {
                removed += collapseGroup(group);
                group = new ArrayList<>(List.of(record));
                groupMin = temp;
                groupMax = temp;
            }
        }
        removed += collapseGroup(group);

        if (removed > 0) {
            log.info("Aggregated hardware status records for {}: removed {} redundant rows", day, removed);
        }
    }

    private int collapseGroup(List<HardwareStatusRecord> group) {
        if (group.size() < 2) {
            return 0;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (HardwareStatusRecord record : group) {
            sum = sum.add(record.getCpuTemperature());
        }
        BigDecimal average = sum.divide(BigDecimal.valueOf(group.size()), 2, RoundingMode.HALF_UP);
        OffsetDateTime earliest = group.getFirst().getRecordedAt();

        hardwareStatusRepository.deleteAll(group);
        hardwareStatusRepository.save(new HardwareStatusRecord(average, earliest));

        return group.size() - 1;
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
