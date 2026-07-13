package io.github.ivannavas.local_server_management.controller;

import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.local_server_management.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system")
public class SystemRestController {
    @Autowired
    private SystemService systemService;

    @GetMapping("/hardware-status")
    public HardwareStatus getHardwareStatus() {
        return systemService.getHardwareStatus();
    }
}
