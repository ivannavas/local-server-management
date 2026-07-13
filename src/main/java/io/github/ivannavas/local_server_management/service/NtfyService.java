package io.github.ivannavas.local_server_management.service;

public interface NtfyService {
    void send(String title, String message, String tags);
}
