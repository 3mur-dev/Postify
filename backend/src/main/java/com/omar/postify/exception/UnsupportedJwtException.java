package com.omar.postify.exception;

public class UnsupportedJwtException extends JwtAuthenticationException {
    public UnsupportedJwtException(String message) {
        super(message);
    }
}
