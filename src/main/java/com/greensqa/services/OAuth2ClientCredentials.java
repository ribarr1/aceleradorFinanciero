package com.greensqa.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class OAuth2ClientCredentials {

    private final HttpClient http;
    private final ObjectMapper mapper;

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scope;     // puede ser null
    //private final String audience;  // puede ser null (Auth0 usa audience, otros no)

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public OAuth2ClientCredentials(HttpClient http, ObjectMapper mapper,
                                   String tokenUrl, String clientId, String clientSecret,
                                   String scope) { //, String audience
        this.http = http;
        this.mapper = mapper;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
       // this.audience = audience;
    }

    public synchronized String getAccessToken() throws IOException, InterruptedException {
        // refresca si falta poco (30s)
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
            return cachedToken;
        }

        String form = "grant_type=client_credentials"
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);

        if (scope != null && !scope.isBlank()) {
            form += "&scope=" + enc(scope);
        }
       // if (audience != null && !audience.isBlank()) {
        //    form += "&audience=" + enc(audience);
        //}

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("Token request failed: HTTP " + resp.statusCode() + " body=" + resp.body());
        }

        JsonNode json = mapper.readTree(resp.body());
        String accessToken = json.path("access_token").asText(null);
        long expiresIn = json.path("expires_in").asLong(3600);

        if (accessToken == null || accessToken.isBlank()) {
            throw new IOException("No access_token in response: " + resp.body());
        }

        this.cachedToken = accessToken;
        this.expiresAt = Instant.now().plusSeconds(expiresIn);
        return accessToken;
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
