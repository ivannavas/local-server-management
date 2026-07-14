package io.github.ivannavas.local_server_management.service.impl;

import io.github.ivannavas.local_server_management.entity.HardwareStatusRecord;
import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.local_server_management.model.HardwareStatusPatchRequest;
import io.github.ivannavas.local_server_management.repository.HardwareStatusRepository;
import io.github.ivannavas.local_server_management.service.NtfyService;
import io.github.ivannavas.local_server_management.service.SystemService;
import io.github.ivannavas.local_server_management.tools.SystemTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@Slf4j
public class SystemServiceImpl implements SystemService {

    private static final double BASE_THRESHOLD = 65.0;
    private static final double THRESHOLD_STEP = 10.0;

    private static final BigDecimal AGGREGATION_TOLERANCE = BigDecimal.valueOf(3.0);

    private static final Path HWMON_BASE = Path.of("/sys/class/hwmon");
    private static final Path THERMAL_BASE = Path.of("/sys/class/thermal");

    private static final Set<String> CPU_HWMON_NAMES =
            Set.of("coretemp", "k10temp", "zenpower", "cpu_thermal");
    private static final Pattern TEMP_INPUT = Pattern.compile("temp\\d+_input");
    private static final Set<String> PACKAGE_LABELS = Set.of("package", "tctl", "tdie");

    @Value("${system.boost.read-path:/sys/devices/system/cpu/cpufreq/boost}")
    private String boostReadPath;

    @Value("${system.boost.write-path:/var/lib/cpu-boost/desired}")
    private String boostWritePath;

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
    public HardwareStatus updateHardwareStatus(HardwareStatusPatchRequest req) {
        writeBoostEnabled(req.boostEnabled());
        try {
            wait(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        recordHardwareStatus();
        return getHardwareStatus();
    }

    @Override
    public List<HardwareStatusRecord> getHardwareStatusHistory(OffsetDateTime from, OffsetDateTime to) {
        return hardwareStatusRepository.findByRecordedAtBetween(from, to);
    }

    @Scheduled(fixedRate = 60_000)
    public void recordHardwareStatus() {
        double temperature = readCpuTemperature();
        HardwareStatusRecord record = new HardwareStatusRecord(
                BigDecimal.valueOf(temperature),
                readBoostEnabled(),
                OffsetDateTime.now()
        );
        hardwareStatusRepository.save(record);

        notifyOnTemperatureChange(temperature);
    }

    private boolean readBoostEnabled() {
        try {
            return "1".equals(Files.readString(Path.of(boostReadPath)).trim());
        } catch (IOException e) {
            log.warn("Could not read CPU boost state from sysfs at {}", boostReadPath, e);
            return false;
        }
    }

    private void writeBoostEnabled(boolean enabled) {
        try {
            Files.writeString(Path.of(boostWritePath), enabled ? "1" : "0");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not request CPU boost change at " + boostWritePath, e);
        }
    }

    private double readCpuTemperature() {
        Path tempInput = findCpuTempInput();
        if (tempInput == null) {
            log.warn("Could not find a CPU temperature sensor in sysfs (hwmon/thermal)");
            return 0.0;
        }
        try {
            String raw = Files.readString(tempInput).trim();
            return Long.parseLong(raw) / 1000.0;
        } catch (IOException | NumberFormatException e) {
            log.warn("Could not read CPU temperature from sysfs at {}", tempInput, e);
            return 0.0;
        }
    }

    /**
     * Locates the CPU temperature input file, preferring the hwmon coretemp/k10temp
     * driver (available on bare metal such as Proxmox) and falling back to the ACPI
     * thermal zones.
     */
    private Path findCpuTempInput() {
        Path hwmonInput = findHwmonCpuTempInput();
        return hwmonInput != null ? hwmonInput : findThermalZoneTempInput();
    }

    private Path findHwmonCpuTempInput() {
        if (!Files.isDirectory(HWMON_BASE)) {
            return null;
        }
        try (Stream<Path> hwmons = Files.list(HWMON_BASE)) {
            return hwmons
                    .filter(this::isCpuHwmon)
                    .map(this::preferredTempInput)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isCpuHwmon(Path hwmon) {
        try {
            String name = Files.readString(hwmon.resolve("name")).trim().toLowerCase();
            return CPU_HWMON_NAMES.contains(name);
        } catch (IOException e) {
            return false;
        }
    }

    private Path preferredTempInput(Path hwmon) {
        try (Stream<Path> files = Files.list(hwmon)) {
            List<Path> inputs = files
                    .filter(p -> TEMP_INPUT.matcher(p.getFileName().toString()).matches())
                    .sorted()
                    .toList();
            return inputs.stream()
                    .filter(this::isPackageLabel)
                    .findFirst()
                    .orElse(inputs.isEmpty() ? null : inputs.getFirst());
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isPackageLabel(Path tempInput) {
        String labelFile = tempInput.getFileName().toString().replace("_input", "_label");
        try {
            String label = Files.readString(tempInput.resolveSibling(labelFile)).trim().toLowerCase();
            return PACKAGE_LABELS.stream().anyMatch(label::contains);
        } catch (IOException e) {
            return false;
        }
    }

    private Path findThermalZoneTempInput() {
        if (!Files.isDirectory(THERMAL_BASE)) {
            return null;
        }
        try (Stream<Path> zones = Files.list(THERMAL_BASE)) {
            return zones
                    .filter(zone -> zone.getFileName().toString().startsWith("thermal_zone"))
                    .filter(this::isCpuZone)
                    .map(zone -> zone.resolve("temp"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isCpuZone(Path zone) {
        try {
            String type = Files.readString(zone.resolve("type")).trim().toLowerCase();
            return type.contains("x86_pkg_temp") || type.contains("cpu") || type.contains("coretemp");
        } catch (IOException e) {
            return false;
        }
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
        HardwareStatusRecord earliest = group.getFirst();

        hardwareStatusRepository.deleteAll(group);
        hardwareStatusRepository.save(
                new HardwareStatusRecord(average, earliest.isBoostEnabled(), earliest.getRecordedAt()));

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
