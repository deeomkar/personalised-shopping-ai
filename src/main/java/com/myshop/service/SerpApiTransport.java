package com.myshop.service;

import java.net.URI;
import java.time.Duration;

/** HTTP boundary for SerpApi, allowing tests to avoid live quota usage. */
@FunctionalInterface
public interface SerpApiTransport {

    SerpApiHttpResponse get(URI uri, Duration timeout);
}
