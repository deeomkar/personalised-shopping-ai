package com.myshop.service;

/** Minimal HTTP result used by the SerpApi adapter and fixture tests. */
public record SerpApiHttpResponse(int statusCode, String body) {
    public SerpApiHttpResponse {
        body = body == null ? "" : body;
    }
}
