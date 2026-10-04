package com.myshop.service;

import com.myshop.config.ProductSearchProviderConfig;
import com.myshop.model.Product;
import com.myshop.model.SearchFilters;
import com.myshop.model.SearchRequest;
import com.myshop.model.SearchSortMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LiveProductSearchProviderTest {

    @Test
    void unconfiguredShellFailsClearlyInsteadOfFabricatingResults() {
        LiveProductSearchProvider provider = new LiveProductSearchProvider(
                new ProductSearchProviderConfig("dataforseo", "", null,
                        ProductSearchProviderConfig.DEFAULT_REQUEST_TIMEOUT)
        );

        assertThrows(IllegalStateException.class, () -> provider.search(request()));
    }

    @Test
    void selectedAdapterIsTheOnlySourceOfLiveResults() {
        List<Product> expected = MockCatalogService.searchableProducts().subList(0, 1);
        LiveProductSearchProvider provider = new LiveProductSearchProvider(
                new ProductSearchProviderConfig("test", "key", null,
                        ProductSearchProviderConfig.DEFAULT_REQUEST_TIMEOUT),
                ignored -> expected
        );

        assertEquals(expected, provider.search(request()));
    }

    private SearchRequest request() {
        return new SearchRequest(
                "headphones", null, 1, null, SearchFilters.none(), SearchSortMode.RECOMMENDED
        );
    }
}
