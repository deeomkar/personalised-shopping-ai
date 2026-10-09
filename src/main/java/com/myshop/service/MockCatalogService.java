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
                        "₹135",
                        "₹170",
                        "21% off",
                        4.8,
                        184,
                        null,
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
                        "₹149",
                        null,
                        null,
                        4.7,
                        92,
                        null,
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
                        "₹59.90",
                        null,
                        null,
                        4.6,
                        318,
                        null,
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
                        "₹390",
                        "₹450",
                        "13% off",
                        4.9,
                        67,
                        null,
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
                        "₹45",
                        null,
                        null,
                        4.5,
                        41,
                        null,
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
                        "₹150",
                        null,
                        null,
                        4.8,
                        220,
                        null,
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
                        "₹43",
                        null,
                        null,
                        4.7,
                        88,
                        null,
                        IconType.BEAUTY,
                        "artwork-brown",
                        null,
                        "A gentle hand wash with a fresh botanical scent.",
                        false
                )
        );
    }

    /**
     * Neutral home-feed cards used only when the user has no local product history yet.
     * They intentionally contain no price, seller, or product link.
     */
    public static List<Product> discoveryPlaceholders() {
        return List.of(
                new Product(
                        "discover-footwear",
                        "",
                        "Everyday footwear",
                        "Footwear",
                        null,
                        null,
                        null,
                        0,
                        0,
                        null,
                        IconType.SHOE,
                        "category-sand",
                        null,
                        "Start with a search to find products in India.",
                        false
                ),
                new Product(
                        "discover-fashion",
                        "",
                        "Easy everyday layers",
                        "Fashion",
                        null,
                        null,
                        null,
                        0,
                        0,
                        null,
                        IconType.SHIRT,
                        "category-sage",
                        null,
                        "Find styles that fit your routine and preferences.",
                        false
                ),
                new Product(
                        "discover-home",
                        "",
                        "Home essentials",
                        "Home",
                        null,
                        null,
                        null,
                        0,
                        0,
                        null,
                        IconType.HOME,
                        "category-clay",
                        null,
                        "Browse useful pieces for your everyday space.",
                        false
                ),
                new Product(
                        "discover-tech",
                        "",
                        "Everyday tech",
                        "Electronics",
                        null,
                        null,
                        null,
                        0,
                        0,
                        null,
                        IconType.HEADPHONES,
                        "category-blue",
                        null,
                        "Search when you are ready to compare real products.",
                        false
                )
        );
    }

    public static List<Product> searchableProducts() {
        return Stream.concat(pickedProducts().stream(), recentlyViewed().stream()).toList();
    }
}
