package com.greensqa.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class ServiceClient {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String cookie;
    private final Duration timeout;

    public ServiceClient(String cookie) {
        this.cookie = cookie == null ? "" : cookie;
        this.timeout = Duration.ofSeconds(45);
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.mapper = new ObjectMapper();
    }

    public JsonNode postJson(String url, JsonNode body, Map<String, String> extraHeaders) {
        try {
            String payload = mapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            if (!cookie.isEmpty()) builder.header("Cookie", cookie);
            if (extraHeaders != null) extraHeaders.forEach(builder::header);

            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " -> " + resp.body());
            }
            return mapper.readTree(resp.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error POST JSON: " + e.getMessage(), e);
        }
    }

    public void saveJson(java.nio.file.Path path, JsonNode data) {
        try {
            java.nio.file.Files.createDirectories(path.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando JSON: " + path, e);
        }
    }

    public ObjectMapper mapper() { return mapper; }
}
