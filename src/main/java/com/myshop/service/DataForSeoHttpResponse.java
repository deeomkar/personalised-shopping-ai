package com.myshop.service;

/** Minimal HTTP result used by the DataForSEO adapter and its fixture tests. */
public record DataForSeoHttpResponse(int statusCode, String body) {
    public DataForSeoHttpResponse {
        body = body == null ? "" : body;
    }
}
