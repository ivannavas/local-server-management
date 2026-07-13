package io.github.ivannavas.local_server_management.model;

public record HardwareStatus(
        double cpuTemperature,
        int[] fanSpeeds
) {
}
