package com.myshop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.component.IconType;
import com.myshop.config.SerpApiConfig;
import com.myshop.model.Product;
import com.myshop.model.ProductOffer;
import com.myshop.model.SearchRequest;
import com.myshop.model.ShoppingIntent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** SerpApi Google Shopping adapter isolated behind the live product seam. */
public final class SerpApiProductSearchAdapter implements LiveProductSearchAdapter {
    private final SerpApiConfig config;
    private final SerpApiTransport transport;
    private final ObjectMapper objectMapper;
    private volatile SerpApiDiagnostics lastDiagnostics;

    public SerpApiProductSearchAdapter(SerpApiConfig config) {
        this(config, new JavaHttpSerpApiTransport(config.requestTimeout()), new ObjectMapper());
    }

    SerpApiProductSearchAdapter(
            SerpApiConfig config,
            SerpApiTransport transport,
            ObjectMapper objectMapper
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /** Returns the latest redacted request/response diagnostics for verification tooling. */
    public Optional<SerpApiDiagnostics> diagnostics() {
        return Optional.ofNullable(lastDiagnostics);
    }

    @Override
    public List<Product> search(SearchRequest request) {
        Objects.requireNonNull(request, "request");
        if (!config.isConfigured()) {
            throw new IllegalStateException("SERPAPI_API_KEY is not configured");
        }
        URI requestUri = buildUri(request);
        SerpApiHttpResponse response = transport.get(requestUri, config.requestTimeout());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            SerpApiDiagnostics diagnostics = diagnostics(
                    requestUri, response, "HTTP_ERROR", ""
            );
            lastDiagnostics = diagnostics;
            throw new SerpApiException("SerpApi HTTP " + response.statusCode(), diagnostics);
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            SerpApiDiagnostics diagnostics = diagnostics(requestUri, response, "SUCCESS", root);
            if (root.hasNonNull("error")) {
                diagnostics = new SerpApiDiagnostics(
                        diagnostics.statusCode(), diagnostics.endpoint(),
                        diagnostics.googleShoppingEngine(), diagnostics.queryPresent(),
                        diagnostics.apiKeyPresentAndNonEmpty(), diagnostics.englishLanguage(),
                        diagnostics.indiaCountry(), "PROVIDER_ERROR",
                        sanitize(root.path("error").asText("")), diagnostics.sanitizedResponseBody(),
                        diagnostics.searchId(), diagnostics.shoppingResultsPresent(),
                        diagnostics.shoppingResultCount(), 0, 0
                );
                lastDiagnostics = diagnostics;
                throw new SerpApiException("SerpApi returned an API error", diagnostics);
            }
            MappingResult mapping = mapResults(root.path("shopping_results"), request);
            lastDiagnostics = diagnostics.withMappedProductCounts(
                    mapping.mappedProductCount(), mapping.products().size()
            );
            return mapping.products();
        } catch (SerpApiException exception) {
            throw exception;
        } catch (Exception exception) {
            SerpApiDiagnostics diagnostics = diagnostics(
                    requestUri, response, "MALFORMED_RESPONSE", exception.getMessage()
            );
            lastDiagnostics = diagnostics;
            throw new SerpApiException("SerpApi response was malformed", diagnostics, exception);
        }
    }

    private SerpApiDiagnostics diagnostics(
            URI requestUri,
            SerpApiHttpResponse response,
            String category,
            String providerMessage
    ) {
        try {
            JsonNode root = objectMapper.readTree(response.body());
            return diagnostics(requestUri, response, category, root)
                    .withMappedProductCounts(0, 0);
        } catch (Exception ignored) {
            Map<String, String> query = queryFlags(requestUri);
            return new SerpApiDiagnostics(
                    response.statusCode(), endpoint(requestUri),
                    "google_shopping".equalsIgnoreCase(query.get("engine")),
                    query.containsKey("q"), query.containsKey("api_key")
                            && !query.get("api_key").isBlank(),
                    "en".equalsIgnoreCase(query.get("hl")),
                    "in".equalsIgnoreCase(query.get("gl")),
                    category, sanitize(providerMessage), sanitize(response.body()), null, false, 0, 0, 0
            );
        }
    }

    private SerpApiDiagnostics diagnostics(
            URI requestUri,
            SerpApiHttpResponse response,
            String category,
            JsonNode root
    ) {
        Map<String, String> query = queryFlags(requestUri);
        JsonNode metadata = root == null ? null : root.path("search_metadata");
        String searchId = metadata == null ? null : text(metadata, "id");
        boolean hasShoppingResults = root != null && root.has("shopping_results")
                && root.get("shopping_results").isArray();
        int shoppingResultCount = hasShoppingResults ? root.get("shopping_results").size() : 0;
        return new SerpApiDiagnostics(
                response.statusCode(), endpoint(requestUri),
                "google_shopping".equalsIgnoreCase(query.get("engine")),
                query.containsKey("q") && !query.get("q").isBlank(),
                query.containsKey("api_key") && !query.get("api_key").isBlank(),
                "en".equalsIgnoreCase(query.get("hl")),
                "in".equalsIgnoreCase(query.get("gl")),
                category,
                sanitize(providerMessage(root)),
                sanitize(response.body()),
                searchId,
                hasShoppingResults,
                shoppingResultCount,
                0,
                0
        );
    }

    private String providerMessage(JsonNode root) {
        if (root == null) {
            return "";
        }
        String error = text(root, "error");
        if (!error.isBlank()) {
            return error;
        }
        return text(root, "message");
    }

    private Map<String, String> queryFlags(URI uri) {
        Map<String, String> query = new LinkedHashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }
        for (String parameter : rawQuery.split("&")) {
            String[] parts = parameter.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length == 1 ? "" : decode(parts[1]);
            query.put(key, value);
        }
        return query;
    }

    private String endpoint(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme() + "://";
        String authority = uri.getRawAuthority() == null ? "" : uri.getRawAuthority();
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        return scheme + authority + path;
    }

    private String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = value.replace(config.apiKey(), "[REDACTED]")
                .replaceAll("(?i)(api_key\\s*[=:]\\s*[\\\"]?)[^&\\s,}\\\"]+", "$1[REDACTED]")
                .replaceAll("[\\r\\n]+", " ")
                .trim();
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) + "…" : sanitized;
    }

    private URI buildUri(SearchRequest request) {
        List<String> parameters = new ArrayList<>();
        add(parameters, "engine", "google_shopping");
        add(parameters, "q", buildKeyword(request));
        add(parameters, "api_key", config.apiKey());
        add(parameters, "gl", config.country());
        add(parameters, "hl", config.language());
        add(parameters, "google_domain", config.googleDomain());
        add(parameters, "num", Integer.toString(config.maxResults()));
        return URI.create(config.baseUri() + "?" + String.join("&", parameters));
    }

    private MappingResult mapResults(JsonNode results, SearchRequest request) {
        if (results == null || !results.isArray()) {
            return new MappingResult(List.of(), 0);
        }
        List<Product> products = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int mappedProductCount = 0;
        for (JsonNode item : results) {
            MappedProduct mapped = mapProduct(item, request);
            if (mapped == null || !seen.add(mapped.product().id())) {
                continue;
            }
            mappedProductCount++;
            if (!withinBudget(mapped.price(), mapped.currency(), request)) {
                continue;
            }
            Product product = mapped.product();
            products.add(product);
            if (products.size() >= config.maxResults()) {
                break;
            }
        }
        return new MappingResult(List.copyOf(products), mappedProductCount);
    }

    private MappedProduct mapProduct(JsonNode item, SearchRequest request) {
        String name = text(item, "title");
        if (name.isBlank()) {
            return null;
        }
        BigDecimal price = decimal(item.get("extracted_price"));
        if (price == null) {
            price = decimal(item.get("price"));
        }
        BigDecimal originalPrice = decimal(item.get("extracted_old_price"));
        if (originalPrice == null) {
            originalPrice = decimal(item.get("old_price"));
        }
        String currency = currency(item);
        String source = text(item, "source");
        String productUrl = firstHttpUrl(item, "link", "product_link");
        String id = firstText(item, "product_id", "productId");
        if (id.isBlank()) {
            id = stableId(name, source, productUrl);
        }
        String category = effectiveCategory(request);
        String imageUrl = firstHttpUrl(item, "thumbnail", "serpapi_thumbnail");
        if (imageUrl == null) {
            imageUrl = firstHttpUrlFromArray(item.get("thumbnails"));
        }
        if (imageUrl == null) {
            imageUrl = firstHttpUrlFromArray(item.get("serpapi_thumbnails"));
        }
        String priceText = text(item, "price");
        if (priceText.isBlank() && price != null) {
            priceText = formatPrice(price, currency);
        }
        String originalPriceText = text(item, "old_price");
        if (originalPriceText.isBlank() && originalPrice != null) {
            originalPriceText = formatPrice(originalPrice, currency);
        }
        ProductOffer.Availability availability = availability(item);
        String delivery = text(item, "delivery");
        List<ProductOffer> offers = offer(
                id, source, price, originalPrice, currency, productUrl, availability, delivery
        );

        Product product = new Product(
                id,
                firstText(item, "brand", "manufacturer"),
                name,
                category,
                priceText,
                originalPriceText.isBlank() ? null : originalPriceText,
                discount(price, originalPrice),
                number(item.get("rating")),
                integer(item.get("reviews")),
                source,
                iconFor(category),
                "artwork-live",
                imageUrl,
                text(item, "snippet"),
                false,
                offers,
                productUrl
        );
        return new MappedProduct(product, price, currency);
    }

    private boolean withinBudget(BigDecimal price, String currency, SearchRequest request) {
        ShoppingIntent intent = request.shoppingIntent();
        return price == null || !"INR".equals(currency) || intent == null
                || intent.maxBudget() == null
                || price.compareTo(BigDecimal.valueOf(intent.maxBudget())) <= 0;
    }

    private List<ProductOffer> offer(
            String productId,
            String source,
            BigDecimal price,
            BigDecimal originalPrice,
            String currency,
            String productUrl,
            ProductOffer.Availability availability,
            String delivery
    ) {
        if (source.isBlank() || price == null || !currency.matches("[A-Z]{3}") || productUrl == null) {
            return List.of();
        }
        try {
            return List.of(new ProductOffer(
                    productId, source, price, originalPrice, currency, productUrl, availability, delivery
            ));
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }

    private String buildKeyword(SearchRequest request) {
        if (!request.rawQuery().isBlank()) {
            return request.rawQuery();
        }
        ShoppingIntent intent = request.shoppingIntent();
        if (intent == null) {
            return request.category() == null || request.category().isBlank()
                    ? "shopping products" : request.category();
        }
        List<String> terms = new ArrayList<>();
        addTerms(terms, intent.colors(), 2);
        addTerms(terms, intent.productType(), 1);
        addTerms(terms, intent.category(), 1);
        addTerms(terms, intent.useCases(), 1);
        return terms.isEmpty() ? "shopping products" : String.join(" ", terms);
    }

    private String effectiveCategory(SearchRequest request) {
        if (request.filters().category() != null && !request.filters().category().isBlank()) {
            return request.filters().category();
        }
        if (request.category() != null && !request.category().isBlank()) {
            return request.category();
        }
        return request.shoppingIntent() == null || request.shoppingIntent().category() == null
                ? "" : request.shoppingIntent().category();
    }

    private IconType iconFor(String category) {
        if (category == null) {
            return IconType.BAG;
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "footwear", "shoes" -> IconType.SHOE;
            case "fashion" -> IconType.SHIRT;
            case "electronics" -> IconType.HEADPHONES;
            case "beauty" -> IconType.BEAUTY;
            case "accessories" -> IconType.WATCH;
            case "home" -> IconType.HOME;
            case "sports" -> IconType.SPORTS;
            default -> IconType.BAG;
        };
    }

    private String currency(JsonNode item) {
        String explicit = text(item, "currency").toUpperCase(Locale.ROOT);
        if (explicit.matches("[A-Z]{3}")) {
            return explicit;
        }
        String price = text(item, "price");
        if (price.contains("₹") || price.toLowerCase(Locale.ROOT).contains("inr")) {
            return "INR";
        }
        if (price.contains("$") || price.toLowerCase(Locale.ROOT).contains("usd")) {
            return "USD";
        }
        if (price.contains("€") || price.toLowerCase(Locale.ROOT).contains("eur")) {
            return "EUR";
        }
        if (price.contains("£") || price.toLowerCase(Locale.ROOT).contains("gbp")) {
            return "GBP";
        }
        return "";
    }

    private ProductOffer.Availability availability(JsonNode item) {
        String value = firstText(item, "availability", "stock").toLowerCase(Locale.ROOT);
        if (value.contains("out") || value.contains("unavailable")) {
            return ProductOffer.Availability.OUT_OF_STOCK;
        }
        if (value.contains("limited")) {
            return ProductOffer.Availability.LIMITED_STOCK;
        }
        if (value.contains("backorder")) {
            return ProductOffer.Availability.BACKORDERED;
        }
        if (value.contains("pre-order") || value.contains("preorder")) {
            return ProductOffer.Availability.PRE_ORDER;
        }
        if (value.contains("stock") || value.contains("available")) {
            return ProductOffer.Availability.IN_STOCK;
        }
        return ProductOffer.Availability.UNKNOWN;
    }

    private BigDecimal decimal(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            if (value.isNumber()) {
                return value.decimalValue();
            }
            String normalized = value.asText().replace(",", "").replaceAll("[^0-9.\\-]", "");
            return normalized.isBlank() ? null : new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private double number(JsonNode value) {
        BigDecimal number = decimal(value);
        return number == null ? 0 : Math.max(0, Math.min(5, number.doubleValue()));
    }

    private int integer(JsonNode value) {
        BigDecimal number = decimal(value);
        return number == null ? 0 : Math.max(0, number.intValue());
    }

    private String formatPrice(BigDecimal value, String currency) {
        String amount = value.setScale(Math.min(2, Math.max(0, value.stripTrailingZeros().scale())), RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
        return "INR".equals(currency) ? "₹" + amount : amount + (currency.isBlank() ? "" : " " + currency);
    }

    private String discount(BigDecimal price, BigDecimal originalPrice) {
        if (price == null || originalPrice == null || originalPrice.signum() <= 0
                || originalPrice.compareTo(price) <= 0) {
            return null;
        }
        int percent = originalPrice.subtract(price).divide(originalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
        return percent + "% off";
    }

    private String firstHttpUrl(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (isHttpUrl(value)) {
                return value;
            }
        }
        return null;
    }

    private String firstHttpUrlFromArray(JsonNode values) {
        if (values == null || !values.isArray()) {
            return null;
        }
        for (JsonNode value : values) {
            String candidate = value.asText("").trim();
            if (isHttpUrl(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String stableId(String name, String source, String url) {
        String value = (name + "-" + source + "-" + (url == null ? "" : url))
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return value.isBlank() ? "serpapi-product" : value;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return "";
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private void addTerms(List<String> terms, List<String> values, int limit) {
        values.stream().filter(value -> value != null && !value.isBlank()).limit(limit).forEach(terms::add);
    }

    private void addTerms(List<String> terms, String value, int limit) {
        if (limit > 0 && value != null && !value.isBlank()) {
            terms.add(value);
        }
    }

    private void add(List<String> parameters, String key, String value) {
        parameters.add(URLEncoder.encode(key, StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    static final class SerpApiException extends RuntimeException {
        private final SerpApiDiagnostics diagnostics;

        SerpApiException(String message, SerpApiDiagnostics diagnostics) {
            super(message);
            this.diagnostics = diagnostics;
        }

        SerpApiException(String message, SerpApiDiagnostics diagnostics, Throwable cause) {
            super(message, cause);
            this.diagnostics = diagnostics;
        }

        SerpApiDiagnostics diagnostics() {
            return diagnostics;
        }
    }

    private record MappedProduct(Product product, BigDecimal price, String currency) {
    }

    private record MappingResult(List<Product> products, int mappedProductCount) {
    }
}
