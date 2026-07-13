package io.github.ivannavas.local_server_management.service.impl;

import io.github.ivannavas.local_server_management.service.NtfyService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class NtfyServiceImpl implements NtfyService {
    private RestClient restClient;

    @Value("${ntfy.server.url}")
    private String serverUrl;

    @Value("${ntfy.server.topic}")
    private String topic;

    @PostConstruct
    public void init() {
        restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }

    @Override
    public void send(String title, String message, String tags) {
        try {
            restClient.post()
                    .uri("/{topic}", topic)
                    .header("Title", title)
                    .header("Tags", tags)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send ntfy notification: {}", e.getMessage());
        }
    }
}
