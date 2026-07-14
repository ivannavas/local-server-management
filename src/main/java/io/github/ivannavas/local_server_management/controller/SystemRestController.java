package io.github.ivannavas.local_server_management.controller;

import io.github.ivannavas.local_server_management.entity.HardwareStatusRecord;
import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.local_server_management.model.HardwareStatusPatchRequest;
import io.github.ivannavas.local_server_management.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/system")
public class SystemRestController {

    @Autowired
    private SystemService systemService;

    @GetMapping("/hardware-status")
    public HardwareStatus getHardwareStatus() {
        return systemService.getHardwareStatus();
    }

    @PatchMapping("/hardware-status")
    public HardwareStatus updateHardwareStatus(@RequestBody HardwareStatusPatchRequest request) {
        if (request.boostEnabled() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "boostEnabled is required");
        }
        return systemService.updateHardwareStatus(request);
    }

    @GetMapping("/hardware-status/history")
    public List<HardwareStatusRecord> getHardwareStatusHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return systemService.getHardwareStatusHistory(from, to);
    }
}
