package com.myshop.service;

import java.net.URI;
import java.time.Duration;

/** HTTP boundary for DataForSEO, allowing the adapter to be tested without network calls. */
@FunctionalInterface
public interface DataForSeoTransport {

    DataForSeoHttpResponse send(
            String method,
            URI uri,
            String login,
            String password,
            String body,
            Duration timeout
    );
}
