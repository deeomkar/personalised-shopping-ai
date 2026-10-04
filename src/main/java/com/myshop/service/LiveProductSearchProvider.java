package com.myshop.service;

import com.myshop.config.ProductSearchProviderConfig;
import com.myshop.config.DataForSeoConfig;
import com.myshop.model.Product;
import com.myshop.model.SearchRequest;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provider shell for a live product-search integration.
 *
 * <p>Vendor-specific HTTP and mapping stay behind {@link LiveProductSearchAdapter}.
 * A configured fallback keeps the application usable when a live request fails.</p>
 */
public final class LiveProductSearchProvider implements ProductSearchProvider {

    private static final Logger LOGGER = Logger.getLogger(LiveProductSearchProvider.class.getName());

    private final ProductSearchProviderConfig configuration;
    private final LiveProductSearchAdapter adapter;
    private final ProductSearchProvider fallback;

    public LiveProductSearchProvider() {
        this(ProductSearchProviderConfig.fromEnvironment());
    }

    public LiveProductSearchProvider(ProductSearchProviderConfig configuration) {
        this(configuration, new UnconfiguredAdapter(
                Objects.requireNonNull(configuration, "configuration").providerId()), null);
    }

    public LiveProductSearchProvider(
            ProductSearchProviderConfig configuration,
            LiveProductSearchAdapter adapter
    ) {
        this(configuration, adapter, null);
    }

    public LiveProductSearchProvider(
            ProductSearchProviderConfig configuration,
            LiveProductSearchAdapter adapter,
            ProductSearchProvider fallback
    ) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.fallback = fallback;
    }

    public LiveProductSearchProvider(DataForSeoConfig configuration) {
        this(
                new ProductSearchProviderConfig(
                        "dataforseo",
                        configuration.hasCredentials() ? "configured" : "",
                        configuration.baseUri(),
                        configuration.requestTimeout()
                ),
                new DataForSeoProductSearchAdapter(configuration),
                new MockProductSearchProvider()
        );
    }

    public ProductSearchProviderConfig configuration() {
        return configuration;
    }

    @Override
    public List<Product> search(SearchRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            List<Product> products = adapter.search(request);
            if (products == null || products.isEmpty()) {
                throw new IllegalStateException("Live product provider returned no products");
            }
            return List.copyOf(products);
        } catch (RuntimeException exception) {
            if (fallback == null) {
                throw exception;
            }
            LOGGER.log(Level.WARNING, "Live product search failed; using mock provider: "
                    + exception.getClass().getSimpleName());
            return List.copyOf(fallback.search(request));
        }
    }

    private static final class UnconfiguredAdapter implements LiveProductSearchAdapter {
        private final String providerId;

        private UnconfiguredAdapter(String providerId) {
            this.providerId = providerId;
        }

        @Override
        public List<Product> search(SearchRequest request) {
            throw new IllegalStateException(
                    "No live product adapter is configured for provider '"
                            + providerId + "'");
        }
    }
}
