package com.omar.postify.exception;

public class ExpiredJwtException extends JwtAuthenticationException {
    public ExpiredJwtException(String message) {
        super(message);
    }
}
