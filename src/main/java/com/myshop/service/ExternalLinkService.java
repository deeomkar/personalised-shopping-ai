package com.myshop.service;

import java.awt.Desktop;
import java.net.URI;

/** Opens only provider-returned HTTP(S) links and fails safely. */
public final class ExternalLinkService {
    public enum Result { OPENED, INVALID_URL, UNSUPPORTED, FAILED }

    private ExternalLinkService() { }

    public static boolean isSafe(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static Result open(String value) {
        if (!isSafe(value)) return Result.INVALID_URL;
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return Result.UNSUPPORTED;
        }
        try {
            Desktop.getDesktop().browse(URI.create(value.trim()));
            return Result.OPENED;
        } catch (Exception exception) {
            return Result.FAILED;
        }
    }
}
