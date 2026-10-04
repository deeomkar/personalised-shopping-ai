package com.myshop.service;

public final class AuthException extends RuntimeException {

    public enum Code {
        VALIDATION,
        DUPLICATE_EMAIL,
        INVALID_CREDENTIALS,
        PERSISTENCE
    }

    private final Code code;

    public AuthException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public AuthException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
