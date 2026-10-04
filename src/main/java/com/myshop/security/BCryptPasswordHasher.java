package com.myshop.security;

import org.mindrot.jbcrypt.BCrypt;

public final class BCryptPasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        try {
            return BCrypt.checkpw(rawPassword, hash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
