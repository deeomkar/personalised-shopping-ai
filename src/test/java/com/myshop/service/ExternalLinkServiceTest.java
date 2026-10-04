package com.myshop.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalLinkServiceTest {
    @Test
    void onlyHttpAndHttpsLinksAreSafe() {
        assertTrue(ExternalLinkService.isSafe("https://example.com/product"));
        assertTrue(ExternalLinkService.isSafe("http://example.com"));
        assertFalse(ExternalLinkService.isSafe("javascript:alert(1)"));
        assertFalse(ExternalLinkService.isSafe("not-a-url"));
    }
}
