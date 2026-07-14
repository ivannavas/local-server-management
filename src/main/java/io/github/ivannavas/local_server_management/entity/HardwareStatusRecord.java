package io.github.ivannavas.local_server_management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "hardware_statuses")
public class HardwareStatusRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cpu_temperature", precision = 5, scale = 2)
    private BigDecimal cpuTemperature;

    @Column(name = "boost_enabled")
    private boolean boostEnabled;

    @Column(name = "recorded_at")
    private OffsetDateTime recordedAt;

    public HardwareStatusRecord(BigDecimal cpuTemperature, boolean boostEnabled, OffsetDateTime recordedAt) {
        this.cpuTemperature = cpuTemperature;
        this.boostEnabled = boostEnabled;
        this.recordedAt = recordedAt;
    }
}
