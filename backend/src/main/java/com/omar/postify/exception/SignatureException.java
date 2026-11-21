package com.omar.postify.exception;

public class SignatureException extends JwtAuthenticationException {
    public SignatureException(String message) {
        super(message);
    }
}
