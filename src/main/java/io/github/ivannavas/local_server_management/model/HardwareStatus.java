package io.github.ivannavas.local_server_management.model;

import java.util.Map;

public record HardwareStatus(
        double cpuTemperature,
        boolean boostEnabled,
        Map<String, Long> databasesSize,
        long totalSize
) {
}
