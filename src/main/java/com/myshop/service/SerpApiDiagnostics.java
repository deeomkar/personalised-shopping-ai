package com.myshop.service;

/**
 * Redacted diagnostics for one SerpApi request. This contains no API key or
 * request query values and is intended for verification tooling, not UI copy.
 */
public record SerpApiDiagnostics(
        int statusCode,
        String endpoint,
        boolean googleShoppingEngine,
        boolean queryPresent,
        boolean apiKeyPresentAndNonEmpty,
        boolean englishLanguage,
        boolean indiaCountry,
        String responseCategory,
        String providerMessage,
        String sanitizedResponseBody,
        String searchId,
        boolean shoppingResultsPresent,
        int shoppingResultCount,
        int mappedProductCount,
        int liveProductCount
) {
    public SerpApiDiagnostics {
        endpoint = endpoint == null ? "" : endpoint;
        responseCategory = responseCategory == null ? "UNKNOWN" : responseCategory;
        providerMessage = providerMessage == null ? "" : providerMessage;
        sanitizedResponseBody = sanitizedResponseBody == null ? "" : sanitizedResponseBody;
        searchId = searchId == null || searchId.isBlank() ? null : searchId;
        shoppingResultCount = Math.max(0, shoppingResultCount);
        mappedProductCount = Math.max(0, mappedProductCount);
        liveProductCount = Math.max(0, liveProductCount);
    }

    public SerpApiDiagnostics withMappedProductCounts(int mappedCount, int liveCount) {
        return new SerpApiDiagnostics(
                statusCode, endpoint, googleShoppingEngine, queryPresent,
                apiKeyPresentAndNonEmpty, englishLanguage, indiaCountry,
                responseCategory, providerMessage, sanitizedResponseBody,
                searchId, shoppingResultsPresent, shoppingResultCount, mappedCount, liveCount
        );
    }
}
