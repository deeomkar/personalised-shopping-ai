package com.myshop.service;

import com.myshop.model.SearchRequest;
import com.myshop.model.ShoppingIntent;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Runtime order: Groq, usable Gemini, then deterministic fallback. */
public final class ShoppingIntentProviderChain implements ShoppingIntentProvider {
    private static final Logger LOGGER = Logger.getLogger(ShoppingIntentProviderChain.class.getName());

    private final GroqShoppingIntentProvider groqProvider;
    private final GeminiShoppingIntentProvider geminiProvider;
    private final ShoppingIntentProvider fallbackProvider;

    public ShoppingIntentProviderChain() {
        this(new GroqShoppingIntentProvider(), new GeminiShoppingIntentProvider(),
                new FallbackShoppingIntentProvider());
    }

    ShoppingIntentProviderChain(GroqShoppingIntentProvider groqProvider,
                                GeminiShoppingIntentProvider geminiProvider,
                                ShoppingIntentProvider fallbackProvider) {
        this.groqProvider = Objects.requireNonNull(groqProvider, "groqProvider");
        this.geminiProvider = Objects.requireNonNull(geminiProvider, "geminiProvider");
        this.fallbackProvider = Objects.requireNonNull(fallbackProvider, "fallbackProvider");
    }

    @Override
    public ShoppingIntent understand(SearchRequest request) {
        if (groqProvider.isConfigured()) {
            try {
                return groqProvider.understandLive(request);
            } catch (RuntimeException exception) {
                log("Groq provider unavailable; continuing to next intent provider", exception);
            }
        }
        if (geminiProvider.isConfigured()) {
            try {
                return geminiProvider.understandLive(request);
            } catch (RuntimeException exception) {
                log("Gemini provider unavailable; continuing to fallback", exception);
            }
        }
        return fallbackProvider.understand(request);
    }

    private void log(String message, RuntimeException exception) {
        LOGGER.log(Level.FINE, message + ": " + exception.getClass().getSimpleName());
    }
}
