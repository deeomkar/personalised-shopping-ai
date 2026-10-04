package com.myshop.model;

import java.util.List;
import java.util.Objects;

/** A provider-returned product with a deterministic local relevance score. */
public record Recommendation(
        Product product,
        double score,
        List<RecommendationReason> reasons
) {
    public Recommendation {
        Objects.requireNonNull(product, "product");
        if (!Double.isFinite(score) || score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        reasons = List.copyOf(reasons == null ? List.of() : reasons).stream().limit(3).toList();
    }

    public String matchLabel() {
        if (score >= 65) {
            return "Strong match";
        }
        if (score >= 38) {
            return "Good match";
        }
        return "Relevant option";
    }
}
