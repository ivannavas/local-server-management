package io.github.ivannavas.local_server_management.tools;

import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.sprout.annotation.Tool;
import io.github.ivannavas.sprout.mcp.annotation.Mcp;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

@Mcp(name = "local-server-management")
public class SystemTools {

    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hal = systemInfo.getHardware();

    @Tool(name = "getHardwareStatus", description = "Get the hardware status information")
    public HardwareStatus getHardwareStatus() {
        double cpuTemperature = hal.getSensors().getCpuTemperature();
        int[] fanSpeeds = hal.getSensors().getFanSpeeds();
        return new HardwareStatus(cpuTemperature, fanSpeeds);
    }
}
