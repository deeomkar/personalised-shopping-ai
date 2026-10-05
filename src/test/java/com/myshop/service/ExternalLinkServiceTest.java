package com.myshop.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void rejectsInvalidUrlsBeforeAttemptingToOpen() {
        assertEquals(ExternalLinkService.Result.INVALID_URL,
                ExternalLinkService.open("javascript:alert(1)"));
        assertEquals(ExternalLinkService.Result.INVALID_URL,
                ExternalLinkService.open("not-a-url"));
    }
}
