package com.myshop.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class JavaHttpGroqTransport implements GroqHttpTransport {
    private static final URI CHAT_COMPLETIONS_URI = URI.create(
            "https://api.groq.com/openai/v1/chat/completions"
    );
    private final HttpClient client;

    JavaHttpGroqTransport() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    JavaHttpGroqTransport(HttpClient client) {
        this.client = client;
    }

    @Override
    public GroqHttpResponse post(String apiKey, String requestBody) {
        HttpRequest request = HttpRequest.newBuilder(CHAT_COMPLETIONS_URI)
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new GroqHttpResponse(response.statusCode(), response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Groq network request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Groq request was interrupted", exception);
        }
    }
}
