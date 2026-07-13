package io.github.ivannavas.local_server_management.service.impl;

import io.github.ivannavas.local_server_management.service.NtfyService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        List<String> tagList = (tags == null || tags.isBlank())
                ? List.of()
                : Arrays.stream(tags.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();

        Map<String, Object> payload = Map.of(
                "topic", topic,
                "title", title,
                "message", message,
                "tags", tagList
        );

        try {
            restClient.post()
                    .uri("/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send ntfy notification: {}", e.getMessage());
        }
    }
}
