package com.omar.postify.exception;

public class MalformedJwtException extends JwtAuthenticationException {
    public MalformedJwtException(String message) {
        super(message);
    }
}
