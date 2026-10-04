package com.myshop.service;

public final class PreferenceException extends RuntimeException {

    public enum Code {
        VALIDATION,
        PERSISTENCE
    }

    private final Code code;

    public PreferenceException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public PreferenceException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
