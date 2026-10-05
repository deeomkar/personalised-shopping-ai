package com.myshop.service;

import org.junit.jupiter.api.Test;

import java.net.http.HttpTimeoutException;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteImageServiceTest {
    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );
    private static final byte[] ONE_PIXEL_WEBP = Base64.getDecoder().decode(
            "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA"
    );

    @Test
    void loadsNonEmptyHttp200ImageBytesIntoJavaFxImage() {
        RemoteImageService service = service((uri, timeout) -> new RemoteImageService.HttpResult(
                200, "image/png", ONE_PIXEL_PNG, false
        ));

        RemoteImageService.Result result = service.load("https://images.example/product.png").join();

        assertEquals(RemoteImageService.Status.LOADED, result.status());
        assertEquals(200, result.statusCode());
        assertEquals("image/png", result.contentType());
        assertEquals(ONE_PIXEL_PNG.length, result.bytes().length);
        assertNotNull(result.image());
        assertTrue(result.loaded());
    }

    @Test
    void convertsWebpBytesWhenJavaFxNativeDecoderRejectsThem() {
        RemoteImageService service = service((uri, timeout) -> new RemoteImageService.HttpResult(
                200, "image/webp", ONE_PIXEL_WEBP, false
        ));

        RemoteImageService.Result result = service.load("https://images.example/product.webp").join();

        assertEquals(RemoteImageService.Status.LOADED, result.status());
        assertNotNull(result.image());
    }

    @Test
    void non200ResponseUsesFailureState() {
        RemoteImageService service = service((uri, timeout) -> new RemoteImageService.HttpResult(
                404, "text/html", new byte[]{1}, false
        ));

        RemoteImageService.Result result = service.load("https://images.example/missing.png").join();

        assertEquals(RemoteImageService.Status.HTTP_ERROR, result.status());
        assertEquals(404, result.statusCode());
        assertTrue(!result.loaded());
    }

    @Test
    void timeoutUsesFailureState() {
        RemoteImageService service = service((uri, timeout) -> {
            throw new HttpTimeoutException("timed out");
        });

        assertEquals(RemoteImageService.Status.FAILED,
                service.load("https://images.example/slow.png").join().status());
    }

    @Test
    void successfulImageIsReusedFromBoundedCache() {
        AtomicInteger requests = new AtomicInteger();
        RemoteImageService service = service((uri, timeout) -> {
            requests.incrementAndGet();
            return new RemoteImageService.HttpResult(200, "image/png", ONE_PIXEL_PNG, false);
        });

        RemoteImageService.Result first = service.load("https://images.example/cached.png").join();
        RemoteImageService.Result second = service.load("https://images.example/cached.png").join();

        assertSame(first, second);
        assertEquals(1, requests.get());
    }

    private RemoteImageService service(RemoteImageService.RemoteImageTransport transport) {
        return new RemoteImageService(transport, Runnable::run, 4);
    }
}
