package com.myshop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.component.IconType;
import com.myshop.config.DataForSeoConfig;
import com.myshop.model.Product;
import com.myshop.model.ProductOffer;
import com.myshop.model.SearchRequest;
import com.myshop.model.ShoppingIntent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * DataForSEO Merchant API adapter for Google Shopping product results.
 *
 * <p>The adapter owns task submission, polling, JSON traversal, and normalization;
 * the rest of MyShop only sees {@link Product} values.</p>
 */
public final class DataForSeoProductSearchAdapter implements LiveProductSearchAdapter {
    private static final String PRODUCTS_TASK_POST = "/v3/merchant/google/products/task_post";
    private static final String PRODUCTS_TASK_GET = "/v3/merchant/google/products/task_get/advanced/";
    private static final int SUCCESS_CODE = 20000;
    private static final int DEFAULT_DEPTH = 20;

    private final DataForSeoConfig config;
    private final DataForSeoTransport transport;
    private final ObjectMapper objectMapper;

    public DataForSeoProductSearchAdapter(DataForSeoConfig config) {
        this(config, new JavaHttpDataForSeoTransport(config.requestTimeout()), new ObjectMapper());
    }

    DataForSeoProductSearchAdapter(
            DataForSeoConfig config,
            DataForSeoTransport transport,
            ObjectMapper objectMapper
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public List<Product> search(SearchRequest request) {
        Objects.requireNonNull(request, "request");
        if (!config.hasCredentials()) {
            throw new IllegalStateException("DataForSEO credentials are not configured");
        }

        String taskId = submitTask(request);
        JsonNode result = pollTask(taskId);
        return mapProducts(result, request);
    }

    private String submitTask(SearchRequest request) {
        try {
            var task = objectMapper.createObjectNode();
            task.put("language_name", config.languageName());
            task.put("location_name", config.locationName());
            task.put("keyword", buildKeyword(request));
            task.put("depth", DEFAULT_DEPTH);
            ShoppingIntent intent = request.shoppingIntent();
            if (intent != null && intent.maxBudget() != null && isIndiaCurrencyContext(request)) {
                task.put("price_max", intent.maxBudget());
            }

            var payload = objectMapper.createArrayNode();
            payload.add(task);
            DataForSeoHttpResponse response = transport.send(
                    "POST", endpoint(PRODUCTS_TASK_POST), config.login(), config.password(),
                    objectMapper.writeValueAsString(payload), config.requestTimeout()
            );
            JsonNode root = parseHttpResponse(response, "task submission");
            ensureSuccess(root, "task submission");
            JsonNode taskResult = firstTask(root);
            String taskId = text(taskResult, "id");
            if (taskId.isBlank()) {
                throw new DataForSeoException("DataForSEO task submission did not return a task id");
            }
            return taskId;
        } catch (DataForSeoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DataForSeoException("DataForSEO task submission could not be created", exception);
        }
    }

    private JsonNode pollTask(String taskId) {
        Instant deadline = Instant.now().plus(config.pollTimeout());
        while (true) {
            DataForSeoHttpResponse response = transport.send(
                    "GET", endpoint(PRODUCTS_TASK_GET + taskId), config.login(), config.password(),
                    "", config.requestTimeout()
            );
            JsonNode root = parseHttpResponse(response, "task result");
            ensureSuccess(root, "task result");
            JsonNode task = firstTask(root);
            int taskStatus = task.path("status_code").asInt(0);
            if (taskStatus >= 40000) {
                throw new DataForSeoException("DataForSEO task failed with status " + taskStatus
                        + ": " + text(task, "status_message"));
            }
            JsonNode result = task.get("result");
            if (result != null && !result.isNull() && result.isArray()) {
                return result;
            }
            if (!Instant.now().isBefore(deadline)) {
                throw new DataForSeoException("DataForSEO task did not complete before the polling timeout");
            }
            sleep(config.pollInterval());
        }
    }

    private List<Product> mapProducts(JsonNode results, SearchRequest request) {
        List<JsonNode> candidates = new ArrayList<>();
        collectProductNodes(results, candidates);
        List<Product> products = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode candidate : candidates) {
            Product product = mapProduct(candidate, request);
            if (product == null || !seen.add(product.id())) {
                continue;
            }
            products.add(product);
            if (products.size() >= config.maxResults()) {
                break;
            }
        }
        return List.copyOf(products);
    }

    private void collectProductNodes(JsonNode node, List<JsonNode> candidates) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            String title = text(node, "title");
            if (!title.isBlank() && looksLikeProduct(node)) {
                candidates.add(node);
            }
            node.fields().forEachRemaining(entry -> collectProductNodes(entry.getValue(), candidates));
        } else if (node.isArray()) {
            node.forEach(child -> collectProductNodes(child, candidates));
        }
    }

    private boolean looksLikeProduct(JsonNode node) {
        return node.has("price") || node.has("product_images") || node.has("seller")
                || node.has("product_id") || node.has("shopping_url") || node.has("url");
    }

    private Product mapProduct(JsonNode node, SearchRequest request) {
        String name = text(node, "title");
        if (name.isBlank()) {
            return null;
        }
        BigDecimal price = decimal(node.get("price"));
        BigDecimal originalPrice = decimal(node.get("old_price"));
        String currency = text(node, "currency").toUpperCase(Locale.ROOT);
        if (isOverBudget(price, currency, request)) {
            return null;
        }

        String store = firstText(node, "seller", "domain");
        String productUrl = firstHttpUrl(node, "url", "shopping_url");
        String id = firstText(node, "product_id", "data_docid", "gid");
        if (id.isBlank()) {
            id = stableId(name, store, productUrl);
        }
        String category = effectiveCategory(request);
        String imageUrl = firstHttpUrlFromArray(node.get("product_images"));
        double rating = rating(node.get("product_rating"));
        int reviewCount = node.path("reviews_count").isNumber()
                ? node.path("reviews_count").asInt()
                : node.path("product_rating").path("votes_count").asInt(0);
        String priceText = price == null ? "" : formatPrice(price, currency);
        String originalPriceText = originalPrice == null ? null : formatPrice(originalPrice, currency);
        String discount = discount(price, originalPrice);
        List<ProductOffer> offers = offer(
                id, store, price, originalPrice, currency, productUrl, availability(node)
        );

        return new Product(
                id,
                text(node, "brand"),
                name,
                category,
                priceText,
                originalPriceText,
                discount,
                rating,
                reviewCount,
                store,
                iconFor(category),
                "artwork-live",
                imageUrl,
                text(node, "description"),
                false,
                offers,
                productUrl
        );
    }

    private List<ProductOffer> offer(
            String productId,
            String store,
            BigDecimal price,
            BigDecimal originalPrice,
            String currency,
            String productUrl,
            ProductOffer.Availability availability
    ) {
        if (store.isBlank() || price == null || !currency.matches("[A-Z]{3}") || productUrl == null) {
            return List.of();
        }
        try {
            return List.of(new ProductOffer(
                    productId, store, price, originalPrice, currency, productUrl,
                    availability
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
        addTerm(terms, intent.colors(), 2);
        addTerm(terms, intent.productType(), 1);
        addTerm(terms, intent.category(), 1);
        addTerm(terms, intent.useCases(), 1);
        return terms.isEmpty() ? "shopping products" : String.join(" ", terms);
    }

    private boolean isIndiaCurrencyContext(SearchRequest request) {
        return config.locationName().equalsIgnoreCase("India");
    }

    private boolean isOverBudget(BigDecimal price, String currency, SearchRequest request) {
        ShoppingIntent intent = request.shoppingIntent();
        return price != null && "INR".equals(currency) && intent != null && intent.maxBudget() != null
                && price.compareTo(BigDecimal.valueOf(intent.maxBudget())) > 0;
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

    private double rating(JsonNode rating) {
        if (rating == null || rating.isNull()) {
            return 0;
        }
        BigDecimal value = decimal(rating.get("value"));
        if (value == null) {
            return 0;
        }
        int maximum = rating.path("rating_max").asInt(5);
        if (maximum > 0 && maximum != 5) {
            return value.doubleValue() * 5 / maximum;
        }
        return Math.max(0, Math.min(5, value.doubleValue()));
    }

    private ProductOffer.Availability availability(JsonNode node) {
        String value = firstText(node, "availability", "stock", "availability_status")
                .toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return ProductOffer.Availability.UNKNOWN;
        }
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

    private String formatPrice(BigDecimal value, String currency) {
        BigDecimal rounded = value.stripTrailingZeros();
        String amount = rounded.scale() <= 0
                ? rounded.toBigInteger().toString()
                : rounded.setScale(Math.min(2, Math.max(0, rounded.scale())), RoundingMode.HALF_UP).toPlainString();
        return "INR".equals(currency) ? "₹" + amount : amount + (currency.isBlank() ? "" : " " + currency);
    }

    private String discount(BigDecimal price, BigDecimal originalPrice) {
        if (price == null || originalPrice == null || originalPrice.signum() <= 0
                || originalPrice.compareTo(price) <= 0) {
            return null;
        }
        int percent = originalPrice.subtract(price)
                .divide(originalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
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

    private String firstHttpUrlFromArray(JsonNode array) {
        if (array == null || !array.isArray()) {
            return null;
        }
        for (JsonNode value : array) {
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

    private String stableId(String name, String store, String url) {
        String value = (name + "-" + store + "-" + (url == null ? "" : url))
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return value.isBlank() ? "dataforseo-product" : value;
    }

    private void addTerm(List<String> terms, List<String> values, int limit) {
        values.stream().filter(value -> value != null && !value.isBlank()).limit(limit).forEach(terms::add);
    }

    private void addTerm(List<String> terms, String value, int limit) {
        if (value != null && !value.isBlank() && limit > 0) {
            terms.add(value);
        }
    }

    private URI endpoint(String path) {
        return config.baseUri().resolve(path);
    }

    private JsonNode parseHttpResponse(DataForSeoHttpResponse response, String operation) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new DataForSeoException("DataForSEO " + operation + " returned HTTP " + response.statusCode());
        }
        try {
            return objectMapper.readTree(response.body());
        } catch (Exception exception) {
            throw new DataForSeoException("DataForSEO " + operation + " returned malformed JSON", exception);
        }
    }

    private void ensureSuccess(JsonNode root, String operation) {
        int status = root.path("status_code").asInt(0);
        if (status != SUCCESS_CODE) {
            throw new DataForSeoException("DataForSEO " + operation + " failed with status "
                    + status + ": " + text(root, "status_message"));
        }
    }

    private JsonNode firstTask(JsonNode root) {
        JsonNode tasks = root.path("tasks");
        if (!tasks.isArray() || tasks.isEmpty() || tasks.get(0).isNull()) {
            throw new DataForSeoException("DataForSEO response did not contain a task");
        }
        return tasks.get(0);
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

    private void sleep(Duration duration) {
        if (duration.isZero()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DataForSeoException("DataForSEO polling was interrupted", exception);
        }
    }

    static final class DataForSeoException extends RuntimeException {
        DataForSeoException(String message) {
            super(message);
        }

        DataForSeoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
