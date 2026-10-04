package com.myshop.repository;

import com.myshop.component.IconType;
import com.myshop.model.Product;
import com.myshop.model.ProductOffer;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Maps the small offline product snapshot used by saved/history storage. */
final class ProductPersistenceMapper {

    private ProductPersistenceMapper() {
    }

    static void bindProduct(PreparedStatement statement, int start, Product product) throws SQLException {
        statement.setString(start, product.id());
        statement.setString(start + 1, product.brand());
        statement.setString(start + 2, product.name());
        statement.setString(start + 3, product.category());
        statement.setString(start + 4, product.price());
        statement.setString(start + 5, product.originalPrice());
        statement.setString(start + 6, product.discount());
        statement.setDouble(start + 7, product.rating());
        statement.setInt(start + 8, product.reviewCount());
        statement.setString(start + 9, product.store());
        statement.setString(start + 10, product.artwork() == null ? null : product.artwork().name());
        statement.setString(start + 11, product.artworkClass());
        statement.setString(start + 12, product.imageUrl());
        statement.setString(start + 13, product.description());
        ProductOffer offer = product.offers().stream().findFirst().orElse(null);
        if (offer == null) {
            for (int offset = 14; offset <= 21; offset++) {
                statement.setObject(start + offset, null);
            }
        } else {
            statement.setString(start + 14, offer.productId());
            statement.setString(start + 15, offer.storeName());
            statement.setBigDecimal(start + 16, offer.price());
            statement.setBigDecimal(start + 17, offer.originalPrice());
            statement.setString(start + 18, offer.currency());
            statement.setString(start + 19, offer.productUrl());
            statement.setString(start + 20, offer.availability().name());
            statement.setString(start + 21, offer.delivery());
        }
    }

    static Product readProduct(ResultSet resultSet) throws SQLException {
        IconType artwork = icon(resultSet.getString("artwork"));
        ProductOffer offer = readOffer(resultSet);
        return new Product(
                resultSet.getString("product_id"), resultSet.getString("brand"), resultSet.getString("name"),
                resultSet.getString("category"), resultSet.getString("price"), resultSet.getString("original_price"),
                resultSet.getString("discount"), resultSet.getDouble("rating"), resultSet.getInt("review_count"),
                resultSet.getString("store"), artwork, resultSet.getString("artwork_class"),
                resultSet.getString("image_url"), resultSet.getString("description"), true,
                offer == null ? List.of() : List.of(offer)
        );
    }

    private static ProductOffer readOffer(ResultSet resultSet) throws SQLException {
        String offerId = resultSet.getString("offer_product_id");
        if (offerId == null || offerId.isBlank()) {
            return null;
        }
        String availability = resultSet.getString("offer_availability");
        try {
            return new ProductOffer(
                    offerId, resultSet.getString("offer_store_name"), resultSet.getBigDecimal("offer_price"),
                    resultSet.getBigDecimal("offer_original_price"), resultSet.getString("offer_currency"),
                    resultSet.getString("offer_url"), availability == null
                            ? ProductOffer.Availability.UNKNOWN : ProductOffer.Availability.valueOf(availability),
                    resultSet.getString("offer_delivery")
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static IconType icon(String value) {
        try {
            return value == null ? IconType.BAG : IconType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return IconType.BAG;
        }
    }
}
