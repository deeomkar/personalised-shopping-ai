package com.myshop.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Built-in Java HTTP transport for SerpApi. */
public final class JavaHttpSerpApiTransport implements SerpApiTransport {
    private final HttpClient client;

    public JavaHttpSerpApiTransport(Duration connectTimeout) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public SerpApiHttpResponse get(URI uri, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new SerpApiHttpResponse(response.statusCode(), response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("SerpApi network request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SerpApi request was interrupted", exception);
        }
    }
}
