package io.github.ivannavas.local_server_management.tools;

import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.local_server_management.repository.HardwareStatusRepository;
import io.github.ivannavas.sprout.annotation.Tool;
import io.github.ivannavas.sprout.mcp.annotation.Mcp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Mcp(name = "local-server-management")
public class SystemTools {

    @Autowired
    private HardwareStatusRepository hardwareStatusRepository;

    @Tool(name = "getHardwareStatus", description = "Get the hardware status information")
    public HardwareStatus getHardwareStatus() {
        return hardwareStatusRepository.findTopByOrderByRecordedAtDesc()
                .map(record -> new HardwareStatus(
                        record.getCpuTemperature().doubleValue(),
                        record.isBoostEnabled()))
                .orElse(new HardwareStatus(0.0, false));
    }
}
