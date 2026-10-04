package com.myshop.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/** Built-in Java HTTP transport for the DataForSEO REST API. */
public final class JavaHttpDataForSeoTransport implements DataForSeoTransport {
    private final HttpClient client;

    public JavaHttpDataForSeoTransport(Duration connectTimeout) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    @Override
    public DataForSeoHttpResponse send(
            String method,
            URI uri,
            String login,
            String password,
            String body,
            Duration timeout
    ) {
        String credentials = login + ":" + password;
        String basic = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/json");
        if ("POST".equalsIgnoreCase(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method");
        }

        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new DataForSeoHttpResponse(response.statusCode(), response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("DataForSEO network request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DataForSEO request was interrupted", exception);
        }
    }
}
