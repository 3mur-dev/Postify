package com.omar.postify.exception;

public class InsufficientAuthenticationException extends RuntimeException {
    public InsufficientAuthenticationException(String message) {
        super(message);
    }
}
