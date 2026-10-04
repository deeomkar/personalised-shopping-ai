package com.myshop.service;

@FunctionalInterface
interface GroqHttpTransport {
    GroqHttpResponse post(String apiKey, String requestBody);
}
