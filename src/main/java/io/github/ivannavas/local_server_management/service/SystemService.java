package io.github.ivannavas.local_server_management.service;

import io.github.ivannavas.local_server_management.model.HardwareStatus;

public interface SystemService {
    HardwareStatus getHardwareStatus();
}
