package com.myshop.service;

import com.myshop.config.DataForSeoConfig;
import com.myshop.config.ProductSearchProviderConfig;
import com.myshop.config.SerpApiConfig;

/** Selects SerpApi when configured and otherwise keeps the mock provider safe. */
public final class ProductSearchProviderFactory {

    private ProductSearchProviderFactory() {
    }

    public static ProductSearchProvider fromEnvironment() {
        return create(SerpApiConfig.fromEnvironment());
    }

    static ProductSearchProvider create(SerpApiConfig configuration) {
        if (configuration == null || !configuration.isConfigured()) {
            return new MockProductSearchProvider();
        }
        return new LiveProductSearchProvider(
                new ProductSearchProviderConfig(
                        "serpapi", configuration.apiKey(), configuration.baseUri(), configuration.requestTimeout()
                ),
                new SerpApiProductSearchAdapter(configuration),
                new MockProductSearchProvider()
        );
    }

    /** Compatibility hook for Block 7B fixtures; DataForSEO is no longer runtime-selected. */
    static ProductSearchProvider create(DataForSeoConfig ignored) {
        return new MockProductSearchProvider();
    }
}
