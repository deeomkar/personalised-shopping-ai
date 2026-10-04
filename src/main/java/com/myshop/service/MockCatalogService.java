package com.myshop.service;

import com.myshop.component.IconType;
import com.myshop.model.Category;
import com.myshop.model.Product;

import java.util.List;
import java.util.stream.Stream;

public final class MockCatalogService {

    private MockCatalogService() {
    }

    public static List<Category> categories() {
        return List.of(
                new Category("Fashion", IconType.SHIRT, "category-sage"),
                new Category("Footwear", IconType.SHOE, "category-sand"),
                new Category("Electronics", IconType.HEADPHONES, "category-blue"),
                new Category("Beauty", IconType.BEAUTY, "category-blush"),
                new Category("Accessories", IconType.WATCH, "category-lilac"),
                new Category("Home", IconType.HOME, "category-clay"),
                new Category("Sports", IconType.SPORTS, "category-sky")
        );
    }

    public static List<Product> pickedProducts() {
        return List.of(
                new Product(
                        "veja-campo-leather-sneakers",
                        "Veja",
                        "Campo Leather Sneakers",
                        "Footwear",
                        "$135",
                        "$170",
                        "21% off",
                        4.8,
                        184,
                        "Nordstrom",
                        IconType.SHOE,
                        "artwork-olive",
                        null,
                        "A clean leather sneaker with an easy everyday silhouette.",
                        false
                ),
                new Product(
                        "marshall-major-v-headphones",
                        "Marshall",
                        "Major V Headphones",
                        "Electronics",
                        "$149",
                        null,
                        null,
                        4.7,
                        92,
                        "Best Buy",
                        IconType.HEADPHONES,
                        "artwork-ink",
                        null,
                        "Wireless headphones with a warm, detailed listening profile.",
                        true
                ),
                new Product(
                        "uniqlo-soft-ribbed-overshirt",
                        "Uniqlo",
                        "Soft Ribbed Overshirt",
                        "Fashion",
                        "$59.90",
                        null,
                        null,
                        4.6,
                        318,
                        "Uniqlo",
                        IconType.SHIRT,
                        "artwork-clay",
                        null,
                        "A relaxed layering piece with soft ribbed texture.",
                        false
                ),
                new Product(
                        "seiko-presage-cocktail-time",
                        "Seiko",
                        "Presage Cocktail Time",
                        "Accessories",
                        "$390",
                        "$450",
                        "13% off",
                        4.9,
                        67,
                        "Macy's",
                        IconType.WATCH,
                        "artwork-amber",
                        null,
                        "A polished automatic watch with a deep cocktail-inspired dial.",
                        false
                )
        );
    }

    public static List<Product> recentlyViewed() {
        return List.of(
                new Product(
                        "ferm-living-ripple-glass-set",
                        "Ferm Living",
                        "Ripple Glass Set",
                        "Home",
                        "$45",
                        null,
                        null,
                        4.5,
                        41,
                        "West Elm",
                        IconType.BAG,
                        "artwork-sand",
                        null,
                        "Hand-blown glasses with a softly rippled finish.",
                        false
                ),
                new Product(
                        "on-running-cloud-5-in-chalk",
                        "On Running",
                        "Cloud 5 in Chalk",
                        "Footwear",
                        "$150",
                        null,
                        null,
                        4.8,
                        220,
                        "On",
                        IconType.SHOE,
                        "artwork-sage",
                        null,
                        "A lightweight everyday trainer with a comfortable cloud sole.",
                        false
                ),
                new Product(
                        "aesop-resurrection-aromatique",
                        "Aesop",
                        "Resurrection Aromatique",
                        "Beauty",
                        "$43",
                        null,
                        null,
                        4.7,
                        88,
                        "Aesop",
                        IconType.BEAUTY,
                        "artwork-brown",
                        null,
                        "A gentle hand wash with a fresh botanical scent.",
                        false
                )
        );
    }

    public static List<Product> searchableProducts() {
        return Stream.concat(pickedProducts().stream(), recentlyViewed().stream()).toList();
    }
}
