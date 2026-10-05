package com.myshop.service;

import javafx.scene.image.Image;
import org.glavo.webp.WebPImage;
import org.glavo.webp.javafx.WebPFXImage;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/** Loads provider-returned product images without blocking the JavaFX thread. */
public final class RemoteImageService {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(12);
    public static final int DEFAULT_CACHE_SIZE = 64;

    private static final RemoteImageService SHARED = new RemoteImageService();

    private final RemoteImageTransport transport;
    private final Executor executor;
    private final int maxCacheEntries;
    private final Map<String, CompletableFuture<Result>> cache = new LinkedHashMap<>(16, 0.75f, true);

    public static RemoteImageService shared() {
        return SHARED;
    }

    public RemoteImageService() {
        this(new JavaHttpRemoteImageTransport(DEFAULT_TIMEOUT), ForkJoinPool.commonPool(), DEFAULT_CACHE_SIZE);
    }

    RemoteImageService(RemoteImageTransport transport, Executor executor, int maxCacheEntries) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.executor = Objects.requireNonNull(executor, "executor");
        if (maxCacheEntries < 1) {
            throw new IllegalArgumentException("maxCacheEntries must be positive");
        }
        this.maxCacheEntries = maxCacheEntries;
    }

    public CompletableFuture<Result> load(String value) {
        String url = value == null ? "" : value.trim();
        if (!ExternalLinkService.isSafe(url)) {
            return CompletableFuture.completedFuture(Result.failure(Status.INVALID_URL, 0, "", null, false));
        }

        synchronized (cache) {
            CompletableFuture<Result> existing = cache.get(url);
            if (existing != null) {
                return existing;
            }
            CompletableFuture<Result> future = CompletableFuture.supplyAsync(
                    () -> fetch(URI.create(url)), executor
            );
            cache.put(url, future);
            trimCache();
            future.whenComplete((result, error) -> {
                if (error != null || result == null || !result.loaded()) {
                    synchronized (cache) {
                        cache.remove(url, future);
                    }
                }
            });
            return future;
        }
    }

    private Result fetch(URI uri) {
        try {
            HttpResult response = transport.get(uri, DEFAULT_TIMEOUT);
            if (response.statusCode() != 200) {
                return Result.failure(Status.HTTP_ERROR, response.statusCode(), response.contentType(), null,
                        response.redirected());
            }
            if (response.body().length == 0) {
                return Result.failure(Status.EMPTY_BODY, response.statusCode(), response.contentType(), null,
                        response.redirected());
            }
            Image image = decode(response.body());
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return Result.failure(Status.DECODE_ERROR, response.statusCode(), response.contentType(), response.body(),
                        response.redirected());
            }
            return new Result(Status.LOADED, response.statusCode(), response.contentType(), response.body(), image,
                    response.redirected());
        } catch (Exception exception) {
            return Result.failure(Status.FAILED, 0, "", null, false);
        }
    }

    private Image decode(byte[] bytes) throws Exception {
        if (isWebp(bytes)) {
            return new WebPFXImage(WebPImage.read(new ByteArrayInputStream(bytes)));
        }

        return new Image(new ByteArrayInputStream(bytes));
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private void trimCache() {
        while (cache.size() > maxCacheEntries) {
            cache.remove(cache.keySet().iterator().next());
        }
    }

    public enum Status {
        LOADED,
        INVALID_URL,
        HTTP_ERROR,
        EMPTY_BODY,
        DECODE_ERROR,
        FAILED
    }

    public record Result(Status status, int statusCode, String contentType, byte[] bytes, Image image,
                         boolean redirected) {
        public Result {
            contentType = contentType == null ? "" : contentType;
            bytes = bytes == null ? new byte[0] : bytes.clone();
        }

        static Result failure(Status status, int statusCode, String contentType, byte[] bytes, boolean redirected) {
            return new Result(status, statusCode, contentType, bytes, null, redirected);
        }

        public boolean loaded() {
            return status == Status.LOADED && image != null;
        }
    }

    interface RemoteImageTransport {
        HttpResult get(URI uri, Duration timeout) throws Exception;
    }

    record HttpResult(int statusCode, String contentType, byte[] body, boolean redirected) {
        HttpResult {
            contentType = contentType == null ? "" : contentType;
            body = body == null ? new byte[0] : body.clone();
        }
    }

    private static final class JavaHttpRemoteImageTransport implements RemoteImageTransport {
        private final HttpClient client;

        private JavaHttpRemoteImageTransport(Duration connectTimeout) {
            client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(connectTimeout)
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
        }

        @Override
        public HttpResult get(URI uri, Duration timeout) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "image/*")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return new HttpResult(
                    response.statusCode(),
                    response.headers().firstValue("content-type").orElse(""),
                    response.body(),
                    response.previousResponse().isPresent()
            );
        }
    }
}
