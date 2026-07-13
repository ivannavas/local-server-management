package io.github.ivannavas.local_server_management.service.impl;

import io.github.ivannavas.local_server_management.model.HardwareStatus;
import io.github.ivannavas.local_server_management.service.SystemService;
import io.github.ivannavas.local_server_management.tools.SystemTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemServiceImpl implements SystemService {
    @Autowired
    private SystemTools systemTools;


    @Override
    public HardwareStatus getHardwareStatus() {
        return systemTools.getHardwareStatus();
    }
}
